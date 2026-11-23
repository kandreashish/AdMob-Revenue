package com.lavish.admob.bubble

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.lavish.admob.MainActivity
import com.lavish.admob.RepositoryHolder
import com.lavish.admob.ui.utils.formatAmount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Foreground service that draws a floating "today's earnings" bubble over other apps.
 * - Drag to move, snaps to nearest edge, position remembered.
 * - Tap: opens AdDash (or the unlock promo if the trial has ended).
 * - Drag onto the ✕ target at the bottom to hide.
 * - Refreshes earnings at the user's Auto-refresh setting while the screen is on.
 */
class EarningsBubbleService : Service() {

    companion object {
        private const val CHANNEL_ID = "earnings_bubble"
        private const val NOTIF_ID = 4201
        private const val ACTION_STOP = "com.lavish.admob.bubble.STOP"
        private const val PREFS = "earnings_bubble"
        private const val KEY_X = "bubble_x"
        private const val KEY_Y = "bubble_y"
        private const val KEY_ENABLED = "bubble_enabled"
        const val EXTRA_OPEN_PROMO = "open_bubble_promo"
        /** Bubble opacity when idle (translucent) vs. while being touched/dragged. */
        private const val IDLE_ALPHA = 0.6f
        private const val ACTIVE_ALPHA = 1f

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            context.getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, true).apply()
            _enabled.value = true // UI (banner / switch) updates instantly
            BubbleEntitlementManager.markPromoCompleted()
            androidx.core.content.ContextCompat.startForegroundService(
                context, Intent(context, EarningsBubbleService::class.java)
            )
        }

        fun stop(context: Context) {
            context.getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, false).apply()
            _enabled.value = false
            context.stopService(Intent(context, EarningsBubbleService::class.java))
        }

        /**
         * True while AdDash itself is on screen. The bubble hides (and stops refreshing, since the
         * app refreshes the same data) while the app is open, and reappears when the user leaves.
         */
        val appInForeground = kotlinx.coroutines.flow.MutableStateFlow(false)

        fun isEnabled(context: Context): Boolean =
            context.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

        private val _enabled = kotlinx.coroutines.flow.MutableStateFlow<Boolean?>(null)

        /** Observable on/off state so Compose UI reacts the moment the bubble is started or stopped. */
        fun enabledFlow(context: Context): kotlinx.coroutines.flow.StateFlow<Boolean?> {
            if (_enabled.value == null) _enabled.value = isEnabled(context)
            return _enabled
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var wm: WindowManager
    private var bubble: FrameLayout? = null
    private var closeTarget: TextView? = null
    private lateinit var amountText: TextView
    private lateinit var contentColumn: LinearLayout
    private var sizeScale = 1f
    private lateinit var labelText: TextView
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private var locked = false
    private var goalProgress: Double? = null

    private fun dp(v: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startInForeground()
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return }
        addBubble()
        observeData()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stop(this)
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        bubble?.let { runCatching { wm.removeView(it) } }
        closeTarget?.let { runCatching { wm.removeView(it) } }
        bubble = null
        closeTarget = null
        super.onDestroy()
    }

    // ---------------- Foreground notification ----------------

    private fun startInForeground() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Earnings bubble", NotificationManager.IMPORTANCE_MIN).apply {
                    description = "Keeps the floating earnings bubble running"
                    setShowBadge(false)
                }
            )
        }
        val notification = buildNotification(null, 0L)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIF_ID, notification, type)
    }

    /** Ongoing notification: today's amount + last sync time (also shown as the notification timestamp). */
    private fun buildNotification(amount: String?, lastSyncMs: Long): Notification {
        val stopPi = PendingIntent.getService(
            this, 1, Intent(this, EarningsBubbleService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val syncText = if (lastSyncMs > 0)
            "Last sync " + java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(lastSyncMs))
        else "Not synced yet"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(com.lavish.admob.android.R.drawable.ic_stat_bubble)
            .setContentTitle(if (amount != null) "Today: $amount" else "Earnings bubble is on")
            .setContentText(syncText)
            .apply { if (lastSyncMs > 0) setWhen(lastSyncMs).setShowWhen(true) else setShowWhen(false) }
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(openAppIntent(false))
            .addAction(0, "Hide bubble", stopPi)
            .build()
    }

    private var lastNotified: Pair<String?, Long>? = null

    private fun updateNotification(amount: String?, lastSyncMs: Long) {
        val key = amount to lastSyncMs
        if (key == lastNotified) return
        lastNotified = key
        runCatching {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .notify(NOTIF_ID, buildNotification(amount, lastSyncMs))
        }
    }

    private fun openAppIntent(openPromo: Boolean): PendingIntent {
        val i = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_OPEN_PROMO, openPromo)
        return PendingIntent.getActivity(
            this, if (openPromo) 3 else 2, i,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    // ---------------- Data ----------------

    private fun observeData() {
        val repo = RepositoryHolder.get(applicationContext)

        // Bubble stays visible at all times, including while the app is open.
        setBubbleVisible(true)

        // Live-resize when the user changes the size in Settings.
        scope.launch {
            combine(BubbleSizePref.diameterDp, BubbleSizePref.previewDp) { saved, preview -> preview ?: saved }
                .collect { applySize(it) }
        }
        BubbleEntitlementManager.start(applicationContext)

        scope.launch {
            combine(
                repo.accountSummary, repo.session, BubbleEntitlementManager.access, repo.lastRefreshTimestampMs
            ) { s, session, access, syncMs ->
                val amount = if (session.isLoggedIn && access.canUseBubble)
                    session.currencySymbol + formatAmount(s.todayRevenue) else null
                updateNotification(amount, syncMs)
                goalProgress = BubbleColors.progress(s.todayRevenue, s.dailyGoal)
                Triple(s.todayRevenue, session, access)
            }.collect { (revenue, session, access) ->
                when {
                    !session.isLoggedIn || access is BubbleAccess.SignedOut -> {
                        locked = false; render("TODAY", "—")
                    }
                    access is BubbleAccess.TrialExpired || access is BubbleAccess.NotStarted -> {
                        locked = true; render("LOCKED", "Unlock")
                    }
                    else -> {
                        locked = false; render("TODAY", session.currencySymbol + compact(revenue))
                    }
                }
            }
        }

        // Periodic refresh follows the user's in-app "Auto-refresh" setting (Off / 30s / 1 / 2 / 5 min).
        // Off => the bubble just shows the last synced value. Skipped while the screen is off,
        // while the app is open (it refreshes itself), or when the bubble is locked.
        scope.launch {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            repo.autoRefreshInterval.collectLatest { interval ->
                val intervalMs = interval.seconds * 1000L
                if (intervalMs <= 0) return@collectLatest
                while (isActive) {
                    delay(intervalMs)
                    if (pm.isInteractive && !appInForeground.value &&
                        BubbleEntitlementManager.access.value.canUseBubble &&
                        repo.session.value.isLoggedIn
                    ) {
                        runCatching { repo.refreshRealData() }
                    }
                }
            }
        }
    }

    private fun setBubbleVisible(visible: Boolean, fadeOutMs: Long = 0L) {
        val b = bubble ?: return
        b.animate().cancel()
        if (visible) {
            b.alpha = IDLE_ALPHA
            b.visibility = View.VISIBLE
        } else {
            hideCloseTarget()
            if (fadeOutMs > 0 && b.visibility == View.VISIBLE) {
                b.animate().alpha(0f).setDuration(fadeOutMs).withEndAction {
                    b.visibility = View.GONE
                    b.alpha = IDLE_ALPHA
                }.start()
            } else {
                b.visibility = View.GONE
            }
        }
        // A hidden overlay window must not swallow touches meant for the app underneath.
        bubbleParams.flags = if (visible)
            bubbleParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        else
            bubbleParams.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        runCatching { wm.updateViewLayout(b, bubbleParams) }
    }

    private fun compact(v: Double): String = when {
        v >= 1_000_000 -> String.format("%.1fM", v / 1_000_000)
        v >= 10_000 -> String.format("%.1fK", v / 1_000)
        else -> formatAmount(v)
    }

    private fun render(label: String, amount: String) {
        if (bubble == null) return
        labelText.text = label
        amountText.text = amount
        amountText.setTextSize(TypedValue.COMPLEX_UNIT_SP, (if (amount.length > 7) 11f else 13f) * sizeScale)
        // Colour reflects progress toward the daily goal (red → amber → blue → green)
        (bubble?.background as? GradientDrawable)?.colors =
            if (locked) BubbleColors.LOCKED else BubbleColors.forProgress(goalProgress)
    }

    private fun applySize(diameterDp: Int) {
        val b = bubble ?: return
        sizeScale = BubbleSizePref.scaleFor(diameterDp)
        val px = dp(diameterDp)
        if (bubbleParams.width == px) { render(labelText.text.toString(), amountText.text.toString()); return }
        // Keep the bubble's centre roughly where it was
        bubbleParams.x += (bubbleParams.width - px) / 2
        bubbleParams.y += (bubbleParams.height - px) / 2
        bubbleParams.width = px
        bubbleParams.height = px
        contentColumn.layoutParams = FrameLayout.LayoutParams(px, px, Gravity.CENTER)
        labelText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f * sizeScale)
        applySpacing()
        b.elevation = dp(8) * sizeScale
        render(labelText.text.toString(), amountText.text.toString())
        runCatching { wm.updateViewLayout(b, bubbleParams) }
        // While the slider is moving, resize in place; snap to the edge once the user lets go.
        if (BubbleSizePref.previewDp.value == null) snapToEdge()
    }

    /** Gap between "TODAY" and the amount scales with the bubble. */
    private fun applySpacing() {
        (amountText.layoutParams as? LinearLayout.LayoutParams)?.let {
            it.topMargin = (dp(1) * sizeScale).toInt()
            amountText.layoutParams = it
        }
    }

    // ---------------- Overlay views ----------------

    private val overlayType =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    @SuppressLint("ClickableViewAccessibility")
    private fun addBubble() {
        BubbleSizePref.init(this)
        sizeScale = BubbleSizePref.scaleFor(BubbleSizePref.diameterDp.value)
        val size = dp(BubbleSizePref.diameterDp.value)
        labelText = TextView(this).apply {
            setTextColor(Color.argb(220, 255, 255, 255))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f * sizeScale)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            text = "TODAY"
        }
        amountText = TextView(this).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f * sizeScale)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            maxLines = 1
            text = "—"
        }
        contentColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(labelText)
            addView(amountText)
            labelText.includeFontPadding = false
            amountText.includeFontPadding = false
        }
        val view = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                BubbleColors.NO_GOAL
            ).apply { shape = GradientDrawable.OVAL }
            elevation = dp(8).toFloat()
            alpha = IDLE_ALPHA
            addView(contentColumn, FrameLayout.LayoutParams(size, size, Gravity.CENTER))
        }

        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        bubbleParams = WindowManager.LayoutParams(
            size, size, overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.getInt(KEY_X, resources.displayMetrics.widthPixels - size - dp(8))
            y = prefs.getInt(KEY_Y, resources.displayMetrics.heightPixels / 3)
        }

        view.setOnTouchListener(BubbleTouchListener())
        wm.addView(view, bubbleParams)
        bubble = view
    }

    private fun showCloseTarget() {
        if (closeTarget != null) return
        val s = dp(56)
        val v = TextView(this).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(200, 217, 48, 37))
            }
        }
        val p = WindowManager.LayoutParams(
            s, s, overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dp(48)
        }
        runCatching { wm.addView(v, p) }
        closeTarget = v
    }

    private fun hideCloseTarget() {
        closeTarget?.let { runCatching { wm.removeView(it) } }
        closeTarget = null
    }

    private fun isOverCloseTarget(): Boolean {
        val dm = resources.displayMetrics
        val bubbleCx = bubbleParams.x + bubbleParams.width / 2f
        val bubbleCy = bubbleParams.y + bubbleParams.height / 2f
        val targetCx = dm.widthPixels / 2f
        val targetCy = dm.heightPixels - dp(48) - dp(28).toFloat()
        return hypot(bubbleCx - targetCx, bubbleCy - targetCy) < dp(72)
    }

    private fun snapToEdge() {
        val dm = resources.displayMetrics
        val margin = dp(8)
        val targetX = if (bubbleParams.x + bubbleParams.width / 2 < dm.widthPixels / 2) margin
        else dm.widthPixels - bubbleParams.width - margin
        bubbleParams.y = bubbleParams.y.coerceIn(dp(24), dm.heightPixels - bubbleParams.height - dp(24))
        ValueAnimator.ofInt(bubbleParams.x, targetX).apply {
            duration = 220
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                bubbleParams.x = it.animatedValue as Int
                bubble?.let { b -> runCatching { wm.updateViewLayout(b, bubbleParams) } }
            }
            start()
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putInt(KEY_X, targetX).putInt(KEY_Y, bubbleParams.y).apply()
    }

    private inner class BubbleTouchListener : View.OnTouchListener {
        private var startX = 0; private var startY = 0
        private var touchX = 0f; private var touchY = 0f
        private var dragging = false

        override fun onTouch(v: View, e: MotionEvent): Boolean {
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = bubbleParams.x; startY = bubbleParams.y
                    touchX = e.rawX; touchY = e.rawY
                    dragging = false
                    v.animate().alpha(ACTIVE_ALPHA).setDuration(120).start()
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - touchX; val dy = e.rawY - touchY
                    if (!dragging && (abs(dx) > dp(6) || abs(dy) > dp(6))) {
                        dragging = true
                        showCloseTarget()
                    }
                    if (dragging) {
                        bubbleParams.x = startX + dx.toInt()
                        bubbleParams.y = startY + dy.toInt()
                        runCatching { wm.updateViewLayout(v, bubbleParams) }
                        closeTarget?.scaleX = if (isOverCloseTarget()) 1.3f else 1f
                        closeTarget?.scaleY = closeTarget?.scaleX ?: 1f
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().alpha(IDLE_ALPHA).setDuration(300).start()
                    if (dragging) {
                        val close = isOverCloseTarget()
                        hideCloseTarget()
                        if (close) stop(this@EarningsBubbleService) else snapToEdge()
                    } else if (e.action == MotionEvent.ACTION_UP) {
                        runCatching { openAppIntent(locked).send() }
                    }
                }
            }
            return true
        }
    }
}
