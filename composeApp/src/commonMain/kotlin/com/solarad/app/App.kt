package com.solarad.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solarad.app.data.AdMobRepository
import com.solarad.app.model.AppMonetization
import com.solarad.app.theme.BorderMedium
import com.solarad.app.theme.BorderSubtle
import com.solarad.app.theme.BotanicalGreen
import com.solarad.app.theme.BotanicalGreenLight
import com.solarad.app.theme.EspressoSlate
import com.solarad.app.theme.LinenSand
import com.solarad.app.theme.MutedSlate
import com.solarad.app.theme.OceanAzure
import com.solarad.app.theme.OceanAzureLight
import com.solarad.app.theme.PureWhite
import com.solarad.app.theme.SolarAdTheme
import com.solarad.app.theme.SolarAmber
import com.solarad.app.theme.SolarAmberLight
import com.solarad.app.ui.components.FormatSegmentedBar
import com.solarad.app.ui.components.NavigationTab
import com.solarad.app.ui.components.SolarBottomDock
import com.solarad.app.ui.screens.AnalyticsScreen
import com.solarad.app.ui.screens.AppsScreen
import com.solarad.app.ui.screens.LoginScreen
import com.solarad.app.ui.screens.TodayScreen
import com.solarad.app.ui.screens.WalletScreen

@Composable
fun App() {
    val repository = remember { AdMobRepository() }
    val session by repository.session.collectAsState()
    val summary by repository.accountSummary.collectAsState()
    val allApps by repository.allApps.collectAsState()
    val selectedRange by repository.selectedTimeRange.collectAsState()
    val analytics by repository.analytics.collectAsState()
    val selectedAnalyticsRange by repository.selectedAnalyticsRange.collectAsState()
    val wallet by repository.wallet.collectAsState()
    val isRefreshing by repository.isRefreshing.collectAsState()

    var currentTab by remember { mutableStateOf(NavigationTab.TODAY) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var selectedAppForModal by remember { mutableStateOf<AppMonetization?>(null) }
    var showSyncToast by remember { mutableStateOf(false) }

    SolarAdTheme {
        if (!session.isLoggedIn) {
            LoginScreen(
                onContinueWithGoogle = { repository.loginWithGoogle() },
                onContinueWithToken = { token -> repository.loginWithApiToken(token) }
            )
        } else {
            Scaffold(
                containerColor = LinenSand,
                bottomBar = {
                    SolarBottomDock(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it }
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = paddingValues.calculateBottomPadding() / 2)
                ) {
                    when (currentTab) {
                        NavigationTab.TODAY -> {
                            TodayScreen(
                                summary = summary,
                                apps = allApps,
                                selectedRange = selectedRange,
                                onSelectRange = { repository.setTimeRange(it) },
                                onViewAllApps = { currentTab = NavigationTab.APPS },
                                onSelectApp = { selectedAppForModal = it },
                                onAccountClick = { showProfileDialog = true }
                            )
                        }

                        NavigationTab.APPS -> {
                            AppsScreen(
                                apps = allApps,
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
                                onRangeSelect = { repository.setAnalyticsRange(it) },
                                onProfileClick = { showProfileDialog = true },
                                onExportReport = { showSyncToast = true }
                            )
                        }

                        NavigationTab.WALLET -> {
                            WalletScreen(
                                wallet = wallet,
                                publisherId = session.publisherId,
                                onProfileClick = { showProfileDialog = true },
                                onManagePayoutMethods = { showProfileDialog = true },
                                onDownloadReceipt = { /* handled in screen */ }
                            )
                        }
                    }

                    // Floating Live Sync notification pill
                    AnimatedVisibility(
                        visible = isRefreshing,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                    ) {
                        Surface(
                            color = EspressoSlate,
                            shape = RoundedCornerShape(999.dp),
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = SolarAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Syncing live AdMob metrics...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PureWhite
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
                                repository.logout()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFDC2626)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
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
                                color = SolarAmberLight,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = SolarAmber
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = session.userName,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EspressoSlate
                                )
                                Text(
                                    text = session.email,
                                    fontSize = 12.sp,
                                    color = MutedSlate
                                )
                            }
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Publisher ID row
                            Surface(
                                color = LinenSand,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "PUBLISHER ID",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MutedSlate,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = session.publisherId,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EspressoSlate
                                    )
                                }
                            }

                            // Sync status row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(BotanicalGreen)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Auto-sync active (15s ticker)",
                                        fontSize = 12.sp,
                                        color = MutedSlate
                                    )
                                }

                                ElevatedButton(
                                    onClick = { repository.refresh() },
                                    colors = ButtonDefaults.elevatedButtonColors(
                                        containerColor = LinenSand,
                                        contentColor = EspressoSlate
                                    ),
                                    shape = RoundedCornerShape(999.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sync Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    },
                    containerColor = PureWhite,
                    shape = RoundedCornerShape(24.dp)
                )
            }

            // App Detail Inspection Dialog
            selectedAppForModal?.let { app ->
                AlertDialog(
                    onDismissRequest = { selectedAppForModal = null },
                    confirmButton = {
                        TextButton(onClick = { selectedAppForModal = null }) {
                            Text("Close", color = SolarAmber, fontWeight = FontWeight.Bold)
                        }
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (app.platform == "iOS") OceanAzureLight else BotanicalGreenLight,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (app.platform == "iOS") "🍎" else "🤖",
                                        fontSize = 16.sp
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = app.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EspressoSlate
                                )
                                Text(
                                    text = "${app.platform} • ${app.category}",
                                    fontSize = 12.sp,
                                    color = MutedSlate
                                )
                            }
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Today Revenue", fontSize = 11.sp, color = MutedSlate)
                                    Text(
                                        text = "$${app.todayRevenue}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EspressoSlate
                                    )
                                }
                                Column {
                                    Text("Avg eCPM", fontSize = 11.sp, color = MutedSlate)
                                    Text(
                                        text = "$${app.avgEcpm}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OceanAzure
                                    )
                                }
                                Column {
                                    Text("Fill Rate", fontSize = 11.sp, color = MutedSlate)
                                    Text(
                                        text = "${app.fillRate}%",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BotanicalGreen
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Ad Format Contribution",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EspressoSlate
                            )

                            FormatSegmentedBar(formats = app.formats)
                        }
                    },
                    containerColor = PureWhite,
                    shape = RoundedCornerShape(24.dp)
                )
            }
        }
    }
}
