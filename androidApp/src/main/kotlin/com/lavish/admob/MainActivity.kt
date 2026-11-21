package com.lavish.admob

import android.accounts.Account
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.lavish.admob.App
import com.lavish.admob.data.AdMobRepository
import com.lavish.admob.data.initAndroidSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Process-wide repository holder. Survives Activity recreation (locale/theme/config changes)
 * so we don't spin up a second repository + coroutine scope and leak the old Activity.
 */
private object RepositoryHolder {
    @Volatile private var instance: AdMobRepository? = null

    fun get(context: android.content.Context): AdMobRepository =
        instance ?: synchronized(this) {
            instance ?: run {
                val app = context.applicationContext
                initAndroidSettings(app)
                AdMobRepository().also { repo ->
                    // Uses only applicationContext: never captures an Activity
                    repo.tokenRefresher = {
                        withContext(Dispatchers.IO) {
                            try {
                                GoogleSignIn.getLastSignedInAccount(app)?.let { renewAdMobToken(app, repo, it) }
                            } catch (e: Exception) {
                                Log.w("AdCashAuth", "Automatic token renewal failed: ${e.message}")
                                null
                            }
                        }
                    }
                    instance = repo
                }
            }
        }
}

private const val ADMOB_OAUTH_SCOPE = "oauth2:https://www.googleapis.com/auth/admob.readonly https://www.googleapis.com/auth/admob.report https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email"

private fun renewAdMobToken(context: android.content.Context, repository: AdMobRepository, account: GoogleSignInAccount): String? {
    val app = context.applicationContext
    val email = account.email ?: ""
    val androidAccount = account.account ?: Account(email, "com.google")
    val staleToken = repository.getCurrentToken()
    if (!staleToken.isNullOrBlank()) {
        try {
            GoogleAuthUtil.clearToken(app, staleToken)
        } catch (_: Exception) {}
    }
    return GoogleAuthUtil.getToken(app, androidAccount, ADMOB_OAUTH_SCOPE)
}

class MainActivity : ComponentActivity() {
    private lateinit var repository: AdMobRepository
    private lateinit var googleSignInClient: GoogleSignInClient
    private var pendingAccount: GoogleSignInAccount? = null

    companion object {
        private const val TAG = "AdCashAuth"
        private const val WEB_CLIENT_ID = "623789927018-3g5s745ahi6epl46fihj4ukoelvkq9qp.apps.googleusercontent.com"
        private const val ADMOB_SCOPE = "oauth2:https://www.googleapis.com/auth/admob.readonly https://www.googleapis.com/auth/admob.report https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email"
    }

    private val consentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val account = pendingAccount
        if (result.resultCode == RESULT_OK && account != null) {
            obtainTokenAndLogin(account)
        } else {
            val errorMsg = "AdMob authorization was not completed."
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            repository.setAuthError(errorMsg)
        }
    }

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data: Intent? = result.data
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            val name = account.displayName ?: account.givenName ?: "Google User"
            val email = account.email ?: "publisher@gmail.com"
            Log.d(TAG, "Signed in as $name ($email)")
            obtainTokenAndLogin(account)
        } catch (e: ApiException) {
            val isCancelled = e.statusCode == CommonStatusCodes.CANCELED || e.statusCode == 12501
            val errorMsg = if (isCancelled) {
                "Google Sign-In was cancelled."
            } else {
                "Google Sign-In failed (code: ${e.statusCode}). Please check Google Play Services."
            }
            Log.e(TAG, errorMsg, e)
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
            repository.setAuthError(errorMsg)
        } catch (e: Exception) {
            val errorMsg = "Sign-in error: ${e.localizedMessage ?: "Unknown error"}"
            Log.e(TAG, errorMsg, e)
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
            repository.setAuthError(errorMsg)
        }
    }

    private fun obtainTokenAndLogin(account: GoogleSignInAccount) {
        val name = account.displayName ?: account.givenName ?: "Google User"
        val email = account.email ?: "publisher@gmail.com"
        val photoUrl = account.photoUrl?.toString()
        val androidAccount = account.account ?: Account(email, "com.google")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Requesting AdMob OAuth token for $email...")
                val token = GoogleAuthUtil.getToken(applicationContext, androidAccount, ADMOB_SCOPE)
                Log.d(TAG, "Successfully acquired AdMob OAuth token!")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Signed in as $name", Toast.LENGTH_SHORT).show()
                    repository.loginWithOAuthToken(name, email, token, photoUrl)
                }
            } catch (e: UserRecoverableAuthException) {
                Log.w(TAG, "Consent required, launching user consent...", e)
                pendingAccount = account
                withContext(Dispatchers.Main) {
                    val intent = e.intent
                    if (intent != null) {
                        consentLauncher.launch(intent)
                    } else {
                        val msg = "AdMob consent is required to access your account."
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                        repository.setAuthError(msg)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "AdMob token acquisition error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    val errorMsg = "Could not acquire AdMob authorization token: ${e.localizedMessage ?: e.message}. Please verify your Google account has AdMob access."
                    Toast.makeText(this@MainActivity, errorMsg, Toast.LENGTH_LONG).show()
                    repository.setAuthError(errorMsg)
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Reuse the process-wide repository (token refresher is wired there with applicationContext)
        repository = RepositoryHolder.get(this)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestScopes(
                com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/admob.readonly"),
                com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/admob.report")
            )
            .requestEmail()
            .requestProfile()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // If user is already signed in on device, sync photo and silently renew token on startup
        val lastAccount = GoogleSignIn.getLastSignedInAccount(this)
        if (lastAccount != null && savedInstanceState == null) {
            val photoUrl = lastAccount.photoUrl?.toString()
            if (!photoUrl.isNullOrBlank()) {
                repository.updateAvatarUrl(photoUrl)
            }
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val freshToken = renewAdMobToken(applicationContext, repository, lastAccount)
                    if (!freshToken.isNullOrBlank()) {
                        withContext(Dispatchers.Main) {
                            repository.updateAccessToken(freshToken)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Silent startup token refresh failed: ${e.message}")
                }
            }
        }

        setContent {
            App(
                repository = repository,
                onGoogleSignInRequest = {
                    repository.clearAuthError()
                    try {
                        googleSignInClient.signOut().addOnCompleteListener {
                            val intent = googleSignInClient.signInIntent
                            signInLauncher.launch(intent)
                        }
                    } catch (e: Exception) {
                        try {
                            signInLauncher.launch(googleSignInClient.signInIntent)
                        } catch (e2: Exception) {
                            val errorMsg = "Could not launch Google Sign-In: ${e2.localizedMessage}"
                            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
                            repository.setAuthError(errorMsg)
                        }
                    }
                },
                onSignOutRequest = {
                    try {
                        googleSignInClient.signOut()
                    } catch (_: Exception) {
                    }
                    repository.logout()
                }
            )
        }
    }
}
