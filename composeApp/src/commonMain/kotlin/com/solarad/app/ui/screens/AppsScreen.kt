package com.solarad.app.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import com.solarad.app.model.AppMonetization
import com.solarad.app.model.PlatformFilter
import com.solarad.app.ui.components.AppIcon
import com.solarad.app.theme.BorderMedium
import com.solarad.app.theme.BorderSubtle
import com.solarad.app.theme.BotanicalGreen
import com.solarad.app.theme.BotanicalGreenLight
import com.solarad.app.theme.CoralCrimson
import com.solarad.app.theme.CoralCrimsonLight
import com.solarad.app.theme.EspressoSlate
import com.solarad.app.theme.LinenSand
import com.solarad.app.theme.MutedSlate
import com.solarad.app.theme.OceanAzure
import com.solarad.app.theme.OceanAzureLight
import com.solarad.app.theme.PureWhite
import com.solarad.app.theme.SolarAmber
import com.solarad.app.theme.SolarTheme
import com.solarad.app.ui.components.FormatSegmentedBar
import com.solarad.app.ui.components.SolarHeader
import com.solarad.app.ui.components.SolarPillTabs
import com.solarad.app.ui.utils.formatAmount
import com.solarad.app.ui.utils.formatCompactNumber

@Composable
fun AppsScreen(
    apps: List<AppMonetization>,
    accountName: String = "My AdMob",
    avatarUrl: String? = null,
    isOffline: Boolean = false,
    onSelectApp: (AppMonetization) -> Unit,
    onAddApp: (name: String, platform: String) -> Unit,
    onAccountClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedPlatform by remember { mutableStateOf(PlatformFilter.ALL) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newAppName by remember { mutableStateOf("") }
    var newAppPlatform by remember { mutableStateOf("Android") }

    val filteredApps = apps.filter { app ->
        val matchesPlatform = when (selectedPlatform) {
            PlatformFilter.ALL -> true
            PlatformFilter.IOS -> app.platform.equals("iOS", ignoreCase = true)
            PlatformFilter.ANDROID -> app.platform.equals("Android", ignoreCase = true)
        }
        val matchesQuery = searchQuery.isBlank() || app.name.contains(searchQuery, ignoreCase = true)
        matchesPlatform && matchesQuery
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SolarTheme.colors.background),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Header
        item {
            SolarHeader(
                title = "Your Apps",
                subtitle = if (isOffline) "${apps.size} Connected Apps (Cached)" else "${apps.size} Connected AdMob Applications",
                accountName = accountName,
                avatarUrl = avatarUrl,
                isOffline = isOffline,
                onAccountClick = onAccountClick
            )
        }

        // Search Bar
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .border(1.dp, SolarTheme.colors.borderMedium, RoundedCornerShape(999.dp)),
                color = SolarTheme.colors.cardBackground
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = SolarTheme.colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search apps by name...",
                                fontSize = 13.sp,
                                color = SolarTheme.colors.textSecondary
                            )
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = SolarTheme.colors.textPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Platform Filter Tabs
        item {
            SolarPillTabs(
                items = PlatformFilter.entries,
                selectedItem = selectedPlatform,
                labelProvider = { it.label },
                onSelect = { selectedPlatform = it }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (filteredApps.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = SolarTheme.colors.cardBackground,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SolarTheme.colors.borderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching apps found" else "No AdMob apps registered yet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SolarTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Try clearing your search query or filter" else "Link an application registered in your AdMob console below.",
                            fontSize = 12.sp,
                            color = SolarTheme.colors.textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Detailed App Cards
            items(filteredApps) { app ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                        .shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = Color.Black.copy(alpha = 0.04f)
                        )
                        .border(1.dp, SolarTheme.colors.borderSubtle, RoundedCornerShape(20.dp))
                        .clickable { onSelectApp(app) },
                    color = SolarTheme.colors.cardBackground,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // App Card Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                AppIcon(app = app, size = 36.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = app.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SolarTheme.colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${app.platform} • ${app.category}",
                                        fontSize = 11.sp,
                                        color = SolarTheme.colors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Details",
                                tint = SolarTheme.colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Revenue and Delta Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Today's Revenue",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SolarTheme.colors.textSecondary
                                )
                                Text(
                                    text = "$${formatAmount(app.todayRevenue)}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SolarTheme.colors.textPrimary,
                                    maxLines = 1
                                )
                            }

                            val isAppPositive = app.deltaVsYesterday >= 0.0
                            Surface(
                                color = if (isAppPositive) SolarTheme.colors.successContainer else SolarTheme.colors.errorContainer,
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isAppPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                        contentDescription = null,
                                        tint = if (isAppPositive) BotanicalGreen else CoralCrimson,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${if (isAppPositive) "+" else "-"}$${formatAmount(abs(app.deltaVsYesterday))} vs y'day",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAppPositive) BotanicalGreen else CoralCrimson,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Ad Unit Format Split Bar & Breakdown
                        FormatSegmentedBar(formats = app.formats)

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Metrics Footer Row - 3 equal columns for perfect compact screen fit
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = SolarTheme.colors.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text("Impressions", fontSize = 10.sp, color = SolarTheme.colors.textSecondary, maxLines = 1)
                                    Text(
                                        text = formatCompactNumber(app.totalImpressions),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SolarTheme.colors.textPrimary,
                                        maxLines = 1
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Fill Rate", fontSize = 10.sp, color = SolarTheme.colors.textSecondary, maxLines = 1)
                                    Text(
                                        text = "${app.fillRate}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BotanicalGreen,
                                        maxLines = 1
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text("Avg eCPM", fontSize = 10.sp, color = SolarTheme.colors.textSecondary, maxLines = 1)
                                    Text(
                                        text = "$${formatAmount(app.avgEcpm)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OceanAzure,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Link New App Action Button
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .border(1.dp, SolarAmber.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                    .clickable { showAddDialog = true },
                color = SolarTheme.colors.cardBackground
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = SolarAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Link New AdMob App",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SolarAmber
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "Link AdMob Application",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SolarTheme.colors.textPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Enter your application title as registered in Google AdMob:",
                        fontSize = 12.sp,
                        color = SolarTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newAppName,
                        onValueChange = { newAppName = it },
                        label = { Text("App Name", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Android", "iOS").forEach { plat ->
                            val isSelected = newAppPlatform == plat
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(999.dp))
                                    .border(
                                        1.dp,
                                        if (isSelected) SolarAmber else SolarTheme.colors.borderMedium,
                                        RoundedCornerShape(999.dp)
                                    )
                                    .clickable { newAppPlatform = plat },
                                color = if (isSelected) SolarAmber else SolarTheme.colors.surfaceVariant
                            ) {
                                Text(
                                    text = plat,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) {
                                        if (SolarTheme.colors.isDark) EspressoSlate else PureWhite
                                    } else {
                                        SolarTheme.colors.textPrimary
                                    },
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newAppName.isNotBlank()) {
                            onAddApp(newAppName, newAppPlatform)
                            newAppName = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SolarAmber),
                    enabled = newAppName.trim().isNotBlank()
                ) {
                    Text("Add App", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = SolarTheme.colors.textSecondary, fontSize = 13.sp)
                }
            },
            containerColor = SolarTheme.colors.cardBackground,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
