package com.lavish.admob

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.lavish.admob.data.AdMobRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UIKit.UIViewController
import kotlin.coroutines.resume

/**
 * Implemented in Swift (GoogleAuthBridge.swift) on top of the GoogleSignIn-iOS SDK.
 * All callbacks may be invoked from any thread.
 */
interface IosAuthBridge {
    fun signIn(
        onSuccess: (name: String, email: String, accessToken: String, avatarUrl: String?) -> Unit,
        onError: (message: String) -> Unit
    )

    /** Returns a fresh AdMob access token, or null if the user must sign in again. */
    fun refreshToken(onResult: (accessToken: String?) -> Unit)

    fun signOut()
}

/** Process-wide repository so SwiftUI view rebuilds never create a second instance. */
private object IosRepositoryHolder {
    private var instance: AdMobRepository? = null

    fun get(bridge: IosAuthBridge): AdMobRepository =
        instance ?: AdMobRepository().also { repo ->
            repo.tokenRefresher = {
                suspendCancellableCoroutine { cont ->
                    bridge.refreshToken { token -> if (cont.isActive) cont.resume(token) }
                }
            }
            instance = repo
        }
}

fun MainViewController(authBridge: IosAuthBridge): UIViewController {
    val repository = IosRepositoryHolder.get(authBridge)
    return ComposeUIViewController {
        App(
            repository = remember { repository },
            onGoogleSignInRequest = {
                repository.clearAuthError()
                authBridge.signIn(
                    onSuccess = { name, email, token, avatar ->
                        repository.loginWithOAuthToken(name, email, token, avatar)
                    },
                    onError = { message -> repository.setAuthError(message) }
                )
            },
            onSignOutRequest = { authBridge.signOut() }
        )
    }
}
