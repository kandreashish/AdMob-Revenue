package com.lavish.admob.ui.screens

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lavish.admob.theme.DarkCharcoalBg
import com.lavish.admob.theme.DarkCharcoalSurface
import com.lavish.admob.theme.PureWhite
import com.lavish.admob.theme.SolarAmber
import com.lavish.admob.theme.SolarTheme

private val AdMobRedStart = Color(0xFFEA4335)
private val AdMobRedMid = Color(0xFFD93025)
private val AdMobRedDark = Color(0xFF670503)

private val GoldLight = Color(0xFFFEF7E0)
private val GoldMid = Color(0xFF1A73E8)
private val GoldDark = Color(0xFF185ABC)
private val GoldText = Color(0xFF174EA6)

private val SplashGlowBrush = Brush.radialGradient(
    colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent)
)

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splashPulse")
    val pulseScale = infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val glowAlpha = infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AdMobRedStart,
                        AdMobRedMid,
                        AdMobRedDark
                    )
                )
            ),
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
                        // Animated values read only in the draw phase: no per-frame recomposition,
                        // constant brush instead of a new gradient every frame
                        .graphicsLayer {
                            scaleX = pulseScale.value
                            scaleY = pulseScale.value
                            alpha = glowAlpha.value
                        }
                        .background(SplashGlowBrush)
                )

                // AdDash Icon
                AdCashIconBadge(
                    modifier = Modifier
                        .size(112.dp)
                        .graphicsLayer {
                            scaleX = pulseScale.value
                            scaleY = pulseScale.value
                        }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Brand Name
            Text(
                text = "AdDash",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PureWhite,
                letterSpacing = (-0.5).sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Tagline
            Text(
                text = "Real-Time AdMob Revenue",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFFAD2CF),
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
                tint = GoldLight,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = " Secured with Google AdMob API",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFFAD2CF)
            )
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
                    colors = listOf(Color.White, Color(0xFFFCE8E6))
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
                    colors = listOf(Color(0xFFFF5252), Color(0xFFD93025))
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
