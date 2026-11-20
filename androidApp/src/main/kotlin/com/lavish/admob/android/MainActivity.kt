package com.lavish.admob.android

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
import com.solarad.app.App
import com.solarad.app.data.AdMobRepository
import com.solarad.app.data.initAndroidSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                val token = GoogleAuthUtil.getToken(this@MainActivity, androidAccount, ADMOB_SCOPE)
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

    private fun renewAdMobToken(account: GoogleSignInAccount): String? {
        val email = account.email ?: ""
        val androidAccount = account.account ?: Account(email, "com.google")
        val staleToken = repository.getCurrentToken()
        if (!staleToken.isNullOrBlank()) {
            try {
                GoogleAuthUtil.clearToken(this, staleToken)
            } catch (_: Exception) {}
        }
        return GoogleAuthUtil.getToken(this, androidAccount, ADMOB_SCOPE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        initAndroidSettings(this)
        repository = AdMobRepository()

        // Hook up automatic silent token refresher so tokens never expire
        repository.tokenRefresher = {
            withContext(Dispatchers.IO) {
                try {
                    GoogleSignIn.getLastSignedInAccount(this@MainActivity)?.let { renewAdMobToken(it) }
                } catch (e: Exception) {
                    Log.w(TAG, "Automatic token renewal failed: ${e.message}")
                    null
                }
            }
        }

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
        if (lastAccount != null) {
            val photoUrl = lastAccount.photoUrl?.toString()
            if (!photoUrl.isNullOrBlank()) {
                repository.updateAvatarUrl(photoUrl)
            }
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val freshToken = renewAdMobToken(lastAccount)
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
