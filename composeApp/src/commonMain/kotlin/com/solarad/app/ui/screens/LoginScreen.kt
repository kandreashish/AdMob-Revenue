package com.solarad.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solarad.app.theme.BorderMedium
import com.solarad.app.theme.BorderSubtle
import com.solarad.app.theme.BotanicalGreen
import com.solarad.app.theme.EspressoSlate
import com.solarad.app.theme.LightSlate
import com.solarad.app.theme.LinenSand
import com.solarad.app.theme.MutedSlate
import com.solarad.app.theme.OceanAzure
import com.solarad.app.theme.OceanAzureLight
import com.solarad.app.theme.PureWhite
import com.solarad.app.theme.SolarAmber
import com.solarad.app.theme.SolarAmberLight

@Composable
fun LoginScreen(
    onContinueWithGoogle: () -> Unit,
    onContinueWithToken: (String) -> Unit
) {
    var showTokenDialog by remember { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LinenSand)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Ambient Radiant Sun Motif
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = CircleShape,
                        ambientColor = SolarAmber.copy(alpha = 0.4f),
                        spotColor = SolarAmber.copy(alpha = 0.3f)
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD54F),
                                SolarAmber,
                                Color(0xFFD97706)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = "SolarAd Brand",
                    tint = PureWhite,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Brand Title
            Text(
                text = "SolarAd",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = EspressoSlate,
                letterSpacing = (-0.02).sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Value Proposition Headline
            Text(
                text = "Your Daily Revenue,\nEffortlessly Clear.",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = EspressoSlate,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Track your AdMob earnings, eCPM velocity, and app performance in peaceful real-time daylight.",
                fontSize = 14.sp,
                color = MutedSlate,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Trust & Privacy Pill
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .border(1.dp, OceanAzure.copy(alpha = 0.25f), RoundedCornerShape(999.dp)),
                color = OceanAzureLight
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = OceanAzure,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Read-only connection • Credentials never stored",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OceanAzure
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Primary Google Sign In CTA
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(999.dp),
                        ambientColor = Color.Black.copy(alpha = 0.06f)
                    )
                    .clip(RoundedCornerShape(999.dp))
                    .border(1.dp, BorderMedium, RoundedCornerShape(999.dp))
                    .clickable { onContinueWithGoogle() },
                color = PureWhite
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Google 'G' stylized badge
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4285F4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "G",
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Continue with Google",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = EspressoSlate
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Secondary Option: API Token
            Text(
                text = "Or connect via AdMob API Token",
                fontSize = 13.sp,
                color = MutedSlate,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable { showTokenDialog = true }
                    .padding(4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 3 Feature Highlights
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeaturePointCard(
                    icon = Icons.Default.Schedule,
                    title = "Instant Daily Earnings",
                    description = "Live intraday tracking, pacing deltas vs yesterday, and daily goal progress."
                )
                FeaturePointCard(
                    icon = Icons.Default.Insights,
                    title = "Unit-Level eCPM Insights",
                    description = "Automatic format breakdown across Rewarded, Interstitial, Native, and Banner."
                )
                FeaturePointCard(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "Automatic Payout Countdown",
                    description = "Track your $100 threshold progress, upcoming wire date, and payment health."
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Footer
            Text(
                text = "Official Google AdMob Reporting API v1 Verified",
                fontSize = 11.sp,
                color = LightSlate,
                fontWeight = FontWeight.Medium
            )
        }
    }

    if (showTokenDialog) {
        AlertDialog(
            onDismissRequest = { showTokenDialog = false },
            title = {
                Text(
                    text = "Connect via API Token",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = EspressoSlate
                )
            },
            text = {
                Column {
                    Text(
                        text = "Paste your Google Cloud Service Account OAuth token or read-only reporting token:",
                        fontSize = 13.sp,
                        color = MutedSlate
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        placeholder = { Text("ya29.a0AfH6SM...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTokenDialog = false
                        onContinueWithToken(tokenInput.ifBlank { "demo-token" })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SolarAmber)
                ) {
                    Text("Connect", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTokenDialog = false }) {
                    Text("Cancel", color = MutedSlate)
                }
            },
            containerColor = PureWhite,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun FeaturePointCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp)),
        color = PureWhite,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SolarAmberLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SolarAmber,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = EspressoSlate
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MutedSlate,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
