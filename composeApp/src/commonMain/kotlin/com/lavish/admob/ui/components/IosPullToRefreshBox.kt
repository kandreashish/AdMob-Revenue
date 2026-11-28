package com.lavish.admob.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.layout.layout
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity
import com.lavish.admob.theme.EspressoSlate
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Authentic Apple iOS-style activity indicator (12 radial spokes).
 *
 * During pull gesture: spokes light up progressively clockwise based on [pullFraction].
 * During active refresh: cycles through stepped brightness phases clockwise like native iOS UIActivityIndicatorView.
 */
@Composable
fun IosActivityIndicator(
    isRefreshing: Boolean,
    pullFraction: () -> Float,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = EspressoSlate
) {
    val totalSpokes = 12

    // 12 stepped alpha levels characteristic of iOS UIActivityIndicatorView
    val steppedAlphas = remember {
        floatArrayOf(1.00f, 0.88f, 0.74f, 0.60f, 0.48f, 0.38f, 0.28f, 0.20f, 0.15f, 0.12f, 0.10f, 0.08f)
    }

    // Only run the frame clock while actually refreshing; idle spinner costs zero frames
    val activePhaseState: androidx.compose.runtime.State<Float> = if (isRefreshing) {
        rememberInfiniteTransition(label = "spinner").animateFloat(
            initialValue = 0f,
            targetValue = 12f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 960, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "spinnerPhase"
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }
    val activePhase by activePhaseState

    Canvas(modifier = modifier.size(size)) {
        val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
        val outerRadius = size.toPx() * 0.46f
        val innerRadius = size.toPx() * 0.23f
        val strokeWidth = size.toPx() * 0.095f

        val currentStep = if (isRefreshing) activePhase.toInt() % totalSpokes else 0
        val litSpokeCount = if (isRefreshing) totalSpokes else (pullFraction() * totalSpokes).toInt().coerceIn(0, totalSpokes)

        for (i in 0 until totalSpokes) {
            // Spoke angle in radians, starting at 12 o'clock (-PI / 2)
            val angleRad = (i * (360f / totalSpokes) - 90f) * (kotlin.math.PI.toFloat() / 180f)
            val cosAngle = cos(angleRad)
            val sinAngle = sin(angleRad)

            val startPoint = Offset(
                x = center.x + innerRadius * cosAngle,
                y = center.y + innerRadius * sinAngle
            )
            val endPoint = Offset(
                x = center.x + outerRadius * cosAngle,
                y = center.y + outerRadius * sinAngle
            )

            val spokeAlpha = if (isRefreshing) {
                // Stepped phase rotation
                val alphaIndex = (i - currentStep + totalSpokes) % totalSpokes
                steppedAlphas[alphaIndex]
            } else {
                // Progressive fill during user drag
                if (i < litSpokeCount) 0.85f else 0.10f
            }

            drawLine(
                color = tint.copy(alpha = spokeAlpha),
                start = startPoint,
                end = endPoint,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Container supporting iOS-style rubber-band pull-to-refresh with smooth physics and
 * native-looking 12-spoke indicator.
 */
@Composable
fun IosPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    threshold: Dp = 64.dp,
    refreshingHeight: Dp = 54.dp,
    maxPullDistance: Dp = 128.dp,
    indicatorTint: Color = EspressoSlate,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val thresholdPx = with(density) { threshold.toPx() }
    val refreshingHeightPx = with(density) { refreshingHeight.toPx() }
    val maxPullDistancePx = with(density) { maxPullDistance.toPx() }

    // Drag writes this synchronously (no launched snapTo → no stale-read jitter).
    val pullOffset = remember { Animatable(0f) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var settleJob by remember { mutableStateOf<Job?>(null) }

    val currentIsRefreshing by rememberUpdatedState(isRefreshing)
    val currentOnRefresh by rememberUpdatedState(onRefresh)

    val collapseSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    val holdSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    fun settleTo(target: Float, spec: androidx.compose.animation.core.AnimationSpec<Float>) {
        settleJob?.cancel()
        settleJob = coroutineScope.launch {
            pullOffset.snapTo(dragOffset)
            isDragging = false
            pullOffset.animateTo(target, spec) { dragOffset = value }
        }
    }

    // Refresh finished (or never started) → collapse, unless the user is mid-drag.
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing && !isDragging && dragOffset > 0f) settleTo(0f, collapseSpec)
    }

    val nestedScrollConnection = remember(thresholdPx, refreshingHeightPx, maxPullDistancePx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Dragging up while pulled down: collapse the pull offset first
                if (source == NestedScrollSource.UserInput && available.y < 0 && dragOffset > 0f) {
                    settleJob?.cancel(); isDragging = true
                    val newOffset = (dragOffset + available.y).coerceAtLeast(0f)
                    val consumedY = newOffset - dragOffset
                    dragOffset = newOffset
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Dragging down at the top of scrollable content
                if (source == NestedScrollSource.UserInput && available.y > 0) {
                    settleJob?.cancel(); isDragging = true
                    // iOS rubber-band damping curve
                    val progress = (dragOffset / maxPullDistancePx).coerceIn(0f, 0.85f)
                    val damping = (1f - progress) * 0.5f
                    dragOffset = (dragOffset + available.y * damping).coerceAtMost(maxPullDistancePx)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val wasDragging = isDragging
                isDragging = false
                if (!wasDragging || dragOffset <= 0f) return Velocity.Zero // plain content fling: don't interfere

                if (dragOffset >= thresholdPx && !currentIsRefreshing) {
                    currentOnRefresh()
                    settleTo(refreshingHeightPx, holdSpec)
                    // onRefresh may bail out (no token / already running) without ever flipping
                    // isRefreshing → LaunchedEffect never fires. Check after a short grace period.
                    coroutineScope.launch {
                        delay(300)
                        if (!currentIsRefreshing && !isDragging) settleTo(0f, collapseSpec)
                    }
                } else if (currentIsRefreshing) {
                    settleTo(if (dragOffset > 0f) refreshingHeightPx else 0f, holdSpec)
                } else {
                    settleTo(0f, collapseSpec)
                }
                // Swallow the release velocity so content doesn't fling under the indicator
                return available
            }
        }
    }

    Box(
        modifier = modifier.nestedScroll(nestedScrollConnection)
    ) {
        // Offset is only read in layout/draw phases (not composition), so dragging
        // doesn't recompose the screen content every frame.
        val indicatorVisible by remember { derivedStateOf { dragOffset > 2f } }

        // Indicator revealed above content
        if (indicatorVisible || isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .layout { measurable, constraints ->
                        val h = dragOffset.roundToInt().coerceAtLeast(0)
                        val placeable = measurable.measure(
                            constraints.copy(minHeight = h, maxHeight = h)
                        )
                        layout(placeable.width, h) { placeable.place(0, 0) }
                    },
                contentAlignment = Alignment.Center
            ) {
                IosActivityIndicator(
                    isRefreshing = isRefreshing,
                    pullFraction = { (dragOffset / thresholdPx).coerceIn(0f, 1f) },
                    size = 24.dp,
                    tint = indicatorTint
                )
            }
        }

        // Child content displaced downwards revealing indicator
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, dragOffset.roundToInt()) }
        ) {
            content()
        }
    }
}
