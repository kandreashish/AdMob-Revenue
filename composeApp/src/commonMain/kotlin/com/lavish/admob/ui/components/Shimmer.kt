package com.lavish.admob.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lavish.admob.theme.SolarTheme

/**
 * Lightweight skeleton/shimmer placeholders for first load and slow networks.
 *
 * One infinite transition drives every block on screen (via [LocalShimmerProgress]), and the
 * animated value is only read in the draw phase, so the shimmer never triggers recomposition.
 */
private val LocalShimmerProgress = compositionLocalOf<State<Float>?> { null }

/** Hosts a shared shimmer animation for all [ShimmerBlock]s inside [content]. */
@Composable
fun ShimmerHost(content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerProgress"
    )
    CompositionLocalProvider(LocalShimmerProgress provides progress) { content() }
}

/** A single grey placeholder block with a moving highlight. */
@Composable
fun ShimmerBlock(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    val base = SolarTheme.colors.surfaceVariant
    val highlight = if (SolarTheme.colors.isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.55f)
    val progressState = LocalShimmerProgress.current
    Box(
        modifier
            .clip(shape)
            .background(base)
            .drawWithContent {
                drawContent()
                val p = progressState?.value ?: return@drawWithContent
                val band = size.width.coerceAtLeast(200f)
                val x = -band + (size.width + 2 * band) * p
                drawRect(
                    Brush.linearGradient(
                        colors = listOf(Color.Transparent, highlight, Color.Transparent),
                        start = Offset(x, 0f),
                        end = Offset(x + band, size.height)
                    )
                )
            }
    )
}

@Composable
private fun SkeletonCard(height: Dp, modifier: Modifier = Modifier) =
    ShimmerBlock(modifier.fillMaxWidth().height(height), RoundedCornerShape(16.dp))

/** Generic tab skeleton: header, hero card, metric tiles and a list. */
@Composable
fun ShimmerScreenSkeleton(modifier: Modifier = Modifier) = ShimmerHost {
    Column(
        modifier.fillMaxSize().background(SolarTheme.colors.background).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row {
            Column(Modifier.weight(1f)) {
                ShimmerBlock(Modifier.width(140.dp).height(22.dp))
                Spacer(Modifier.height(8.dp))
                ShimmerBlock(Modifier.width(90.dp).height(12.dp))
            }
            ShimmerBlock(Modifier.size(40.dp), CircleShape)
        }
        SkeletonCard(170.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonCard(92.dp, Modifier.weight(1f))
            SkeletonCard(92.dp, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonCard(92.dp, Modifier.weight(1f))
            SkeletonCard(92.dp, Modifier.weight(1f))
        }
        repeat(4) { ShimmerListRow() }
    }
}

/** One list/table row placeholder: icon + two text lines + trailing value. */
@Composable
fun ShimmerListRow(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        ShimmerBlock(Modifier.size(36.dp), RoundedCornerShape(10.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            ShimmerBlock(Modifier.fillMaxWidth(0.6f).height(13.dp))
            Spacer(Modifier.height(6.dp))
            ShimmerBlock(Modifier.fillMaxWidth(0.35f).height(10.dp))
        }
        Spacer(Modifier.width(12.dp))
        ShimmerBlock(Modifier.width(56.dp).height(14.dp))
    }
}

/** Table placeholder (used by Reports while a report is being built). */
@Composable
fun ShimmerTable(rows: Int = 6, modifier: Modifier = Modifier) = ShimmerHost {
    Column(modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ShimmerBlock(Modifier.fillMaxWidth().height(14.dp))
        repeat(rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ShimmerBlock(Modifier.weight(1.6f).height(12.dp))
                ShimmerBlock(Modifier.weight(1f).height(12.dp))
                ShimmerBlock(Modifier.weight(1f).height(12.dp))
            }
        }
    }
}
