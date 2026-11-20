package com.solarad.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solarad.app.theme.DarkCharcoalBg
import com.solarad.app.theme.DarkCharcoalSurface
import com.solarad.app.theme.PureWhite
import com.solarad.app.theme.SolarAmber
import com.solarad.app.theme.SolarTheme

private val AdMobRedStart = Color(0xFFEA4335)
private val AdMobRedMid = Color(0xFFD93025)
private val AdMobRedDark = Color(0xFF670503)

private val GoldLight = Color(0xFFFFF3B0)
private val GoldMid = Color(0xFFF59E0B)
private val GoldDark = Color(0xFFB45309)
private val GoldText = Color(0xFF78350F)

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier
) {
    val isDark = SolarTheme.colors.isDark
    val bgColor = if (isDark) DarkCharcoalBg else Color(0xFFFAFAFA)
    val textPrimary = if (isDark) PureWhite else Color(0xFF1F1F1F)
    val textSecondary = if (isDark) Color(0xFFA8A29E) else Color(0xFF6B7280)

    val infiniteTransition = rememberInfiniteTransition(label = "splashPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = bgColor
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                // Outer Ambient Glow + App Icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(160.dp)
                ) {
                    // Soft Radial Glow
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .scale(pulseScale)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        AdMobRedMid.copy(alpha = glowAlpha),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // AdDash Icon
                    AdCashIconBadge(
                        modifier = Modifier
                            .size(112.dp)
                            .scale(pulseScale)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Brand Name
                Text(
                    text = "AdDash",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textPrimary,
                    letterSpacing = (-0.5).sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Tagline
                Text(
                    text = "Real-Time AdMob Revenue",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary,
                    textAlign = TextAlign.Center
                )
            }

            // Bottom Security Badge
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = SolarAmber,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = " Secured with Google AdMob API",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textSecondary
                )
            }
        }
    }
}

/**
 * High-fidelity Canvas rendering of the AdCash App Icon:
 * - AdMob red gradient squircle
 * - Gold coin stamped with $
 * - Intertwining folded ribbon A
 */
@Composable
fun AdCashIconBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .shadow(16.dp, shape = RoundedCornerShape(26.dp), spotColor = Color(0x662D0202))
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(AdMobRedStart, AdMobRedMid, AdMobRedDark),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Subtle inner stroke highlight
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.35f), Color.Transparent)
                ),
                cornerRadius = CornerRadius(26.dp.toPx(), 26.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )

            // 2. Golden Coin
            val coinCenter = Offset(w * 0.52f, h * 0.49f)
            val coinRadius = w * 0.26f

            // Coin Base
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(GoldLight, GoldMid, GoldDark),
                    start = Offset(coinCenter.x - coinRadius, coinCenter.y - coinRadius),
                    end = Offset(coinCenter.x + coinRadius, coinCenter.y + coinRadius)
                ),
                radius = coinRadius,
                center = coinCenter
            )

            // Coin Rim Specular
            drawCircle(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.8f), GoldMid)
                ),
                radius = coinRadius * 0.90f,
                center = coinCenter,
                style = Stroke(width = 2.dp.toPx())
            )

            // Ribbon Fold 1: White Upward Beam (left leg of 'A')
            val ribbonPath1 = Path().apply {
                moveTo(w * 0.25f, h * 0.76f)
                lineTo(w * 0.44f, h * 0.24f)
                lineTo(w * 0.54f, h * 0.24f)
                lineTo(w * 0.35f, h * 0.76f)
                close()
            }
            drawPath(
                path = ribbonPath1,
                brush = Brush.linearGradient(
                    colors = listOf(Color.White, Color(0xFFFFEBEE))
                )
            )

            // Ribbon Fold 2: Red Cross Bar (cross of 'A')
            val ribbonPath2 = Path().apply {
                moveTo(w * 0.46f, h * 0.48f)
                lineTo(w * 0.76f, h * 0.76f)
                lineTo(w * 0.65f, h * 0.76f)
                lineTo(w * 0.38f, h * 0.48f)
                close()
            }
            drawPath(
                path = ribbonPath2,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFF5252), Color(0xFFD32F2F))
                )
            )

            // Apex accent circle
            drawCircle(
                color = Color.White,
                radius = w * 0.035f,
                center = Offset(w * 0.44f, h * 0.25f)
            )
        }

        // Center dollar text on coin
        Text(
            text = "$",
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            color = GoldText,
            modifier = Modifier.padding(start = 6.dp, top = 2.dp)
        )
    }
}
