package com.lavish.admob.ui.components

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
import com.lavish.admob.model.TimeRange
import com.lavish.admob.theme.BotanicalGreen
import com.lavish.admob.theme.DarkAmberContainer
import com.lavish.admob.theme.DarkOnAmberContainer
import com.lavish.admob.theme.EspressoSlate
import com.lavish.admob.theme.PureWhite
import com.lavish.admob.theme.SolarAmber
import com.lavish.admob.theme.SolarTheme

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
            CompactHorizonItem(TimeRange.THIS_MONTH, "Month"),
            CompactHorizonItem(TimeRange.DAYS_30, "30D"),
            CompactHorizonItem(TimeRange.CUSTOM, "Custom", showEditIcon = true)
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Google AdMob Pill Chips directly on canvas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items.forEach { item ->
                val isSelected = item.range == selectedRange
                val chipBg = if (isSelected) SolarTheme.colors.primaryContainer else SolarTheme.colors.cardBackground
                val chipBorder = if (isSelected) {
                    if (SolarTheme.colors.isDark) SolarTheme.colors.primary.copy(alpha = 0.5f)
                    else com.lavish.admob.theme.AdMobSelectedBorder
                } else {
                    SolarTheme.colors.borderMedium
                }
                val contentColor = if (isSelected) {
                    if (SolarTheme.colors.isDark) SolarTheme.colors.onPrimaryContainer else com.lavish.admob.theme.AdMobSelectedText
                } else {
                    SolarTheme.colors.textSecondary
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(999.dp))
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
                    shape = RoundedCornerShape(999.dp),
                    color = chipBg,
                    border = BorderStroke(1.dp, chipBorder)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            if (item.showEditIcon && isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = contentColor,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                            }

                            Text(
                                text = item.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
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
                            TimeRange.THIS_MONTH -> "Month to date • vs same days last month"
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
