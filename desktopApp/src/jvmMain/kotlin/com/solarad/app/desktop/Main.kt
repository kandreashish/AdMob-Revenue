package com.solarad.app.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.solarad.app.App
import com.solarad.app.data.AdMobRepository
import com.solarad.app.desktop.auth.DesktopGoogleAuthManager
import com.solarad.app.theme.PureWhite
import com.solarad.app.theme.SolarAmber
import com.solarad.app.theme.SolarTheme
import kotlinx.coroutines.launch
import java.awt.Dimension

fun main() {
    val osName = System.getProperty("os.name").orEmpty().lowercase()
    val arch = System.getProperty("os.arch").orEmpty().lowercase()
    val isLinux = osName.contains("linux")
    val isArm = arch.contains("aarch64") || arch.contains("arm")

    // On Linux ARM (Raspberry Pi 4/5), default to SOFTWARE_FAST rendering if not specified,
    // avoiding the known Skiko OpenGL freeze/hang on VideoCore VI / Mesa drivers.
    if (isLinux && isArm) {
        if (System.getProperty("skiko.renderApi") == null && System.getenv("SKIKO_RENDER_API") == null) {
            System.setProperty("skiko.renderApi", "SOFTWARE_FAST")
        }
    }

    application {
        val windowState = rememberWindowState(width = 1120.dp, height = 780.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "AdDash - AdMob Dashboard",
        state = windowState,
        icon = painterResource("icons/icon.png")
    ) {
        window.minimumSize = Dimension(420, 560)

        val repository = remember { AdMobRepository() }
        val authManager = remember(repository) { DesktopGoogleAuthManager(repository) }
        val coroutineScope = rememberCoroutineScope()
        var isSigningIn by remember { mutableStateOf(false) }
        val activeAuthUrl by authManager.activeAuthUrl

        // Setup automatic token refresher so expired tokens renew silently
        remember(repository, authManager) {
            repository.tokenRefresher = {
                authManager.refreshAccessToken()
            }
            true
        }

        // On desktop launch, silently refresh token if a refresh token is stored
        LaunchedEffect(Unit) {
            val storedRefreshToken = repository.getStoredRefreshToken()
            if (!storedRefreshToken.isNullOrBlank()) {
                val freshToken = authManager.refreshAccessToken()
                if (freshToken != null) {
                    repository.updateAccessToken(freshToken)
                }
            }
        }

        App(
            repository = repository,
            onGoogleSignInRequest = {
                if (!isSigningIn) {
                    isSigningIn = true
                    repository.clearAuthError()
                    coroutineScope.launch {
                        try {
                            authManager.signIn(
                                onSuccess = { name, email, token, avatarUrl ->
                                    isSigningIn = false
                                    repository.loginWithOAuthToken(name, email, token, avatarUrl)
                                },
                                onError = { errorMsg ->
                                    isSigningIn = false
                                    repository.setAuthError(errorMsg)
                                }
                            )
                        } catch (e: Exception) {
                            isSigningIn = false
                            repository.setAuthError("Google Sign-In failed: ${e.message}")
                        }
                    }
                }
            },
            onSignOutRequest = {
                authManager.signOut()
            }
        )

        // Helpful interactive dialog when signing in
        if (activeAuthUrl != null) {
            val url = activeAuthUrl.orEmpty()
            var manualInput by remember { mutableStateOf("") }
            val clipboardManager = LocalClipboardManager.current

            AlertDialog(
                onDismissRequest = {
                    authManager.cancelSignIn()
                    isSigningIn = false
                },
                title = {
                    Text(
                        text = "Sign in with Google",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SolarTheme.colors.textPrimary
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "A browser window was opened for Google authorization. If it didn't open automatically, use the buttons below:",
                            fontSize = 13.sp,
                            color = SolarTheme.colors.textSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(url))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SolarAmber),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Copy Link", color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = {
                                    authManager.openBrowserDirectly(url)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Open Browser", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        HorizontalDivider(color = SolarTheme.colors.borderSubtle)
                        Text(
                            text = "If signing in from another device or SSH, paste the redirected link or authorization code here:",
                            fontSize = 11.sp,
                            color = SolarTheme.colors.textMuted
                        )
                        OutlinedTextField(
                            value = manualInput,
                            onValueChange = { manualInput = it },
                            placeholder = { Text("Paste http://127.0.0.1:8585/... or code", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        if (manualInput.isNotBlank()) {
                            Button(
                                onClick = {
                                    authManager.submitManualCode(manualInput)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Submit & Connect", color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = {
                        authManager.cancelSignIn()
                        isSigningIn = false
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
}
