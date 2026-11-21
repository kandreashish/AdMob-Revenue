package com.solarad.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
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
    themeMode: ThemeMode = ThemeMode.LIGHT,
    onSelectTheme: ((ThemeMode) -> Unit)? = null,
    onClearError: () -> Unit = {},
    onContinueWithGoogle: () -> Unit,
    onContinueWithToken: (String) -> Unit
) {
    var showTokenDialog by remember { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf("") }
    var tokenDialogError by remember { mutableStateOf<String?>(null) }

    val openTokenDialog = {
        onClearError()
        tokenDialogError = null
        showTokenDialog = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SolarTheme.colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar: wordmark + theme toggle
            Row(
                modifier = Modifier.fillMaxWidth().widthIn(max = 440.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BrandMark(size = 28.dp)
                Spacer(Modifier.width(8.dp))
                Text("AdDash", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = SolarTheme.colors.textPrimary)
                Spacer(Modifier.weight(1f))
                if (onSelectTheme != null) {
                    val nextMode = when (themeMode) {
                        ThemeMode.SYSTEM -> ThemeMode.LIGHT
                        ThemeMode.LIGHT -> ThemeMode.DARK
                        ThemeMode.DARK -> ThemeMode.SYSTEM
                    }
                    IconButton(onClick = { onSelectTheme(nextMode) }) {
                        Icon(
                            imageVector = when (themeMode) {
                                ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                ThemeMode.DARK -> Icons.Default.DarkMode
                            },
                            contentDescription = "Theme: ${themeMode.label}",
                            tint = SolarTheme.colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f).heightIn(min = 24.dp))

            // Sign-in card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .border(1.dp, SolarTheme.colors.borderMedium, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                color = SolarTheme.colors.cardBackground
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BrandMark(size = 56.dp)
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "Sign in",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Normal,
                        color = SolarTheme.colors.textPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Use the Google account linked to your AdMob publisher account",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = SolarTheme.colors.textSecondary,
                        textAlign = TextAlign.Center
                    )

                    AnimatedVisibility(visible = authError != null, enter = fadeIn(), exit = fadeOut()) {
                        authError?.let { msg ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                                color = SolarTheme.colors.errorContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = SolarTheme.colors.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = msg,
                                        color = SolarTheme.colors.onErrorContainer,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = onClearError, modifier = Modifier.size(24.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss error",
                                            tint = SolarTheme.colors.onErrorContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    // Primary: Sign in with Google (Google branding guidelines: white/neutral button + G mark)
                    Button(
                        onClick = {
                            onClearError()
                            onContinueWithGoogle()
                        },
                        enabled = !isAuthenticating,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SolarAmber,
                            contentColor = PureWhite,
                            disabledContainerColor = SolarAmber.copy(alpha = 0.6f),
                            disabledContentColor = PureWhite
                        ),
                        contentPadding = PaddingValues(start = 4.dp, end = 16.dp)
                    ) {
                        if (isAuthenticating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = PureWhite,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Connecting to AdMob…", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        } else {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(PureWhite),
                                contentAlignment = Alignment.Center
                            ) {
                                GoogleGLogo(Modifier.size(20.dp))
                            }
                            Text(
                                "Sign in with Google",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.width(40.dp))
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(Modifier.weight(1f), color = SolarTheme.colors.borderMedium)
                        Text(
                            "or",
                            fontSize = 12.sp,
                            color = SolarTheme.colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(Modifier.weight(1f), color = SolarTheme.colors.borderMedium)
                    }

                    Spacer(Modifier.height(20.dp))

                    OutlinedButton(
                        onClick = openTokenDialog,
                        enabled = !isAuthenticating,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, SolarTheme.colors.borderMedium)
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = SolarAmber, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Use an access token", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = SolarAmber)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // What you get — quiet, compact list
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 440.dp).padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FeatureRow(Icons.Default.Schedule, "Live earnings", "Today's revenue, eCPM and impressions, refreshed automatically.")
                FeatureRow(Icons.Default.Insights, "AdMob-style reports", "Break down by app, ad format, country and date. Export to CSV.")
                FeatureRow(Icons.Default.Lock, "Read-only access", "AdDash only requests AdMob reporting permission. It can't change your account.")
            }

            Spacer(Modifier.weight(1f).heightIn(min = 24.dp))

            Text(
                text = "AdDash is an independent app and is not affiliated with Google.\nData is provided by the AdMob API.",
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = SolarTheme.colors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 440.dp)
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
                        text = "Paste an OAuth 2.0 access token with the AdMob reporting scope (admob.readonly). Tokens usually expire after 1 hour.",
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
                            color = Color(0xFFD93025),
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
                    Text("Connect", color = PureWhite, fontWeight = FontWeight.Medium, fontSize = 14.sp)
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
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(SolarTheme.colors.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = SolarAmber, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = SolarTheme.colors.textPrimary)
            Spacer(Modifier.height(2.dp))
            Text(description, fontSize = 13.sp, lineHeight = 18.sp, color = SolarTheme.colors.textSecondary)
        }
    }
}

/** App mark: rounded square in AdMob blue with a rising-trend glyph. */
@Composable
private fun BrandMark(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(SolarAmber),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
            contentDescription = null,
            tint = PureWhite,
            modifier = Modifier.size(size * 0.6f)
        )
    }
}

/** Multicolor Google "G" for the Sign in with Google button. */
@Composable
private fun GoogleGLogo(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.2f
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val tl = Offset(inset, inset)
        fun arc(color: Color, start: Float, sweep: Float) =
            drawArc(color, start, sweep, useCenter = false, topLeft = tl, size = arcSize, style = Stroke(stroke))
        arc(Color(0xFFEA4335), -150f, 105f)  // red (top)
        arc(Color(0xFFFBBC05), 150f, 60f)    // yellow (left-bottom)
        arc(Color(0xFF34A853), 45f, 105f)    // green (bottom)
        arc(Color(0xFF4285F4), -20f, 65f)    // blue (right)
        // crossbar
        drawRect(
            Color(0xFF4285F4),
            topLeft = Offset(size.width / 2f, size.height / 2f - stroke / 2f),
            size = Size(size.width / 2f - inset + stroke / 2f, stroke)
        )
    }
}
