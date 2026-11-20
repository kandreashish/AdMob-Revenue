package com.solarad.app.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.solarad.app.App
import com.solarad.app.data.AdMobRepository
import com.solarad.app.desktop.auth.DesktopGoogleAuthManager
import kotlinx.coroutines.launch
import java.awt.Dimension

fun main() = application {
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
    }
}
