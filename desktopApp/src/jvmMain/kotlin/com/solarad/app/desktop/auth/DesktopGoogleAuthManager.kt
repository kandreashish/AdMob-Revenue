package com.solarad.app.desktop.auth

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.solarad.app.data.AdMobRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.awt.Desktop
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64

@Serializable
data class GoogleTokenResponse(
    val access_token: String = "",
    val refresh_token: String? = null,
    val expires_in: Long = 3600,
    val token_type: String = "Bearer",
    val scope: String = "",
    val id_token: String? = null
)

@Serializable
data class DesktopGoogleUserInfo(
    val sub: String = "",
    val name: String = "AdMob Publisher",
    val email: String = "publisher@gmail.com",
    val picture: String? = null
)

class DesktopGoogleAuthManager(
    private val repository: AdMobRepository
) {
    companion object {
        const val DEFAULT_CLIENT_ID = "623789927018-as6q5ju7q7j35vop4ba2m4f4f8pqb5b1.apps.googleusercontent.com"
        const val DEFAULT_CLIENT_SECRET = "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"
        const val DEFAULT_PORT = 8585
        const val REDIRECT_PATH = "/oauth2callback"
        const val AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
        const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
        const val USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v3/userinfo"
        const val ADMOB_SCOPES = "https://www.googleapis.com/auth/admob.readonly https://www.googleapis.com/auth/admob.report https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email"

        private const val TIMEOUT_MS = 180_000L // 3 minutes
    }

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(jsonParser)
        }
    }

    private val _activeAuthUrl = mutableStateOf<String?>(null)
    val activeAuthUrl: State<String?> = _activeAuthUrl

    private var pendingDeferred: CompletableDeferred<String>? = null
    private var activeServerSocket: ServerSocket? = null

    private fun logDesktop(msg: String) {
        println("[AdDash] $msg")
        try {
            val file = File(System.getProperty("user.home"), ".addash.log")
            file.appendText("[${Instant.now()}] $msg\n")
        } catch (_: Throwable) {}
    }

    fun getClientId(): String {
        return System.getenv("ADDASH_CLIENT_ID")
            ?.takeIf { it.isNotBlank() }
            ?: repository.getCustomClientId()
            ?: DEFAULT_CLIENT_ID
    }

    fun getClientSecret(): String {
        return System.getenv("ADDASH_CLIENT_SECRET")
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_CLIENT_SECRET
    }

    /**
     * Submits an authorization code or redirect URL manually (e.g. from copy/paste).
     */
    fun submitManualCode(input: String) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return

        val code = if (trimmed.contains("code=")) {
            val query = if (trimmed.contains("?")) trimmed.substringAfter("?") else trimmed
            query.split("&")
                .firstOrNull { it.startsWith("code=") }
                ?.substringAfter("code=")
                ?.let { URLDecoder.decode(it.replace("+", "%2B"), StandardCharsets.UTF_8.name()) }
                ?: trimmed
        } else {
            trimmed
        }

        logDesktop("Manual authorization code submitted (length: ${code.length})")
        pendingDeferred?.complete(code)
    }

    /**
     * Cancels the active sign-in flow.
     */
    fun cancelSignIn() {
        _activeAuthUrl.value = null
        repository.setRefreshing(false)
        try {
            activeServerSocket?.close()
        } catch (_: Exception) {}
        pendingDeferred?.cancel()
        pendingDeferred = null
    }

    /**
     * Initiates Google Sign-In via browser and loopback server.
     */
    suspend fun signIn(
        onSuccess: (name: String, email: String, token: String, avatarUrl: String?) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        var serverSocket: ServerSocket? = null
        val deferred = CompletableDeferred<String>()
        pendingDeferred = deferred

        try {
            repository.setRefreshing(true)

            // 1. Generate PKCE verifier & challenge
            val codeVerifier = generateCodeVerifier()
            val codeChallenge = generateCodeChallenge(codeVerifier)
            val state = generateState()

            // 2. Bind loopback server on 127.0.0.1
            val (socket, port) = bindLoopbackSocket()
            serverSocket = socket
            activeServerSocket = socket
            serverSocket.soTimeout = TIMEOUT_MS.toInt()

            val redirectUri = "http://127.0.0.1:$port$REDIRECT_PATH"

            // 3. Construct Google authorization URL
            val authUrl = buildAuthUrl(
                clientId = getClientId(),
                redirectUri = redirectUri,
                codeChallenge = codeChallenge,
                state = state
            )

            withContext(Dispatchers.Main) {
                _activeAuthUrl.value = authUrl
            }

            logDesktop("Sign-in initiated with Client ID: ${getClientId()}")
            logDesktop("Redirect URI: $redirectUri")
            logDesktop("Auth URL: $authUrl")

            // 4. Try opening browser automatically
            openBrowserDirectly(authUrl)

            // 5. Start loopback listener in background coroutine
            val listenerScope = CoroutineScope(Dispatchers.IO)
            listenerScope.launch {
                try {
                    val result = awaitCallback(socket, state)
                    if (result.error != null) {
                        logDesktop("OAuth Callback Error: ${result.error}")
                        if (result.error == "redirect_uri_mismatch") {
                            onError("Google OAuth error: redirect_uri_mismatch. To authenticate, ensure 'http://127.0.0.1:$port$REDIRECT_PATH' is listed in your Google Cloud Console Authorized redirect URIs, or use a 'Desktop app' OAuth Client ID.")
                        } else {
                            onError("Sign-in cancelled or failed: ${result.error}")
                        }
                        deferred.cancel()
                    } else if (!result.code.isNullOrBlank()) {
                        logDesktop("Captured authorization code from browser callback (length: ${result.code.length})")
                        deferred.complete(result.code)
                    }
                } catch (e: Exception) {
                    // Socket closed or timed out
                }
            }

            // 6. Await either loopback callback or manual code entry
            val authCode = withTimeoutOrNull(TIMEOUT_MS) {
                deferred.await()
            }

            withContext(Dispatchers.Main) {
                _activeAuthUrl.value = null
            }

            if (authCode.isNullOrBlank()) {
                logDesktop("Sign-in cancelled or timed out before receiving code.")
                onError("Google Sign-In was cancelled or timed out.")
                repository.setRefreshing(false)
                return@withContext
            }

            logDesktop("Exchanging authorization code for OAuth tokens...")

            // 7. Exchange code for access & refresh tokens
            val tokenResult = exchangeCodeForToken(
                code = authCode,
                codeVerifier = codeVerifier,
                redirectUri = redirectUri
            )

            if (tokenResult.tokenResponse == null || tokenResult.tokenResponse.access_token.isBlank()) {
                val detail = tokenResult.errorMessage ?: "Failed to exchange authorization code for an AdMob access token."
                logDesktop("Token exchange failed: $detail")
                onError("Sign-In failed: $detail")
                repository.setRefreshing(false)
                return@withContext
            }

            val tokenResponse = tokenResult.tokenResponse

            // Save refresh token if provided
            tokenResponse.refresh_token?.let { refreshToken ->
                repository.saveRefreshToken(refreshToken)
                logDesktop("Saved refresh token successfully.")
            }

            // 8. Fetch user profile
            val userInfo = fetchUserInfo(tokenResponse.access_token)
            val name = userInfo.name.ifBlank { "AdMob Publisher" }
            val email = userInfo.email.ifBlank { "" }
            val avatarUrl = userInfo.picture

            logDesktop("Successfully authenticated as $name ($email)")

            // 9. Complete login via repository
            withContext(Dispatchers.Main) {
                onSuccess(name, email, tokenResponse.access_token, avatarUrl)
            }

        } catch (e: Exception) {
            val message = e.localizedMessage ?: e.message ?: "Authentication error"
            logDesktop("General exception during signIn: $message")
            onError("Google Sign-In failed: $message")
            repository.setRefreshing(false)
        } finally {
            withContext(Dispatchers.Main) {
                _activeAuthUrl.value = null
            }
            try {
                serverSocket?.close()
            } catch (_: Exception) {}
            activeServerSocket = null
            pendingDeferred = null
        }
    }

    /**
     * Silently refreshes the OAuth access token using the stored refresh token.
     */
    suspend fun refreshAccessToken(): String? = withContext(Dispatchers.IO) {
        val storedRefreshToken = repository.getStoredRefreshToken() ?: return@withContext null
        if (storedRefreshToken.isBlank()) return@withContext null

        try {
            val response = httpClient.submitForm(
                url = TOKEN_ENDPOINT,
                formParameters = Parameters.build {
                    append("client_id", getClientId())
                    append("client_secret", getClientSecret())
                    append("refresh_token", storedRefreshToken)
                    append("grant_type", "refresh_token")
                }
            )

            val text = response.bodyAsText()
            if (!response.status.isSuccess()) {
                logDesktop("Token refresh failed with HTTP ${response.status.value}: $text")
                return@withContext null
            }

            val json = jsonParser.parseToJsonElement(text).jsonObject
            val newAccessToken = json["access_token"]?.jsonPrimitive?.content
            if (!newAccessToken.isNullOrBlank()) {
                repository.updateAccessToken(newAccessToken)
                logDesktop("Silently renewed access token.")
                return@withContext newAccessToken
            }
            null
        } catch (e: Exception) {
            logDesktop("Exception during token refresh: ${e.message}")
            null
        }
    }

    fun signOut() {
        repository.clearRefreshToken()
        logDesktop("User signed out from desktop.")
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    private fun bindLoopbackSocket(): Pair<ServerSocket, Int> {
        val loopback = InetAddress.getByName("127.0.0.1")
        return try {
            val socket = ServerSocket(DEFAULT_PORT, 1, loopback)
            Pair(socket, DEFAULT_PORT)
        } catch (_: Exception) {
            val socket = ServerSocket(0, 1, loopback)
            Pair(socket, socket.localPort)
        }
    }

    internal fun buildAuthUrl(
        clientId: String,
        redirectUri: String,
        codeChallenge: String,
        state: String
    ): String {
        val encodedRedirect = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.name())
        val encodedScope = URLEncoder.encode(ADMOB_SCOPES, StandardCharsets.UTF_8.name())
        val encodedState = URLEncoder.encode(state, StandardCharsets.UTF_8.name())
        val encodedChallenge = URLEncoder.encode(codeChallenge, StandardCharsets.UTF_8.name())

        return "$AUTH_ENDPOINT?" +
                "client_id=$clientId&" +
                "redirect_uri=$encodedRedirect&" +
                "response_type=code&" +
                "scope=$encodedScope&" +
                "code_challenge=$encodedChallenge&" +
                "code_challenge_method=S256&" +
                "state=$encodedState&" +
                "access_type=offline&" +
                "prompt=consent"
    }

    internal fun generateCodeVerifier(): String {
        val bytes = ByteArray(64)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    internal fun generateCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(verifier.toByteArray(StandardCharsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
    }

    internal fun generateState(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun openBrowserDirectly(url: String): Boolean {
        val os = System.getProperty("os.name", "").lowercase()
        val isMac = os.contains("mac") || os.contains("darwin")

        if (isMac) {
            try {
                ProcessBuilder("/usr/bin/open", url).start()
                return true
            } catch (_: Throwable) {}
        }

        if (!isMac) {
            val linuxCommands = listOf(
                listOf("xdg-open", url),
                listOf("chromium-browser", url),
                listOf("chromium", url),
                listOf("google-chrome", url),
                listOf("firefox", url),
                listOf("gio", "open", url),
                listOf("gnome-open", url),
                listOf("sensible-browser", url)
            )

            for (cmd in linuxCommands) {
                try {
                    val pb = ProcessBuilder(cmd)
                    val env = pb.environment()
                    if (env["DISPLAY"].isNullOrBlank() && !System.getenv("DISPLAY").isNullOrBlank()) {
                        env["DISPLAY"] = System.getenv("DISPLAY")
                    }
                    pb.start()
                    return true
                } catch (_: Throwable) {}
            }
        }

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
                return true
            }
        } catch (_: Throwable) {}

        return false
    }

    private data class CallbackResult(val code: String? = null, val error: String? = null)

    private fun awaitCallback(serverSocket: ServerSocket, expectedState: String): CallbackResult {
        while (!serverSocket.isClosed) {
            var client: Socket? = null
            try {
                client = serverSocket.accept()
                val reader = BufferedReader(InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))
                val requestLine = reader.readLine() ?: continue

                val parts = requestLine.split(" ")
                if (parts.size < 2) {
                    client.close()
                    continue
                }

                val fullPath = parts[1]
                if (fullPath.startsWith("/favicon.ico")) {
                    client.close()
                    continue
                }

                val queryIndex = fullPath.indexOf('?')
                val queryParams = mutableMapOf<String, String>()

                if (queryIndex != -1 && queryIndex < fullPath.length - 1) {
                    val query = fullPath.substring(queryIndex + 1)
                    for (pair in query.split("&")) {
                        val eq = pair.indexOf('=')
                        if (eq != -1) {
                            val rawKey = pair.substring(0, eq)
                            val rawValue = pair.substring(eq + 1)
                            val key = URLDecoder.decode(rawKey.replace("+", "%2B"), StandardCharsets.UTF_8.name())
                            val value = URLDecoder.decode(rawValue.replace("+", "%2B"), StandardCharsets.UTF_8.name())
                            queryParams[key] = value
                        }
                    }
                }

                val error = queryParams["error"]
                val code = queryParams["code"]
                val state = queryParams["state"]

                if (state != null && state != expectedState) {
                    sendHtmlResponse(client, false, "Security verification failed (state mismatch).")
                    return CallbackResult(error = "CSRF state token mismatch.")
                }

                if (error != null) {
                    sendHtmlResponse(client, false, "Authentication error: $error")
                    return CallbackResult(error = error)
                }

                if (!code.isNullOrBlank()) {
                    sendHtmlResponse(client, true, "Authentication successful! You may close this tab and return to AdDash.")
                    return CallbackResult(code = code)
                }

                sendHtmlResponse(client, false, "No authorization code found in callback.")
                return CallbackResult(error = "Missing authorization code.")
            } catch (e: Exception) {
                if (serverSocket.isClosed) break
            } finally {
                try { client?.close() } catch (_: Exception) {}
            }
        }
        return CallbackResult(error = "Socket closed or timed out.")
    }

    private fun sendHtmlResponse(socket: Socket, isSuccess: Boolean, message: String) {
        try {
            val writer = OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)
            val icon = if (isSuccess) "✅" else "⚠️"
            val title = if (isSuccess) "AdDash - Authorization Successful" else "AdDash - Authorization Failed"
            val heading = if (isSuccess) "Authentication Complete" else "Authentication Incomplete"
            val statusColor = if (isSuccess) "#38bdf8" else "#f87171"
            val badgeBg = if (isSuccess) "#0369a1" else "#7f1d1d"
            val badgeColor = if (isSuccess) "#e0f2fe" else "#fecaca"
            val badgeText = if (isSuccess) "AdDash Connected" else "Action Required"

            val html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <title>$title</title>
                  <style>
                    body {
                      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                      background-color: #0b1120;
                      color: #f8fafc;
                      display: flex;
                      align-items: center;
                      justify-content: center;
                      height: 100vh;
                      margin: 0;
                    }
                    .card {
                      background-color: #1e293b;
                      border: 1px solid #334155;
                      border-radius: 20px;
                      padding: 44px 36px;
                      text-align: center;
                      max-width: 420px;
                      box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.5);
                    }
                    .icon {
                      font-size: 46px;
                      margin-bottom: 14px;
                    }
                    h1 {
                      font-size: 22px;
                      font-weight: 700;
                      margin: 0 0 10px;
                      color: $statusColor;
                    }
                    p {
                      font-size: 14px;
                      color: #94a3b8;
                      line-height: 1.5;
                      margin: 0 0 22px;
                    }
                    .badge {
                      display: inline-block;
                      padding: 6px 14px;
                      border-radius: 999px;
                      background: $badgeBg;
                      color: $badgeColor;
                      font-size: 12px;
                      font-weight: 600;
                      letter-spacing: 0.3px;
                    }
                  </style>
                </head>
                <body>
                  <div class="card">
                    <div class="icon">$icon</div>
                    <h1>$heading</h1>
                    <p>$message</p>
                    <div class="badge">$badgeText</div>
                  </div>
                </body>
                </html>
            """.trimIndent()

            val bytes = html.toByteArray(StandardCharsets.UTF_8)
            writer.write("HTTP/1.1 200 OK\r\n")
            writer.write("Content-Type: text/html; charset=UTF-8\r\n")
            writer.write("Content-Length: ${bytes.size}\r\n")
            writer.write("Connection: close\r\n\r\n")
            writer.write(html)
            writer.flush()
        } catch (_: Exception) {}
    }

    private data class TokenResult(
        val tokenResponse: GoogleTokenResponse? = null,
        val errorMessage: String? = null
    )

    private suspend fun exchangeCodeForToken(
        code: String,
        codeVerifier: String,
        redirectUri: String
    ): TokenResult {
        val cleanCode = code.trim()
        val formParams = Parameters.build {
            append("client_id", getClientId())
            append("client_secret", getClientSecret())
            append("code", cleanCode)
            append("code_verifier", codeVerifier)
            append("grant_type", "authorization_code")
            append("redirect_uri", redirectUri)
        }

        return try {
            logDesktop("Submitting token exchange for code to $TOKEN_ENDPOINT...")
            val response = httpClient.submitForm(
                url = TOKEN_ENDPOINT,
                formParameters = formParams
            )

            val text = response.bodyAsText()
            logDesktop("Token endpoint response: HTTP ${response.status.value} -> $text")

            if (!response.status.isSuccess()) {
                val detail = try {
                    val json = jsonParser.parseToJsonElement(text).jsonObject
                    val err = json["error"]?.jsonPrimitive?.content ?: "error"
                    val desc = json["error_description"]?.jsonPrimitive?.content
                    if (desc != null) "$err: $desc" else err
                } catch (_: Exception) {
                    text
                }
                TokenResult(errorMessage = detail)
            } else {
                val json = jsonParser.parseToJsonElement(text).jsonObject
                val accessToken = json["access_token"]?.jsonPrimitive?.content ?: ""
                val refreshToken = json["refresh_token"]?.jsonPrimitive?.content
                val expiresIn = json["expires_in"]?.jsonPrimitive?.content?.toLongOrNull() ?: 3600L
                val tokenType = json["token_type"]?.jsonPrimitive?.content ?: "Bearer"
                val idToken = json["id_token"]?.jsonPrimitive?.content
                val token = GoogleTokenResponse(
                    access_token = accessToken,
                    refresh_token = refreshToken,
                    expires_in = expiresIn,
                    token_type = tokenType,
                    id_token = idToken
                )
                TokenResult(tokenResponse = token)
            }
        } catch (e: Exception) {
            logDesktop("Network exception during token exchange: ${e.message}")
            TokenResult(errorMessage = "Network exception: ${e.message}")
        }
    }

    private suspend fun fetchUserInfo(accessToken: String): DesktopGoogleUserInfo {
        return try {
            val response = httpClient.get(USERINFO_ENDPOINT) {
                header("Authorization", "Bearer $accessToken")
            }
            if (response.status.isSuccess()) {
                val text = response.bodyAsText()
                val json = jsonParser.parseToJsonElement(text).jsonObject
                val sub = json["sub"]?.jsonPrimitive?.content ?: ""
                val name = json["name"]?.jsonPrimitive?.content ?: "AdMob Publisher"
                val email = json["email"]?.jsonPrimitive?.content ?: ""
                val picture = json["picture"]?.jsonPrimitive?.content
                DesktopGoogleUserInfo(sub = sub, name = name, email = email, picture = picture)
            } else {
                DesktopGoogleUserInfo()
            }
        } catch (_: Exception) {
            DesktopGoogleUserInfo()
        }
    }
}
