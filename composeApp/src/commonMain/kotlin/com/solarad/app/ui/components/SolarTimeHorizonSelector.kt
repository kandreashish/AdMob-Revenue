package com.solarad.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solarad.app.model.TimeRange
import com.solarad.app.theme.BotanicalGreen
import com.solarad.app.theme.DarkAmberContainer
import com.solarad.app.theme.DarkOnAmberContainer
import com.solarad.app.theme.EspressoSlate
import com.solarad.app.theme.PureWhite
import com.solarad.app.theme.SolarAmber
import com.solarad.app.theme.SolarTheme

private data class CompactHorizonItem(
    val range: TimeRange,
    val label: String,
    val isLive: Boolean = false,
    val showEditIcon: Boolean = false
)

@Composable
fun SolarTimeHorizonSelector(
    selectedRange: TimeRange,
    onSelectRange: (TimeRange) -> Unit,
    onCustomClick: () -> Unit,
    modifier: Modifier = Modifier,
    customDateLabel: String? = null
) {
    val items = remember {
        listOf(
            CompactHorizonItem(TimeRange.TODAY, "Today", isLive = true),
            CompactHorizonItem(TimeRange.YESTERDAY, "Y'day"),
            CompactHorizonItem(TimeRange.LAST_7_DAYS, "7D"),
            CompactHorizonItem(TimeRange.DAYS_30, "30D"),
            CompactHorizonItem(TimeRange.CUSTOM, "Custom", showEditIcon = true)
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Unified Zero-Scroll Segmented Horizon Dock
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(14.dp),
            color = SolarTheme.colors.surfaceVariant,
            border = BorderStroke(1.dp, SolarTheme.colors.borderSubtle),
            shadowElevation = if (SolarTheme.colors.isDark) 0.dp else 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items.forEach { item ->
                    val isSelected = item.range == selectedRange
                    val elevation by animateDpAsState(
                        targetValue = if (isSelected) 2.dp else 0.dp,
                        animationSpec = spring(stiffness = 500f)
                    )
                    val activeBgGradient = Brush.horizontalGradient(
                        colors = listOf(SolarAmber, Color(0xFFD97706))
                    )
                    val activeContentColor = if (SolarTheme.colors.isDark) EspressoSlate else PureWhite

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .shadow(
                                elevation = elevation,
                                shape = RoundedCornerShape(11.dp),
                                ambientColor = if (isSelected) SolarAmber.copy(alpha = if (SolarTheme.colors.isDark) 0.25f else 0.35f) else Color.Transparent,
                                spotColor = if (isSelected) (if (SolarTheme.colors.isDark) SolarAmber.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.15f)) else Color.Transparent
                            )
                            .clip(RoundedCornerShape(11.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (item.range == TimeRange.CUSTOM) {
                                    onCustomClick()
                                } else {
                                    onSelectRange(item.range)
                                }
                            },
                        shape = RoundedCornerShape(11.dp),
                        color = Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (isSelected) Modifier.background(activeBgGradient)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                if (item.isLive) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) activeContentColor else BotanicalGreen)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }

                                if (item.showEditIcon && isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = activeContentColor,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                }

                                Text(
                                    text = item.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) activeContentColor else SolarTheme.colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Dynamic Scope Peek Sub-Banner (Zero clutter, informs exact coverage)
        AnimatedContent(
            targetState = selectedRange,
            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(160)) }
        ) { range ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (range == TimeRange.TODAY) BotanicalGreen else SolarAmber)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (range) {
                            TimeRange.TODAY -> "Live updates • Real-time AdMob sync"
                            TimeRange.YESTERDAY -> "Finalized 24h ledger • Yesterday"
                            TimeRange.LAST_7_DAYS -> "Rolling 7-day trend"
                            TimeRange.DAYS_30 -> "Rolling 30-day view"
                            TimeRange.CUSTOM -> if (!customDateLabel.isNullOrBlank()) "Custom: $customDateLabel" else "Custom date filter"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = SolarTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (range == TimeRange.CUSTOM) {
                    Surface(
                        modifier = Modifier.clickable { onCustomClick() },
                        color = if (SolarTheme.colors.isDark) DarkAmberContainer else SolarAmber.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = if (SolarTheme.colors.isDark) DarkOnAmberContainer else SolarAmber,
                                modifier = Modifier.size(9.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Edit",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (SolarTheme.colors.isDark) DarkOnAmberContainer else SolarAmber
                            )
                        }
                    }
                }
            }
        }
    }
}
