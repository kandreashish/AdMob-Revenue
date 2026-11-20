package com.solarad.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.solarad.app.model.ThemeMode
import com.solarad.app.theme.BorderMedium
import com.solarad.app.theme.BorderSubtle
import com.solarad.app.theme.EspressoSlate
import com.solarad.app.theme.LightSlate
import com.solarad.app.theme.LinenSand
import com.solarad.app.theme.MutedSlate
import com.solarad.app.theme.OceanAzure
import com.solarad.app.theme.OceanAzureLight
import com.solarad.app.theme.PureWhite
import com.solarad.app.theme.SolarAmber
import com.solarad.app.theme.SolarAmberLight
import com.solarad.app.theme.SolarTheme

@Composable
fun LoginScreen(
    authError: String? = null,
    isAuthenticating: Boolean = false,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onSelectTheme: ((ThemeMode) -> Unit)? = null,
    onClearError: () -> Unit = {},
    onContinueWithGoogle: () -> Unit,
    onContinueWithToken: (String) -> Unit
) {
    var showTokenDialog by remember { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf("") }
    var tokenDialogError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SolarTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Theme Toggle Quick Action
            if (onSelectTheme != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    val nextMode = when (themeMode) {
                        ThemeMode.SYSTEM -> ThemeMode.LIGHT
                        ThemeMode.LIGHT -> ThemeMode.DARK
                        ThemeMode.DARK -> ThemeMode.SYSTEM
                    }
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .border(1.dp, SolarTheme.colors.borderSubtle, RoundedCornerShape(999.dp))
                            .clickable { onSelectTheme(nextMode) },
                        color = SolarTheme.colors.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (themeMode) {
                                    ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                    ThemeMode.LIGHT -> Icons.Default.LightMode
                                    ThemeMode.DARK -> Icons.Default.DarkMode
                                },
                                contentDescription = "Toggle Theme",
                                tint = SolarAmber,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = themeMode.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SolarTheme.colors.textPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Ambient Radiant Sun Motif
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .shadow(
                        elevation = 16.dp,
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
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = "AdDash Brand",
                    tint = PureWhite,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Brand Title
            Text(
                text = "AdDash",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SolarTheme.colors.textPrimary,
                letterSpacing = (-0.02).sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Value Proposition Headline
            Text(
                text = "Your Daily Revenue,\nEffortlessly Clear.",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = SolarTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Live Google AdMob earnings, eCPM velocity, and real performance in peaceful daylight.",
                fontSize = 13.sp,
                color = SolarTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Trust & Privacy Pill
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .border(1.dp, OceanAzure.copy(alpha = 0.25f), RoundedCornerShape(999.dp)),
                color = SolarTheme.colors.secondaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = OceanAzure,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Read-only connection • Credentials never stored",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OceanAzure
                    )
                }
            }

            // Auth Error Banner
            AnimatedVisibility(
                visible = authError != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                authError?.let { msg ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(12.dp)),
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                color = Color(0xFF991B1B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = onClearError,
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = Color(0xFF991B1B),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Primary Google Sign In CTA
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(999.dp),
                        ambientColor = Color.Black.copy(alpha = 0.06f)
                    )
                    .clip(RoundedCornerShape(999.dp))
                    .border(1.dp, SolarTheme.colors.borderMedium, RoundedCornerShape(999.dp))
                    .clickable(enabled = !isAuthenticating) {
                        onClearError()
                        onContinueWithGoogle()
                    },
                color = SolarTheme.colors.cardBackground
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isAuthenticating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = SolarAmber,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Connecting to AdMob...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SolarTheme.colors.textPrimary
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4285F4)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "G",
                                color = PureWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Continue with Google",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SolarTheme.colors.textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Option: API Token
            Text(
                text = "Or connect via AdMob API Token",
                fontSize = 12.sp,
                color = SolarTheme.colors.textSecondary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable {
                        onClearError()
                        tokenDialogError = null
                        showTokenDialog = true
                    }
                    .padding(4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 3 Feature Highlights
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeaturePointCard(
                    icon = Icons.Default.Schedule,
                    title = "Real-Time AdMob Metrics",
                    description = "Actual intraday revenue, yesterday pacing deltas, and live eCPM yield."
                )
                FeaturePointCard(
                    icon = Icons.Default.Insights,
                    title = "Ad Format Contribution",
                    description = "Accurate format breakdown across Rewarded, Interstitial, Native, and Banner units."
                )
                FeaturePointCard(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "Payout Balance & Cycle",
                    description = "Track your minimum $100 AdSense payout threshold and wire disbursement schedule."
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Footer
            Text(
                text = "Official Google AdMob Reporting API v1 Verified",
                fontSize = 10.sp,
                color = SolarTheme.colors.textMuted,
                fontWeight = FontWeight.Medium
            )
        }
    }

    if (showTokenDialog) {
        AlertDialog(
            onDismissRequest = {
                showTokenDialog = false
                tokenDialogError = null
            },
            title = {
                Text(
                    text = "Connect via API Token",
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
                        text = "Paste your Google Cloud OAuth access token or read-only reporting token:",
                        fontSize = 12.sp,
                        color = SolarTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = {
                            tokenInput = it
                            if (tokenDialogError != null) tokenDialogError = null
                        },
                        placeholder = { Text("ya29.a0AfH6SM...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = tokenDialogError != null
                    )
                    if (tokenDialogError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tokenDialogError.orEmpty(),
                            color = Color(0xFFDC2626),
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanToken = tokenInput.trim()
                        if (cleanToken.isBlank()) {
                            tokenDialogError = "Token cannot be empty."
                        } else {
                            showTokenDialog = false
                            tokenDialogError = null
                            onContinueWithToken(cleanToken)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SolarAmber),
                    enabled = tokenInput.trim().isNotBlank()
                ) {
                    Text("Connect", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTokenDialog = false
                    tokenDialogError = null
                }) {
                    Text("Cancel", color = SolarTheme.colors.textSecondary, fontSize = 13.sp)
                }
            },
            containerColor = SolarTheme.colors.cardBackground,
            shape = RoundedCornerShape(20.dp)
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
            .border(1.dp, SolarTheme.colors.borderSubtle, RoundedCornerShape(16.dp)),
        color = SolarTheme.colors.cardBackground,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SolarTheme.colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SolarAmber,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SolarTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = SolarTheme.colors.textSecondary,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
