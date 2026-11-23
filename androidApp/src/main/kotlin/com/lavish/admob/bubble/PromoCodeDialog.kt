package com.lavish.admob.bubble

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private const val CODE_LEN = 8

/**
 * "Redeem promo code" dialog (app theme) with an 8-box OTP-style input.
 * Auto-submits when the 8th digit is typed; shakes + clears on a rejected code.
 */
@Composable
fun PromoCodeDialog(
    isWorking: Boolean,
    error: String?,
    onRedeem: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var code by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val shake = remember { Animatable(0f) }

    LaunchedEffect(Unit) { focus.requestFocus(); keyboard?.show() }

    // Rejected => shake the boxes, then clear so the user can retype.
    LaunchedEffect(error) {
        if (error == null) return@LaunchedEffect
        for (x in listOf(14f, -12f, 9f, -6f, 3f, 0f)) shake.animateTo(x, tween(45))
        code = ""
        focus.requestFocus()
    }

    fun submit() {
        if (code.length == CODE_LEN && !isWorking) {
            keyboard?.hide()
            onRedeem(code)
        }
    }

    val cs = MaterialTheme.colorScheme
    Dialog(onDismissRequest = { if (!isWorking) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = cs.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box {
                IconButton(
                    onClick = onDismiss,
                    enabled = !isWorking,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                ) { Icon(Icons.Outlined.Close, "Close", tint = cs.onSurfaceVariant) }

                Column(
                    Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier.size(52.dp).background(cs.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Outlined.CardGiftcard, null, tint = cs.primary, modifier = Modifier.size(26.dp)) }
                    Spacer(Modifier.height(14.dp))
                    Text("Redeem promo code", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Enter your 8-digit code to unlock the bubble forever.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))

                    BasicTextField(
                        value = code,
                        onValueChange = { v ->
                            if (isWorking) return@BasicTextField
                            code = v.filter { it.isDigit() }.take(CODE_LEN)
                            if (code.length == CODE_LEN) submit()
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().focusRequester(focus).offset(x = shake.value.dp),
                        decorationBox = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                repeat(CODE_LEN) { i ->
                                    if (i == 4) Spacer(Modifier.width(8.dp))
                                    OtpBox(
                                        char = code.getOrNull(i),
                                        focused = i == code.length && !isWorking,
                                        isError = error != null && code.isEmpty(),
                                        modifier = Modifier.size(width = 28.dp, height = 40.dp)
                                    )
                                }
                            }
                        }
                    )

                    Text(
                        error ?: " ",
                        color = cs.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 10.dp).alpha(if (error != null) 1f else 0f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = ::submit,
                        enabled = code.length == CODE_LEN && !isWorking,
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        if (isWorking) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text("Redeem", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun OtpBox(char: Char?, focused: Boolean, isError: Boolean, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val borderColor by animateColorAsState(
        when {
            isError -> cs.error
            focused -> cs.primary
            char != null -> cs.primary.copy(alpha = 0.6f)
            else -> cs.outline
        },
        label = "otpBorder"
    )
    val caret by rememberInfiniteTransition(label = "caret").animateFloat(
        1f, 0f, infiniteRepeatable(tween(530), RepeatMode.Reverse), label = "caretAlpha"
    )
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(cs.surfaceVariant)
            .border(if (focused) 2.dp else 1.5.dp, borderColor, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        when {
            char != null -> Text(char.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = cs.onSurface)
            focused -> Box(Modifier.size(width = 2.dp, height = 18.dp).alpha(caret).background(cs.primary))
        }
    }
}
