package com.solarad.app

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.solarad.app.data.AdMobRepository
import com.solarad.app.model.AppMonetization
import com.solarad.app.model.AutoRefreshInterval
import com.solarad.app.model.ThemeMode
import com.solarad.app.model.TimeRange
import com.solarad.app.model.maskPublisherId
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.solarad.app.theme.BotanicalGreen
import com.solarad.app.theme.CoralCrimson
import com.solarad.app.theme.EspressoSlate
import com.solarad.app.theme.OceanAzure
import com.solarad.app.theme.PureWhite
import com.solarad.app.theme.SolarAdTheme
import com.solarad.app.theme.SolarAmber
import com.solarad.app.theme.SolarTheme
import com.solarad.app.ui.components.FormatSegmentedBar
import com.solarad.app.ui.components.IosPullToRefreshBox
import com.solarad.app.ui.components.NavigationTab
import com.solarad.app.ui.components.SetGoalDialog
import com.solarad.app.ui.components.SolarBottomDock
import com.solarad.app.ui.components.UserProfileAvatar
import com.solarad.app.ui.navigation.BackHandler
import com.solarad.app.ui.screens.AnalyticsScreen
import com.solarad.app.ui.screens.AppDetailScreen
import com.solarad.app.ui.screens.AppsScreen
import com.solarad.app.ui.screens.FullScreenEarningScreen
import com.solarad.app.ui.screens.LoginScreen
import com.solarad.app.ui.screens.SplashScreen
import com.solarad.app.ui.screens.TodayScreen
import com.solarad.app.ui.screens.WalletScreen
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    val summary by repository.accountSummary.collectAsState()
    val allApps by repository.allApps.collectAsState()
    val selectedRange by repository.selectedTimeRange.collectAsState()
    val analytics by repository.analytics.collectAsState()
    val selectedAnalyticsRange by repository.selectedAnalyticsRange.collectAsState()
    val wallet by repository.wallet.collectAsState()
    val isRefreshing by repository.isRefreshing.collectAsState()
    val isOffline by repository.isOffline.collectAsState()
    val autoRefreshInterval by repository.autoRefreshInterval.collectAsState()
    val themeMode by repository.themeMode.collectAsState()
    val reportRows by repository.reportRows.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current

    // Auto-refresh periodically ONLY while the screen / app lifecycle is actively in RESUMED state
    DisposableEffect(lifecycleOwner, session.isLoggedIn, autoRefreshInterval) {
        if (!session.isLoggedIn || autoRefreshInterval == AutoRefreshInterval.OFF) {
            return@DisposableEffect onDispose {}
        }

        var refreshJob: Job? = null

        fun startTimer() {
            refreshJob?.cancel()
            refreshJob = CoroutineScope(Dispatchers.Main).launch {
                while (isActive) {
                    val intervalMs = autoRefreshInterval.seconds * 1000L
                    if (intervalMs <= 0) break

                    val lastRefresh = repository.lastRefreshTimestampMs.value
                    val now = Clock.System.now().toEpochMilliseconds()
                    val elapsed = now - lastRefresh
                    val remaining = intervalMs - elapsed

                    if (remaining > 50L) {
                        // Wait for the remaining duration.
                        // If user refreshes at any moment (e.g. at 27s), lastRefreshTimestampMs updates,
                        // and first { it > lastRefresh } immediately wakes up to wait a full 30s more!
                        withTimeoutOrNull(remaining) {
                            repository.lastRefreshTimestampMs.first { it > lastRefresh }
                        }
                        // Recalculate remaining time from the newly recorded refresh timestamp
                        continue
                    }

                    if (!isRefreshing && session.isLoggedIn) {
                        repository.refreshRealData()
                    }
                    delay(500L)
                }
            }
        }

        fun stopTimer() {
            refreshJob?.cancel()
            refreshJob = null
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> startTimer()
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> stopTimer()
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            startTimer()
        }

        onDispose {
            stopTimer()
            lifecycleOwner.lifecycle.removeObserver(observer)
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

    fun navigateToTab(tab: NavigationTab) {
        if (tab != currentTab) {
            tabBackStack.remove(tab)
            tabBackStack.add(tab)
            currentTabName = tab.name
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
                        isFullScreenTodayVisible = false
                    }
                    selectedAppForModal != null -> {
                        selectedAppForModal = null
                    }
                    showProfileDialog -> {
                        showProfileDialog = false
                    }
                    canPopTab -> {
                        tabBackStack.removeAt(tabBackStack.lastIndex)
                        currentTabName = tabBackStack.last().name
                    }
                }
            }

            if (isFullScreenTodayVisible) {
                FullScreenEarningScreen(
                    summary = summary,
                    onExit = { isFullScreenTodayVisible = false }
                )
            } else if (selectedAppForModal != null) {
                AppDetailScreen(
                    app = selectedAppForModal!!,
                    reportRows = reportRows,
                    initialTimeRange = selectedRange,
                    currencySymbol = session.currencySymbol,
                    onBack = { selectedAppForModal = null }
                )
            } else {
                Scaffold(
                    containerColor = SolarTheme.colors.background,
                bottomBar = {
                    SolarBottomDock(
                        currentTab = currentTab,
                        onTabSelected = { navigateToTab(it) }
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = paddingValues.calculateTopPadding())
                ) {
                    val currentAccountName = session.userName.ifBlank { "My" } + "'s AdMob"

                    IosPullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { repository.refreshRealData() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when (currentTab) {
                            NavigationTab.TODAY -> {
                                TodayScreen(
                                    summary = summary,
                                    apps = allApps,
                                    selectedRange = selectedRange,
                                    accountName = currentAccountName,
                                    avatarUrl = session.avatarUrl,
                                    isRefreshing = isRefreshing,
                                    isOffline = isOffline,
                                    onSelectRange = { repository.setTimeRange(it) },
                                    onViewAllApps = { navigateToTab(NavigationTab.APPS) },
                                    onSelectApp = { selectedAppForModal = it },
                                    onAccountClick = { showProfileDialog = true },
                                    onSelectCustomRange = { start, end ->
                                        repository.setTimeRange(TimeRange.CUSTOM, start, end)
                                    },
                                    onSetDailyGoal = { repository.setCustomDailyGoal(it) },
                                    onOpenFullScreen = { isFullScreenTodayVisible = true }
                                )
                            }

                            NavigationTab.APPS -> {
                                AppsScreen(
                                    apps = allApps,
                                    accountName = currentAccountName,
                                    avatarUrl = session.avatarUrl,
                                    isOffline = isOffline,
                                    onSelectApp = { selectedAppForModal = it },
                                    onAddApp = { name, platform ->
                                        repository.addApp(name, platform)
                                    },
                                    onAccountClick = { showProfileDialog = true }
                                )
                            }

                            NavigationTab.ANALYTICS -> {
                                AnalyticsScreen(
                                    analytics = analytics,
                                    selectedRange = selectedAnalyticsRange,
                                    publisherId = session.publisherId,
                                    accountName = currentAccountName,
                                    avatarUrl = session.avatarUrl,
                                    isOffline = isOffline,
                                    onRangeSelect = { repository.setAnalyticsRange(it) },
                                    onProfileClick = { showProfileDialog = true },
                                    onExportReport = {}
                                )
                            }

                            NavigationTab.WALLET -> {
                                WalletScreen(
                                    wallet = wallet,
                                    publisherId = session.publisherId,
                                    accountName = currentAccountName,
                                    avatarUrl = session.avatarUrl,
                                    isOffline = isOffline,
                                    onProfileClick = { showProfileDialog = true },
                                    onManagePayoutMethods = { showProfileDialog = true },
                                    onDownloadReceipt = { /* handled in screen */ }
                                )
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

                    val syncTransition = rememberInfiniteTransition()
                    val syncRotation by syncTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 900, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        )
                    )

                    AnimatedVisibility(
                        visible = showSyncPill,
                        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                    ) {
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
                                        .rotate(syncRotation)
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

            // Profile & Settings Dialog
            if (showProfileDialog) {
                AlertDialog(
                    onDismissRequest = { showProfileDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showProfileDialog = false }) {
                            Text("Done", color = SolarAmber, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = {
                                showProfileDialog = false
                                tabBackStack.clear()
                                tabBackStack.add(NavigationTab.TODAY)
                                currentTabName = NavigationTab.TODAY.name
                                repository.logout()
                                onSignOutRequest?.invoke()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFDC2626)
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
                                                modifier = Modifier.clickable { showFullPublisherId = !showFullPublisherId }
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
                                            onClick = { showProfileGoalDialog = true },
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
                                                            if (SolarTheme.colors.isDark) EspressoSlate else PureWhite
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
                    containerColor = SolarTheme.colors.cardBackground,
                    shape = RoundedCornerShape(20.dp)
                )
            }

            if (showProfileGoalDialog) {
                SetGoalDialog(
                    currentGoal = summary.dailyGoal,
                    currencySymbol = summary.currencySymbol,
                    isCustomGoal = summary.isCustomGoal,
                    onDismiss = { showProfileGoalDialog = false },
                    onSaveGoal = { newGoal ->
                        repository.setCustomDailyGoal(newGoal)
                    },
                    onResetToAuto = {
                        repository.setCustomDailyGoal(null)
                    }
                )
            }
        }
    }
}
}
