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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Widgets
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solarad.app.model.AppMonetization
import com.solarad.app.model.PlatformFilter
import com.solarad.app.theme.BorderMedium
import com.solarad.app.theme.BorderSubtle
import com.solarad.app.theme.BotanicalGreen
import com.solarad.app.theme.BotanicalGreenLight
import com.solarad.app.theme.EspressoSlate
import com.solarad.app.theme.LinenSand
import com.solarad.app.theme.MutedSlate
import com.solarad.app.theme.OceanAzure
import com.solarad.app.theme.PureWhite
import com.solarad.app.theme.SolarAmber
import com.solarad.app.ui.components.FormatSegmentedBar
import com.solarad.app.ui.components.SolarHeader
import com.solarad.app.ui.components.SolarPillTabs

@Composable
fun AppsScreen(
    apps: List<AppMonetization>,
    onSelectApp: (AppMonetization) -> Unit,
    onAddApp: (name: String, platform: String) -> Unit,
    onAccountClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedPlatform by remember { mutableStateOf(PlatformFilter.ALL) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newAppName by remember { mutableStateOf("") }
    var newAppPlatform by remember { mutableStateOf("iOS") }

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
            .background(LinenSand),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Header
        item {
            SolarHeader(
                title = "Your Apps",
                subtitle = "${apps.size} Active Apps Monetizing",
                accountName = "Alex's AdMob",
                onAccountClick = onAccountClick
            )
        }

        // Search Bar
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .border(1.dp, BorderMedium, RoundedCornerShape(999.dp)),
                color = PureWhite
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MutedSlate,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search apps by name...",
                                fontSize = 14.sp,
                                color = MutedSlate
                            )
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = EspressoSlate
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Platform Filter Tabs
        item {
            SolarPillTabs(
                items = PlatformFilter.values().toList(),
                selectedItem = selectedPlatform,
                labelProvider = { it.label },
                onSelect = { selectedPlatform = it }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Detailed App Cards
        items(filteredApps) { app ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color.Black.copy(alpha = 0.04f)
                    )
                    .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
                    .clickable { onSelectApp(app) },
                color = PureWhite,
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // App Card Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when (app.name) {
                                            "CyberDrift" -> Color(0xFF0284C7)
                                            "FocusAI" -> Color(0xFFF59E0B)
                                            else -> Color(0xFF8B5CF6)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (app.name) {
                                        "CyberDrift" -> Icons.Default.SportsEsports
                                        "FocusAI" -> Icons.Default.TrendingUp
                                        else -> Icons.Default.Widgets
                                    },
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
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

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Details",
                            tint = MutedSlate,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Revenue and Delta Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Today's Revenue",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MutedSlate
                            )
                            Text(
                                text = "$${app.todayRevenue}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EspressoSlate
                            )
                        }

                        Surface(
                            color = BotanicalGreenLight,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "+$${app.deltaVsYesterday.toInt()} vs y'day",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BotanicalGreen
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Ad Unit Format Split Bar & Breakdown
                    FormatSegmentedBar(formats = app.formats)

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Metrics Footer Row
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = LinenSand,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Impressions: ",
                                    fontSize = 11.sp,
                                    color = MutedSlate
                                )
                                Text(
                                    text = "${app.totalImpressions / 1000}k",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EspressoSlate
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Fill Rate: ",
                                    fontSize = 11.sp,
                                    color = MutedSlate
                                )
                                Text(
                                    text = "${app.fillRate}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BotanicalGreen
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Avg eCPM: ",
                                    fontSize = 11.sp,
                                    color = MutedSlate
                                )
                                Text(
                                    text = "$${app.avgEcpm}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OceanAzure
                                )
                            }
                        }
                    }
                }
            }
        }

        // Link New App Action Button
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .border(1.dp, SolarAmber.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                    .clickable { showAddDialog = true },
                color = PureWhite
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = SolarAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Link New AdMob App",
                        fontSize = 14.sp,
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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = EspressoSlate
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your mobile application name registered in your AdMob console:",
                        fontSize = 13.sp,
                        color = MutedSlate
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newAppName,
                        onValueChange = { newAppName = it },
                        label = { Text("App Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("iOS", "Android").forEach { plat ->
                            val isSelected = newAppPlatform == plat
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(999.dp))
                                    .border(
                                        1.dp,
                                        if (isSelected) SolarAmber else BorderMedium,
                                        RoundedCornerShape(999.dp)
                                    )
                                    .clickable { newAppPlatform = plat },
                                color = if (isSelected) SolarAmber else PureWhite
                            ) {
                                Text(
                                    text = plat,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) PureWhite else EspressoSlate,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
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
                    colors = ButtonDefaults.buttonColors(containerColor = SolarAmber)
                ) {
                    Text("Add App", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = MutedSlate)
                }
            },
            containerColor = PureWhite,
            shape = RoundedCornerShape(24.dp)
        )
    }
}
