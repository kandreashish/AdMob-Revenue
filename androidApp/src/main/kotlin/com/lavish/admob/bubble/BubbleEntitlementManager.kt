package com.lavish.admob.bubble

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/** Access state of the Earnings Bubble for the signed-in user. */
sealed interface BubbleAccess {
    data object Loading : BubbleAccess
    data object SignedOut : BubbleAccess
    data object NotStarted : BubbleAccess
    data class TrialActive(val endsAtMs: Long, val remainingMs: Long) : BubbleAccess
    data object TrialExpired : BubbleAccess
    data object Unlocked : BubbleAccess

    val canUseBubble: Boolean get() = this is TrialActive || this is Unlocked
    /** True only once we know the real state (not Loading / SignedOut). */
    val isKnown: Boolean get() = this != Loading && this != SignedOut
}

/** Promo bookkeeping (RTDB merged with a local copy, so it works even if a write failed/offline). */
data class PromoRecord(
    val shownCount: Int = 0,
    val lastShownAt: Long = 0L,
    val dismissed: Boolean = false,
    /** User already engaged (started trial / bought / turned bubble on). Never auto-promo again. */
    val completed: Boolean = false
)

/**
 * Single source of truth for trial + unlock state.
 *
 * Realtime Database layout:
 *   bubble/{uid}/trialStartedAt   server timestamp, write-once (enforced by rules)
 *   bubble/{uid}/unlocked         false -> true only, requires purchase/token
 *   bubble/{uid}/purchase/...     token, productId, purchasedAt (write-once)
 *   bubble/{uid}/promo/...        shownCount, lastShownAt, dismissed, completed
 *
 * Edge cases handled:
 *  - Cold start: RTDB's first callback can come from an empty local cache, which looks like
 *    "trial not started". We stay in Loading until a SERVER read confirms the state
 *    (or, if offline, we fall back to the per-uid local copy).
 *  - Trial / unlock / promo state is mirrored locally per uid, so an offline start or a failed
 *    write never makes the promo reappear.
 *  - startTrial() when the trial already exists on the server (write denied) => treated as success.
 *  - Account switch => state is re-bound to the new uid; local copies are keyed by uid.
 */
object BubbleEntitlementManager {
    private const val TAG = "EarningsBubble"
    private const val PREFS = "earnings_bubble_state"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val db: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }
    private lateinit var prefs: SharedPreferences

    private val _access = MutableStateFlow<BubbleAccess>(BubbleAccess.Loading)
    val access: StateFlow<BubbleAccess> = _access.asStateFlow()

    private val _promo = MutableStateFlow(PromoRecord())
    val promo: StateFlow<PromoRecord> = _promo.asStateFlow()

    @Volatile private var serverOffsetMs = 0L
    private var uid: String? = null
    private var trialStartedAt: Long? = null
    private var unlocked = false
    @Volatile private var ownedOnPlay = false
    /** Becomes true once the server (or the offline fallback) has confirmed the user's state. */
    private var confirmed = false
    private var trialDurationMs = 24 * 3_600_000L

    private var userRef: DatabaseReference? = null
    private var userListener: ValueEventListener? = null
    private var started = false

    fun serverNow(): Long = System.currentTimeMillis() + serverOffsetMs

    /** Call once from MainActivity (and the service). Re-binds automatically when Firebase user changes. */
    fun start(context: Context) {
        if (started) return
        started = true
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        trialDurationMs = BubbleFirebase.config().trialDurationMs

        db.getReference(".info/serverTimeOffset")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) {
                    serverOffsetMs = s.getValue(Long::class.java) ?: 0L
                    recompute()
                }
                override fun onCancelled(e: DatabaseError) {}
            })

        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val newUid = auth.currentUser?.uid
            if (newUid != uid || userRef == null) bindUser(newUid)
        }

        // Tick every 30s so TrialActive -> TrialExpired flips promptly (matters for short test trials). so TrialActive -> TrialExpired flips without a DB change.
        scope.launch {
            while (isActive) {
                delay(30_000)
                recompute()
            }
        }
    }

    // ---------------- local per-uid mirror ----------------

    private fun k(key: String) = "${uid}_$key"
    private fun localTrialStart(): Long? = prefs.getLong(k("trialStartedAt"), 0L).takeIf { it > 0 }
    private fun localUnlocked(): Boolean = prefs.getBoolean(k("unlocked"), false)
    private fun localPromo() = PromoRecord(
        shownCount = prefs.getInt(k("promoShown"), 0),
        lastShownAt = prefs.getLong(k("promoLast"), 0L),
        dismissed = prefs.getBoolean(k("promoDismissed"), false),
        completed = prefs.getBoolean(k("promoCompleted"), false)
    )

    private fun saveLocal(block: SharedPreferences.Editor.() -> Unit) {
        if (uid != null) prefs.edit().apply(block).apply()
    }

    // ---------------- binding ----------------

    private fun bindUser(newUid: String?) {
        userListener?.let { l -> userRef?.removeEventListener(l) }
        userRef = null
        userListener = null
        uid = newUid
        trialStartedAt = null
        unlocked = false
        confirmed = false
        _promo.value = PromoRecord()

        if (newUid == null) {
            _access.value = BubbleAccess.SignedOut
            return
        }
        _access.value = BubbleAccess.Loading
        // Start from the local mirror so a known trial/unlock shows instantly, even offline.
        trialStartedAt = localTrialStart()
        unlocked = localUnlocked()
        _promo.value = localPromo()

        val ref = db.getReference("bubble").child(newUid)
        val listener = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) = apply(s)
            override fun onCancelled(e: DatabaseError) {
                Log.w(TAG, "Entitlement listener cancelled: ${e.message}")
            }
        }
        ref.keepSynced(true)
        ref.addValueEventListener(listener)
        userRef = ref
        userListener = listener

        // Confirm with a real server read. Offline/timeout => trust the local mirror.
        scope.launch {
            val snap = withTimeoutOrNull(8_000) { runCatching { ref.get().await() }.getOrNull() }
            if (uid != newUid) return@launch
            if (snap != null) apply(snap)
            confirmed = true
            recompute()
        }
    }

    /** Merge a snapshot with the local mirror. Server values only ever ADD state, never remove it. */
    private fun apply(s: DataSnapshot) {
        s.child("trialStartedAt").getValue(Long::class.java)?.let { server ->
            trialStartedAt = server
            saveLocal { putLong(k("trialStartedAt"), server) }
        }
        if (s.child("unlocked").getValue(Boolean::class.java) == true) {
            unlocked = true
            saveLocal { putBoolean(k("unlocked"), true) }
        }
        val p = s.child("promo")
        val local = localPromo()
        _promo.value = PromoRecord(
            shownCount = maxOf(local.shownCount, p.child("shownCount").getValue(Long::class.java)?.toInt() ?: 0),
            lastShownAt = maxOf(local.lastShownAt, p.child("lastShownAt").getValue(Long::class.java) ?: 0L),
            dismissed = local.dismissed || p.child("dismissed").getValue(Boolean::class.java) == true,
            completed = local.completed || p.child("completed").getValue(Boolean::class.java) == true
        )
        recompute()
    }

    private fun recompute() {
        if (uid == null) return
        val start = trialStartedAt
        _access.value = when {
            unlocked || ownedOnPlay -> BubbleAccess.Unlocked
            start != null -> {
                val endsAt = start + trialDurationMs
                val remaining = endsAt - serverNow()
                if (remaining > 0) BubbleAccess.TrialActive(endsAt, remaining) else BubbleAccess.TrialExpired
            }
            // Only claim "not started" once the server has confirmed it.
            !confirmed -> BubbleAccess.Loading
            else -> BubbleAccess.NotStarted
        }
    }

    // ---------------- actions ----------------

    /** Starts the one-time free trial. Idempotent; if the server already has a trial, adopts it. */
    suspend fun startTrial(): Boolean {
        val ref = userRef ?: return false
        if (trialStartedAt != null) return true
        markPromoCompleted()
        return try {
            ref.child("trialStartedAt").setValue(ServerValue.TIMESTAMP).await()
            true
        } catch (e: Exception) {
            // Most likely PERMISSION_DENIED because it already exists (write-once rule). Re-read it.
            Log.w(TAG, "startTrial write failed (${e.message}); re-reading server value")
            val existing = runCatching {
                ref.child("trialStartedAt").get().await().getValue(Long::class.java)
            }.getOrNull()
            if (existing != null) {
                trialStartedAt = existing
                saveLocal { putLong(k("trialStartedAt"), existing) }
                recompute()
                true
            } else false
        }
    }

    /** Play Billing says the user owns the unlock (works even if the RTDB write hasn't happened yet). */
    fun setOwnedOnPlay(owned: Boolean) {
        ownedOnPlay = owned
        if (owned) {
            saveLocal { putBoolean(k("unlocked"), true) }
            markPromoCompleted()
        }
        if (uid == null && owned) _access.value = BubbleAccess.Unlocked else recompute()
    }

    fun isUnlockedInDb(): Boolean = unlocked

    /** Called by the billing layer after a verified + acknowledged purchase. */
    suspend fun markUnlocked(purchaseToken: String, productId: String): Boolean {
        val ref = userRef ?: return false
        return try {
            // purchase/ is write-once; ignore failure if it already exists (restore on another device)
            runCatching {
                ref.child("purchase").setValue(
                    mapOf(
                        "token" to purchaseToken,
                        "productId" to productId,
                        "platform" to "android",
                        "purchasedAt" to ServerValue.TIMESTAMP
                    )
                ).await()
            }
            ref.child("unlocked").setValue(true).await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "markUnlocked failed: ${e.message}")
            false
        }
    }

    fun recordPromoShown() {
        val now = serverNow()
        val cur = _promo.value
        _promo.value = cur.copy(shownCount = cur.shownCount + 1, lastShownAt = now)
        saveLocal { putInt(k("promoShown"), cur.shownCount + 1); putLong(k("promoLast"), now) }
        userRef?.child("promo")?.updateChildren(
            mapOf("shownCount" to ServerValue.increment(1), "lastShownAt" to ServerValue.TIMESTAMP)
        )
    }

    fun recordPromoDismissedForever() {
        _promo.value = _promo.value.copy(dismissed = true)
        saveLocal { putBoolean(k("promoDismissed"), true) }
        userRef?.child("promo")?.child("dismissed")?.setValue(true)
    }

    /** User engaged with the feature: never auto-show the intro promo again. */
    fun markPromoCompleted() {
        if (_promo.value.completed) return
        _promo.value = _promo.value.copy(completed = true)
        saveLocal { putBoolean(k("promoCompleted"), true) }
        userRef?.child("promo")?.child("completed")?.setValue(true)
    }
}
