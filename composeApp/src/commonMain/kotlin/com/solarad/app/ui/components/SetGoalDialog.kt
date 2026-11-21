package com.solarad.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solarad.app.theme.BotanicalGreen
import com.solarad.app.theme.CoralCrimson
import com.solarad.app.theme.PureWhite
import com.solarad.app.theme.SolarAmber
import com.solarad.app.theme.SolarTheme

@Composable
fun SetGoalDialog(
    currentGoal: Double,
    currencySymbol: String = "$",
    isCustomGoal: Boolean = false,
    selectedRange: com.solarad.app.model.TimeRange? = null,
    periodDays: Int = 1,
    onDismiss: () -> Unit,
    onSaveGoal: (Double) -> Unit,
    onResetToAuto: (() -> Unit)? = null
) {
    val initialText = if (currentGoal > 0) {
        if (currentGoal % 1.0 == 0.0) currentGoal.toInt().toString() else currentGoal.toString()
    } else ""

    var goalInput by remember { mutableStateOf(initialText) }
    var inputError by remember { mutableStateOf<String?>(null) }

    val presetGoals = listOf(25, 50, 100, 250, 500, 1000)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SolarTheme.colors.cardBackground,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SolarTheme.colors.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = SolarAmber,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Daily Revenue Goal",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SolarTheme.colors.textPrimary
                    )
                    Text(
                        text = if (isCustomGoal) "Custom goal active" else "Smart auto-scaled from 7d avg",
                        fontSize = 11.sp,
                        color = if (isCustomGoal) BotanicalGreen else SolarTheme.colors.textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Set a daily earnings target to track your progress. Weekly (7d) and monthly (30d) targets scale automatically from this amount.",
                    fontSize = 12.sp,
                    color = SolarTheme.colors.textSecondary,
                    lineHeight = 17.sp
                )

                Text(
                    text = "Quick Presets:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SolarTheme.colors.textSecondary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetGoals.forEach { preset ->
                        val isSelected = goalInput == preset.toString()
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (isSelected) SolarAmber else SolarTheme.colors.surfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) SolarAmber else SolarTheme.colors.borderSubtle
                            ),
                            modifier = Modifier.clickable {
                                goalInput = preset.toString()
                                inputError = null
                            }
                        ) {
                            Text(
                                text = "$currencySymbol$preset",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) PureWhite else SolarTheme.colors.textPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                OutlinedTextField(
                    value = goalInput,
                    onValueChange = { newValue ->
                        // Only allow numeric and single decimal point
                        if (newValue.isEmpty() || newValue.matches(Regex("""^\d*\.?\d*$"""))) {
                            goalInput = newValue
                            inputError = null
                        }
                    },
                    label = { Text("Daily Target") },
                    prefix = {
                        Text(
                            text = "$currencySymbol ",
                            fontWeight = FontWeight.Bold,
                            color = SolarTheme.colors.textPrimary
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = inputError != null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SolarAmber,
                        focusedLabelColor = SolarAmber,
                        cursorColor = SolarAmber
                    )
                )

                if (periodDays > 1) {
                    val parsedDaily = goalInput.trim().toDoubleOrNull() ?: 0.0
                    val scaledPeriodGoal = parsedDaily * periodDays
                    val periodTitle = when (selectedRange) {
                        com.solarad.app.model.TimeRange.LAST_7_DAYS -> "Past 7 Days"
                        com.solarad.app.model.TimeRange.THIS_MONTH -> "This Month ($periodDays days)"
                        com.solarad.app.model.TimeRange.DAYS_30 -> "Past 30 Days"
                        com.solarad.app.model.TimeRange.CUSTOM -> "Custom Range ($periodDays days)"
                        else -> "$periodDays Days"
                    }
                    val formattedPeriodGoal = if (scaledPeriodGoal % 1.0 == 0.0) {
                        scaledPeriodGoal.toLong().toString()
                    } else {
                        com.solarad.app.ui.utils.formatAmount(scaledPeriodGoal)
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SolarAmber.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, SolarAmber.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "PERIOD TARGET PREVIEW",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SolarAmber,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$periodTitle: $currencySymbol$formattedPeriodGoal",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SolarTheme.colors.textPrimary
                            )
                            Text(
                                text = "Automatically scaled ($currencySymbol${if (goalInput.isBlank()) "0" else goalInput}/day × $periodDays days)",
                                fontSize = 11.sp,
                                color = SolarTheme.colors.textSecondary
                            )
                        }
                    }
                }

                if (inputError != null) {
                    Text(
                        text = inputError ?: "",
                        fontSize = 11.sp,
                        color = CoralCrimson,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = goalInput.trim().toDoubleOrNull()
                    if (parsed == null || parsed <= 0.0) {
                        inputError = "Please enter an amount greater than 0"
                    } else {
                        onSaveGoal(parsed)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SolarAmber),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Goal", fontWeight = FontWeight.Bold, color = PureWhite)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isCustomGoal && onResetToAuto != null) {
                    TextButton(onClick = {
                        onResetToAuto()
                        onDismiss()
                    }) {
                        Text(
                            text = "Reset to Auto",
                            color = SolarTheme.colors.textSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = SolarTheme.colors.textSecondary)
                }
            }
        }
    )
}
