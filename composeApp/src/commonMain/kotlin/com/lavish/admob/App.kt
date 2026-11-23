package com.lavish.admob

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.lavish.admob.data.AdMobRepository
import com.lavish.admob.analytics.MixpanelTracker
import com.lavish.admob.model.AppMonetization
import com.lavish.admob.model.AutoRefreshInterval
import com.lavish.admob.model.ThemeMode
import com.lavish.admob.model.TimeRange
import com.lavish.admob.model.UserSession
import com.lavish.admob.model.maskPublisherId
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.lavish.admob.theme.BotanicalGreen
import com.lavish.admob.theme.CoralCrimson
import com.lavish.admob.theme.EspressoSlate
import com.lavish.admob.theme.OceanAzure
import com.lavish.admob.theme.PureWhite
import com.lavish.admob.theme.SolarAdTheme
import com.lavish.admob.theme.SolarAmber
import com.lavish.admob.theme.SolarTheme
import com.lavish.admob.ui.components.FormatSegmentedBar
import com.lavish.admob.ui.components.IosPullToRefreshBox
import com.lavish.admob.ui.components.NavigationTab
import com.lavish.admob.ui.components.SetGoalDialog
import com.lavish.admob.ui.components.ShimmerScreenSkeleton
import com.lavish.admob.ui.components.SolarBottomDock
import com.lavish.admob.ui.components.UserProfileAvatar
import com.lavish.admob.ui.navigation.BackHandler
import com.lavish.admob.ui.screens.AnalyticsScreen
import com.lavish.admob.ui.screens.AppDetailScreen
import com.lavish.admob.ui.screens.AppsScreen
import com.lavish.admob.ui.screens.FullScreenEarningScreen
import com.lavish.admob.ui.screens.LoginScreen
import com.lavish.admob.ui.screens.ReportsScreen
import com.lavish.admob.ui.screens.SplashScreen
import com.lavish.admob.ui.screens.TodayScreen
import com.lavish.admob.ui.screens.WalletScreen
import kotlin.time.Clock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun App(
    repository: AdMobRepository = remember { AdMobRepository() },
    onGoogleSignInRequest: (() -> Unit)? = null,
    onSignOutRequest: (() -> Unit)? = null
) {
    val session by repository.session.collectAsState()
    val authError by repository.authError.collectAsState()
    // Only light, rarely-changing flows are collected at the root. Heavy data flows
    // (summary, apps, reports, analytics, wallet) are collected inside the tab / dialog
    // that uses them, so one emission doesn't recompose the whole app tree.
    val isRefreshing by repository.isRefreshing.collectAsState()
    val isOffline by repository.isOffline.collectAsState()
    val autoRefreshInterval by repository.autoRefreshInterval.collectAsState()
    val themeMode by repository.themeMode.collectAsState()
    val tracker = repository.tracker

    val lifecycleOwner = LocalLifecycleOwner.current

    // Flush batched analytics when the app goes to background so queued events aren't lost.
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) repository.tracker.flush()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Auto-refresh periodically ONLY while the app is RESUMED. Structured: cancelled automatically
    // when the composition leaves or the keys change; suspended (not polling) while paused.
    LaunchedEffect(lifecycleOwner, session.isLoggedIn, autoRefreshInterval) {
        if (!session.isLoggedIn || autoRefreshInterval == AutoRefreshInterval.OFF) return@LaunchedEffect
        val intervalMs = autoRefreshInterval.seconds * 1000L
        if (intervalMs <= 0) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                val lastRefresh = repository.lastRefreshTimestampMs.value
                val remaining = intervalMs - (Clock.System.now().toEpochMilliseconds() - lastRefresh)
                if (remaining > 50L) {
                    // A manual refresh bumps the timestamp and restarts the countdown
                    withTimeoutOrNull(remaining) {
                        repository.lastRefreshTimestampMs.first { it > lastRefresh }
                    }
                    continue
                }
                if (!repository.isRefreshing.value && repository.session.value.isLoggedIn) {
                    repository.refreshRealData()
                }
                delay(500L)
            }
        }
    }

    var currentTabName by rememberSaveable { mutableStateOf(NavigationTab.TODAY.name) }
    val currentTab = remember(currentTabName) {
        try {
            NavigationTab.valueOf(currentTabName)
        } catch (_: Exception) {
            NavigationTab.TODAY
        }
    }
    val tabBackStack = remember { mutableStateListOf(NavigationTab.TODAY) }

    var isAppLoading by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        if (isAppLoading) {
            delay(850L)
            isAppLoading = false
        }
    }

    var showProfileDialog by rememberSaveable { mutableStateOf(false) }
    var showProfileGoalDialog by rememberSaveable { mutableStateOf(false) }
    var showFullPublisherId by rememberSaveable { mutableStateOf(false) }
    var selectedAppForModal by remember { mutableStateOf<AppMonetization?>(null) }
    var isFullScreenTodayVisible by rememberSaveable { mutableStateOf(false) }

    fun navigateToTab(tab: NavigationTab, source: String = "bottom_dock") {
        if (tab != currentTab) {
            tracker.trackTabSwitched(fromTab = currentTab.name, toTab = tab.name, source = source)
            tabBackStack.remove(tab)
            tabBackStack.add(tab)
            currentTabName = tab.name
        }
    }

    var lastTrackedScreen by remember { mutableStateOf<String?>(null) }
    val currentScreenName = when {
        isAppLoading -> "Splash Screen"
        !session.isLoggedIn -> "Login Screen"
        isFullScreenTodayVisible -> "Full Screen Today Earnings HUD"
        selectedAppForModal != null -> "App Detail: ${selectedAppForModal?.name}"
        showProfileDialog -> "User Profile & Settings Modal"
        showProfileGoalDialog -> "Set Daily Goal Modal"
        currentTab == NavigationTab.TODAY -> "Today Dashboard"
        currentTab == NavigationTab.APPS -> "Apps Directory"
        currentTab == NavigationTab.REPORTS -> "Reports"
        currentTab == NavigationTab.ANALYTICS -> "Analytics & Performance"
        currentTab == NavigationTab.WALLET -> "Wallet & Payments"
        else -> "Dashboard"
    }

    LaunchedEffect(currentScreenName, isAppLoading, session.isLoggedIn) {
        if (currentScreenName != lastTrackedScreen) {
            val previous = lastTrackedScreen
            lastTrackedScreen = currentScreenName

            val meta = mutableMapOf<String, Any?>()
            if (session.isLoggedIn) {
                meta["tab_name"] = currentTab.name
                meta["is_offline"] = isOffline
                if (currentScreenName == "Today Dashboard" || isFullScreenTodayVisible) {
                    val summary = repository.accountSummary.value
                    meta["today_revenue"] = summary.todayRevenue
                    meta["yesterday_revenue"] = summary.yesterdayRevenue
                    meta["daily_goal"] = summary.dailyGoal
                    meta["currency"] = summary.currencySymbol
                }
            }
            if (selectedAppForModal != null) {
                meta["app_name"] = selectedAppForModal!!.name
                meta["app_platform"] = selectedAppForModal!!.platform
                meta["estimated_earnings"] = selectedAppForModal!!.todayRevenue
            }

            tracker.trackScreenViewed(
                screenName = currentScreenName,
                previousScreen = previous,
                tabName = if (session.isLoggedIn && !isFullScreenTodayVisible && selectedAppForModal == null) currentTab.name else null,
                additionalData = meta
            )
        }
    }

    SolarAdTheme(themeMode = themeMode) {
        if (isAppLoading) {
            SplashScreen()
        } else if (!session.isLoggedIn) {
            // Login Screen: Back button is not intercepted here, allowing normal system app exit
            LoginScreen(
                authError = authError,
                isAuthenticating = isRefreshing,
                themeMode = themeMode,
                onSelectTheme = { repository.setThemeMode(it) },
                onClearError = { repository.clearAuthError() },
                onContinueWithGoogle = {
                    if (onGoogleSignInRequest != null) {
                        onGoogleSignInRequest()
                    } else {
                        repository.setAuthError("Google Sign-In is required to access the app.")
                    }
                },
                onContinueWithToken = { token -> repository.loginWithApiToken(token) }
            )
        } else {
            // Backstack Handling when Authenticated
            // Priority 0: Dismiss full-screen Today's Earnings view
            // Priority 1: Dismiss selected App Detail modal
            // Priority 2: Dismiss Profile dialog
            // Priority 3: Pop tab backstack if more than 1 tab visited
            // Priority 4: On TODAY tab with no remaining history -> disabled, lets Android system exit app
            val canPopTab = tabBackStack.size > 1
            val shouldInterceptBack = isFullScreenTodayVisible || selectedAppForModal != null || showProfileDialog || canPopTab

            BackHandler(enabled = shouldInterceptBack) {
                when {
                    isFullScreenTodayVisible -> {
                        tracker.trackBackPressed(currentScreen = "Full Screen Today Earnings HUD", actionTaken = "dismiss_fullscreen")
                        tracker.trackModalDismissed("Full Screen Today Earnings HUD", dismissMethod = "system_back")
                        isFullScreenTodayVisible = false
                    }
                    selectedAppForModal != null -> {
                        val appName = selectedAppForModal?.name ?: "App"
                        tracker.trackBackPressed(currentScreen = "App Detail: $appName", actionTaken = "dismiss_app_detail")
                        tracker.trackModalDismissed("App Detail Modal", dismissMethod = "system_back")
                        selectedAppForModal = null
                    }
                    showProfileDialog -> {
                        tracker.trackBackPressed(currentScreen = "User Profile & Settings Modal", actionTaken = "dismiss_profile")
                        tracker.trackModalDismissed("User Profile Modal", dismissMethod = "system_back")
                        showProfileDialog = false
                    }
                    canPopTab -> {
                        val prevTab = tabBackStack.last().name
                        tabBackStack.removeAt(tabBackStack.lastIndex)
                        val targetTab = tabBackStack.last().name
                        tracker.trackBackPressed(currentScreen = prevTab, actionTaken = "pop_tab_to_$targetTab")
                        tracker.trackTabSwitched(fromTab = prevTab, toTab = targetTab, source = "system_back_button")
                        currentTabName = targetTab
                    }
                }
            }

            if (isFullScreenTodayVisible) Scoped {
                val summary by repository.accountSummary.collectAsState()
                val lastSyncMs by repository.lastRefreshTimestampMs.collectAsState()
                FullScreenEarningScreen(
                    summary = summary,
                    lastSyncMs = lastSyncMs,
                    onExit = {
                        tracker.trackModalDismissed("Full Screen Today Earnings HUD", dismissMethod = "exit_button")
                        isFullScreenTodayVisible = false
                    }
                )
            } else if (showProfileDialog) Scoped {
                // Profile & Settings screen (own composable: collects only what it shows)
                ProfileSettingsDialog(
                    repository = repository,
                    session = session,
                    showFullPublisherId = showFullPublisherId,
                    onTogglePublisherId = { showFullPublisherId = !showFullPublisherId },
                    onDismiss = { method ->
                        tracker.trackModalDismissed("User Profile Modal", dismissMethod = method)
                        showProfileDialog = false
                    },
                    onSignOut = {
                        tracker.trackModalDismissed("User Profile Modal", dismissMethod = "sign_out_button")
                        showProfileDialog = false
                        tabBackStack.clear()
                        tabBackStack.add(NavigationTab.TODAY)
                        currentTabName = NavigationTab.TODAY.name
                        repository.logout()
                        onSignOutRequest?.invoke()
                    },
                    onEditGoal = {
                        tracker.trackModalOpened("Set Daily Goal Modal", triggerSource = "profile_dialog_edit_goal")
                        showProfileGoalDialog = true
                    }
                )
                // Goal dialog is composed right after the Settings screen (same branch) so it always
                // draws on top of it instead of behind/below it.
                if (showProfileGoalDialog) ProfileGoalDialog(repository, tracker) { showProfileGoalDialog = false }
            } else if (selectedAppForModal != null) Scoped {
                val reportRows by repository.reportRows.collectAsState()
                val selectedRange by repository.selectedTimeRange.collectAsState()
                AppDetailScreen(
                    app = selectedAppForModal!!,
                    reportRows = reportRows,
                    initialTimeRange = selectedRange,
                    currencySymbol = session.currencySymbol,
                    onBack = {
                        tracker.trackModalDismissed("App Detail Modal", dismissMethod = "back_button")
                        selectedAppForModal = null
                    }
                )
            } else {
                Scaffold(
                    containerColor = SolarTheme.colors.background,
                bottomBar = {
                    SolarBottomDock(
                        currentTab = currentTab,
                        onTabSelected = { navigateToTab(it, source = "bottom_dock") }
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = paddingValues.calculateTopPadding())
                ) {
                    val currentAccountName = remember(session.userName) { session.userName.ifBlank { "My" } + "'s AdMob" }

                    IosPullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { repository.refreshRealData(); if (currentTab == NavigationTab.REPORTS) repository.loadHourly(true) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = {
                                val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                                (fadeIn(tween(220, delayMillis = 60)) +
                                    slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { w -> dir * w / 10 }) togetherWith
                                    (fadeOut(tween(120)) +
                                        slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { w -> -dir * w / 10 }) using
                                    SizeTransform(clip = false)
                            },
                            contentAlignment = Alignment.TopStart,
                            label = "tabContent",
                            modifier = Modifier.fillMaxSize()
                        ) { animatedTab ->
                        // First load / slow network with nothing cached yet: show a shimmer skeleton
                        // instead of empty zero-value screens.
                        val firstLoadRows by repository.reportRows.collectAsState()
                        if (isRefreshing && firstLoadRows.isEmpty()) {
                            ShimmerScreenSkeleton()
                        } else
                        when (animatedTab) {
                            NavigationTab.TODAY -> Scoped {
                                val summary by repository.accountSummary.collectAsState()
                                val allApps by repository.allApps.collectAsState()
                                val selectedRange by repository.selectedTimeRange.collectAsState()
                                TodayScreen(
                                    summary = summary,
                                    apps = allApps,
                                    selectedRange = selectedRange,
                                    accountName = currentAccountName,
                                    avatarUrl = session.avatarUrl,
                                    isRefreshing = isRefreshing,
                                    isOffline = isOffline,
                                    onSelectRange = { repository.setTimeRange(it) },
                                    onViewAllApps = { navigateToTab(NavigationTab.APPS, source = "view_all_apps_link") },
                                    onSelectApp = {
                                        tracker.trackAppSelected(it.name, it.platform, it.todayRevenue, session.currencySymbol)
                                        tracker.trackModalOpened("App Detail Modal", triggerSource = "today_recent_apps", details = mapOf("app_name" to it.name))
                                        selectedAppForModal = it
                                    },
                                    onAccountClick = {
                                        tracker.trackModalOpened("User Profile Modal", triggerSource = "today_avatar")
                                        showProfileDialog = true
                                    },
                                    onSelectCustomRange = { start, end ->
                                        repository.setTimeRange(TimeRange.CUSTOM, start, end)
                                    },
                                    onSetDailyGoal = { repository.setCustomDailyGoal(it) },
                                    onOpenFullScreen = {
                                        tracker.trackModalOpened(
                                            "Full Screen Today Earnings HUD",
                                            triggerSource = "today_fullscreen_button",
                                            details = mapOf(
                                                "today_revenue" to summary.todayRevenue,
                                                "currency" to summary.currencySymbol
                                            )
                                        )
                                        isFullScreenTodayVisible = true
                                    }
                                )
                            }

                            NavigationTab.APPS -> Scoped {
                                val allApps by repository.allApps.collectAsState()
                                AppsScreen(
                                    apps = allApps,
                                    accountName = currentAccountName,
                                    avatarUrl = session.avatarUrl,
                                    isOffline = isOffline,
                                    onSelectApp = {
                                        tracker.trackAppSelected(it.name, it.platform, it.todayRevenue, session.currencySymbol)
                                        tracker.trackModalOpened("App Detail Modal", triggerSource = "apps_screen_list", details = mapOf("app_name" to it.name))
                                        selectedAppForModal = it
                                    },
                                    onAddApp = { name, platform ->
                                        repository.addApp(name, platform)
                                    },
                                    onAccountClick = {
                                        tracker.trackModalOpened("User Profile Modal", triggerSource = "apps_avatar")
                                        showProfileDialog = true
                                    }
                                )
                            }

                            NavigationTab.REPORTS -> Scoped {
                                val reportRows by repository.reportRows.collectAsState()
                                val summary by repository.accountSummary.collectAsState()
                                val reportApps by repository.allApps.collectAsState()
                                val hourlyState by repository.hourly.collectAsState()
                                ReportsScreen(
                                    hourly = hourlyState,
                                    onLoadHourly = { repository.loadHourly(it) },
                                    rows = reportRows,
                                    apps = reportApps,
                                    today = remember(reportRows, summary.lastSyncTime) { repository.accountToday() },
                                    currencySymbol = session.currencySymbol,
                                    currencyCode = session.currencyCode,
                                    accountName = currentAccountName,
                                    avatarUrl = session.avatarUrl,
                                    isOffline = isOffline,
                                    onProfileClick = {
                                        tracker.trackModalOpened("User Profile Modal", triggerSource = "reports_avatar")
                                        showProfileDialog = true
                                    }
                                )
                            }

                            NavigationTab.ANALYTICS -> Scoped {
                                val analytics by repository.analytics.collectAsState()
                                val selectedAnalyticsRange by repository.selectedAnalyticsRange.collectAsState()
                                AnalyticsScreen(
                                    analytics = analytics,
                                    selectedRange = selectedAnalyticsRange,
                                    publisherId = session.publisherId,
                                    accountName = currentAccountName,
                                    avatarUrl = session.avatarUrl,
                                    isOffline = isOffline,
                                    onRangeSelect = { repository.setAnalyticsRange(it) },
                                    onProfileClick = {
                                        tracker.trackModalOpened("User Profile Modal", triggerSource = "analytics_avatar")
                                        showProfileDialog = true
                                    },
                                    onExportReport = {}
                                )
                            }

                            NavigationTab.WALLET -> Scoped {
                                val wallet by repository.wallet.collectAsState()
                                WalletScreen(
                                    wallet = wallet,
                                    publisherId = session.publisherId,
                                    accountName = currentAccountName,
                                    avatarUrl = session.avatarUrl,
                                    isOffline = isOffline,
                                    onProfileClick = {
                                        tracker.trackModalOpened("User Profile Modal", triggerSource = "wallet_avatar")
                                        showProfileDialog = true
                                    },
                                    onManagePayoutMethods = {
                                        tracker.trackModalOpened("User Profile Modal", triggerSource = "wallet_manage_payout")
                                        showProfileDialog = true
                                    },
                                    onDownloadReceipt = { /* handled in screen */ }
                                )
                            }
                        }
                        }
                    }

                    // Floating Live Sync notification pill
                    var showSyncPill by remember { mutableStateOf(false) }
                    LaunchedEffect(isRefreshing) {
                        if (isRefreshing) {
                            showSyncPill = true
                        } else if (showSyncPill) {
                            delay(600L)
                            showSyncPill = false
                        }
                    }

                    AnimatedVisibility(
                        visible = showSyncPill,
                        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                    ) {
                        // Infinite transition lives inside the pill so it only ticks while visible
                        val syncTransition = rememberInfiniteTransition(label = "syncSpin")
                        val syncRotation = syncTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 900, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "syncRotation"
                        )
                        Surface(
                            color = EspressoSlate,
                            shape = RoundedCornerShape(999.dp),
                            shadowElevation = 8.dp,
                            border = BorderStroke(1.dp, SolarAmber.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = SolarAmber,
                                    modifier = Modifier
                                        .size(15.dp)
                                        .graphicsLayer { rotationZ = syncRotation.value }
                                )
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = "Syncing...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PureWhite
                                )
                            }
                        }
                    }

                    // Floating error / auth warning banner
                    AnimatedVisibility(
                        visible = authError != null && !isRefreshing,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp, start = 20.dp, end = 20.dp)
                    ) {
                        Surface(
                            color = CoralCrimson,
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = authError ?: "",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PureWhite,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Dismiss",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite,
                                    modifier = Modifier.clickable { repository.clearAuthError() }
                                )
                            }
                        }
                    }
                }
            }

        }
    }
}
}

@Composable
private fun ProfileGoalDialog(repository: AdMobRepository, tracker: MixpanelTracker, onClose: () -> Unit) {
    val summary by repository.accountSummary.collectAsState()
    val selectedRange by repository.selectedTimeRange.collectAsState()
    SetGoalDialog(
        currentGoal = summary.dailyGoal,
        currencySymbol = summary.currencySymbol,
        isCustomGoal = summary.isCustomGoal,
        selectedRange = selectedRange,
        periodDays = summary.periodDays,
        onDismiss = {
            tracker.trackModalDismissed("Set Daily Goal Modal", dismissMethod = "cancel_button")
            onClose()
        },
        onSaveGoal = { newGoal ->
            tracker.trackModalDismissed("Set Daily Goal Modal", dismissMethod = "save_goal_button")
            repository.setCustomDailyGoal(newGoal)
        },
        onResetToAuto = {
            tracker.trackModalDismissed("Set Daily Goal Modal", dismissMethod = "reset_auto_button")
            repository.setCustomDailyGoal(null)
        }
    )
}

/** Gives [content] its own recomposition scope so state read inside it only invalidates this block. */
@Composable
private fun Scoped(content: @Composable () -> Unit) {
    content()
}

@Composable
private fun ProfileSettingsDialog(
    repository: AdMobRepository,
    session: UserSession,
    showFullPublisherId: Boolean,
    onTogglePublisherId: () -> Unit,
    onDismiss: (method: String) -> Unit,
    onSignOut: () -> Unit,
    onEditGoal: () -> Unit
) {
    val summary by repository.accountSummary.collectAsState()
    val themeMode by repository.themeMode.collectAsState()
    val autoRefreshInterval by repository.autoRefreshInterval.collectAsState()

        SettingsScreenScaffold(
            onDismissRequest = {
                onDismiss("backdrop_click")
            },
            confirmButton = {
                TextButton(onClick = {
                    onDismiss("done_button")
                }) {
                    Text("Done", color = SolarAmber, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        onSignOut()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFD93025)
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = SolarTheme.colors.primaryContainer,
                        modifier = Modifier.size(42.dp),
                        border = BorderStroke(1.dp, SolarTheme.colors.borderMedium)
                    ) {
                        UserProfileAvatar(
                            avatarUrl = session.avatarUrl,
                            userName = session.userName,
                            modifier = Modifier.fillMaxSize(),
                            iconSize = 24.dp
                        )
                    }
                    Column {
                        Text(
                            text = session.userName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = SolarTheme.colors.textPrimary
                        )
                        Text(
                            text = session.email,
                            fontSize = 12.sp,
                            color = SolarTheme.colors.textSecondary
                        )
                    }
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    // Publisher ID row (Dashed second half for privacy by default)
                    Surface(
                        color = SolarTheme.colors.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PUBLISHER ID",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SolarTheme.colors.textSecondary,
                                    letterSpacing = 1.sp
                                )
                                if (session.publisherId.isNotBlank()) {
                                    Surface(
                                        color = Color.Transparent,
                                        modifier = Modifier.clickable { onTogglePublisherId() }
                                    ) {
                                        Text(
                                            text = if (showFullPublisherId) "Hide" else "Reveal",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SolarAmber
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (showFullPublisherId) {
                                    session.publisherId.ifBlank { "Not Connected" }
                                } else {
                                    val masked = maskPublisherId(session.publisherId)
                                    masked.ifBlank { "Not Connected" }
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SolarTheme.colors.textPrimary
                            )
                        }
                    }

                    // Daily Revenue Goal Card
                    Surface(
                        color = SolarTheme.colors.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "DAILY REVENUE GOAL",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SolarTheme.colors.textSecondary,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = if (summary.isCustomGoal) "Custom" else "Smart Auto",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (summary.isCustomGoal) SolarAmber else BotanicalGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            val goalDisplay = if (summary.dailyGoal % 1.0 == 0.0) {
                                summary.dailyGoal.toInt().toString()
                            } else {
                                summary.dailyGoal.toString()
                            }
                            Text(
                                text = "Target: ${summary.currencySymbol}$goalDisplay / day",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SolarTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (summary.isCustomGoal) {
                                    "Target sets your daily milestone and scales across 7d and 30d."
                                } else {
                                    "Dynamically scaled based on your recent 7-day revenue average."
                                },
                                fontSize = 11.sp,
                                color = SolarTheme.colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        onEditGoal()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SolarAmber),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = PureWhite
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (summary.isCustomGoal) "Change Goal" else "Set Custom Goal",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PureWhite
                                    )
                                }
                                if (summary.isCustomGoal) {
                                    OutlinedButton(
                                        onClick = { repository.setCustomDailyGoal(null) },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Reset Auto",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SolarTheme.colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Platform-specific settings (Android: Earnings Bubble)
                    com.lavish.admob.platform.PlatformSlots.settingsRow?.invoke()

                    // Appearance / Theme Card
                    Surface(
                        color = SolarTheme.colors.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "APPEARANCE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SolarTheme.colors.textSecondary,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = themeMode.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SolarAmber
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Choose light, dark, or follow device theme",
                                fontSize = 11.sp,
                                color = SolarTheme.colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Theme selector pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ThemeMode.entries.forEach { mode ->
                                    val isSelected = mode == themeMode
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { repository.setThemeMode(mode) },
                                        color = if (isSelected) SolarTheme.colors.pillSelectedBg else SolarTheme.colors.cardBackground,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) SolarAmber else SolarTheme.colors.borderSubtle
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 7.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = when (mode) {
                                                    ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                                    ThemeMode.LIGHT -> Icons.Default.LightMode
                                                    ThemeMode.DARK -> Icons.Default.DarkMode
                                                },
                                                contentDescription = null,
                                                modifier = Modifier.size(13.dp),
                                                tint = if (isSelected) {
                                                    PureWhite
                                                } else {
                                                    SolarTheme.colors.textSecondary
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = mode.label,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) SolarTheme.colors.pillSelectedText else SolarTheme.colors.textPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Auto-Refresh Configuration Card
                    Surface(
                        color = SolarTheme.colors.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "AUTO-REFRESH",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SolarTheme.colors.textSecondary,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = if (autoRefreshInterval == AutoRefreshInterval.OFF) "Disabled" else "Every ${autoRefreshInterval.label}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (autoRefreshInterval == AutoRefreshInterval.OFF) SolarTheme.colors.textSecondary else BotanicalGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Updates live metrics while screen is active",
                                fontSize = 11.sp,
                                color = SolarTheme.colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Interval selector pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AutoRefreshInterval.entries.forEach { interval ->
                                    val isSelected = interval == autoRefreshInterval
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { repository.setAutoRefreshInterval(interval) },
                                        color = if (isSelected) SolarTheme.colors.pillSelectedBg else SolarTheme.colors.cardBackground,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) SolarAmber else SolarTheme.colors.borderSubtle
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 7.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = interval.label,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) SolarTheme.colors.pillSelectedText else SolarTheme.colors.textPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Sync status row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(BotanicalGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AdMob API Reporting v1 Live",
                                fontSize = 11.sp,
                                color = SolarTheme.colors.textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        ElevatedButton(
                            onClick = { repository.refreshRealData() },
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = SolarTheme.colors.surfaceVariant,
                                contentColor = SolarTheme.colors.textPrimary
                            ),
                            shape = RoundedCornerShape(999.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            containerColor = SolarTheme.colors.cardBackground
        )
}

/**
 * Full-screen settings page. Keeps the same slots the old AlertDialog used
 * (title / text / confirm / dismiss) so the settings content didn't need to change.
 */
@Composable
private fun SettingsScreenScaffold(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    containerColor: Color
) {
    Surface(color = containerColor, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // Top bar: back + profile header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            ) {
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SolarTheme.colors.textPrimary
                    )
                }
                Spacer(Modifier.width(4.dp))
                title()
            }
            HorizontalDivider(color = SolarTheme.colors.borderSubtle)
            // Scrollable settings content
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) { text() }
            HorizontalDivider(color = SolarTheme.colors.borderSubtle)
            // Bottom actions
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                dismissButton()
                confirmButton()
            }
        }
    }
}
