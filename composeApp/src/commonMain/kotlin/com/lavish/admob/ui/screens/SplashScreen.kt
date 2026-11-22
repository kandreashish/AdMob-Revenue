package com.lavish.admob.ui.screens

import com.lavish.admob.resources.app_icon_foreground
import com.lavish.admob.resources.Res
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
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
 * The real launcher icon: the same foreground artwork as the Android adaptive icon
 * (gold coin + rising arrow + dash lines) on the AdMob-red squircle, so the splash matches the home-screen icon.
 */
@Composable
fun AdCashIconBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .shadow(16.dp, shape = RoundedCornerShape(26.dp), spotColor = Color(0x662D0202))
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    // Same AdMob red gradient as the launcher icon background
                    colors = listOf(Color(0xFFEA4335), Color(0xFFB3261E))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Adaptive-icon foregrounds are 108dp with a 72dp visible area: scale up so it's framed like the launcher.
        Image(
            painter = painterResource(Res.drawable.app_icon_foreground),
            contentDescription = "AdDash",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.5f; scaleY = 1.5f }
        )
    }
}
