package com.solarad.app.desktop.auth

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.awt.Desktop
import java.io.BufferedReader
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
        const val DEFAULT_CLIENT_ID = "623789927018-3g5s745ahi6epl46fihj4ukoelvkq9qp.apps.googleusercontent.com"
        const val DEFAULT_PORT = 8585
        const val REDIRECT_PATH = "/oauth2callback"
        const val AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
        const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
        const val USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v3/userinfo"
        const val ADMOB_SCOPES = "https://www.googleapis.com/auth/admob.readonly https://www.googleapis.com/auth/admob.report https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email"

        private const val TIMEOUT_MS = 120_000 // 2 minutes
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

    /**
     * Resolves the OAuth Client ID to use.
     */
    fun getClientId(): String {
        return DEFAULT_CLIENT_ID
    }

    /**
     * Initiates Google Sign-In via default system browser on macOS and Linux.
     */
    suspend fun signIn(
        onSuccess: (name: String, email: String, token: String, avatarUrl: String?) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        var serverSocket: ServerSocket? = null
        try {
            // 1. Generate PKCE verifier & challenge
            val codeVerifier = generateCodeVerifier()
            val codeChallenge = generateCodeChallenge(codeVerifier)
            val state = generateState()

            // 2. Bind loopback server on 127.0.0.1
            val (socket, port) = bindLoopbackSocket()
            serverSocket = socket
            serverSocket.soTimeout = TIMEOUT_MS

            val redirectUri = "http://127.0.0.1:$port$REDIRECT_PATH"

            // 3. Construct Google authorization URL
            val authUrl = buildAuthUrl(
                clientId = getClientId(),
                redirectUri = redirectUri,
                codeChallenge = codeChallenge,
                state = state
            )

            // 4. Launch browser
            val opened = openBrowser(authUrl)
            if (!opened) {
                onError("Could not automatically open a web browser. Please open this URL manually:\n$authUrl")
                return@withContext
            }

            // 5. Wait for the redirect callback
            val callbackResult = awaitCallback(serverSocket, state)
            if (callbackResult.error != null) {
                val detail = if (callbackResult.error == "redirect_uri_mismatch") {
                    "Google OAuth error: redirect_uri_mismatch. To authenticate on desktop, ensure 'http://127.0.0.1:$port$REDIRECT_PATH' is listed in your Google Cloud Console Authorized redirect URIs, or use a 'Desktop app' OAuth Client ID."
                } else {
                    "Sign-in was cancelled or failed: ${callbackResult.error}"
                }
                onError(detail)
                return@withContext
            }

            val authCode = callbackResult.code
            if (authCode.isNullOrBlank()) {
                onError("No authorization code received from Google.")
                return@withContext
            }

            // 6. Exchange code for access & refresh tokens
            val tokenResponse = exchangeCodeForToken(
                code = authCode,
                codeVerifier = codeVerifier,
                redirectUri = redirectUri
            )

            if (tokenResponse == null || tokenResponse.access_token.isBlank()) {
                onError("Failed to exchange authorization code for an AdMob access token.")
                return@withContext
            }

            // Save refresh token if provided
            tokenResponse.refresh_token?.let { refreshToken ->
                repository.saveRefreshToken(refreshToken)
            }

            // 7. Fetch user profile
            val userInfo = fetchUserInfo(tokenResponse.access_token)
            val name = userInfo.name.ifBlank { "AdMob Publisher" }
            val email = userInfo.email.ifBlank { "" }
            val avatarUrl = userInfo.picture

            // 8. Log in via repository
            withContext(Dispatchers.Main) {
                onSuccess(name, email, tokenResponse.access_token, avatarUrl)
            }

        } catch (e: Exception) {
            val message = e.localizedMessage ?: e.message ?: "Authentication error"
            onError("Google Sign-In failed: $message")
        } finally {
            try {
                serverSocket?.close()
            } catch (_: Exception) {}
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
                    append("refresh_token", storedRefreshToken)
                    append("grant_type", "refresh_token")
                }
            )

            val text = response.bodyAsText()
            if (!response.status.isSuccess()) {
                return@withContext null
            }

            val json = jsonParser.parseToJsonElement(text).jsonObject
            val newAccessToken = json["access_token"]?.jsonPrimitive?.content
            if (!newAccessToken.isNullOrBlank()) {
                repository.updateAccessToken(newAccessToken)
                return@withContext newAccessToken
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Clears desktop authentication state and stored tokens on Sign Out.
     */
    fun signOut() {
        repository.clearRefreshToken()
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    private fun bindLoopbackSocket(): Pair<ServerSocket, Int> {
        val loopback = InetAddress.getByName("127.0.0.1")
        return try {
            // Prefer DEFAULT_PORT (8585) so redirect URI is predictable
            val socket = ServerSocket(DEFAULT_PORT, 1, loopback)
            Pair(socket, DEFAULT_PORT)
        } catch (_: Exception) {
            // Fallback to ephemeral free port
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

    private fun openBrowser(url: String): Boolean {
        // 1. Try AWT Desktop browse
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
                return true
            }
        } catch (_: Throwable) {}

        // 2. Try OS-specific terminal launchers (macOS 'open', Linux 'xdg-open')
        val os = System.getProperty("os.name", "").lowercase()
        val commands = when {
            os.contains("mac") || os.contains("darwin") -> listOf(arrayOf("/usr/bin/open", url), arrayOf("open", url))
            else -> listOf(
                arrayOf("xdg-open", url),
                arrayOf("gio", "open", url),
                arrayOf("gnome-open", url),
                arrayOf("sensible-browser", url),
                arrayOf("firefox", url),
                arrayOf("chromium-browser", url),
                arrayOf("google-chrome", url)
            )
        }

        for (cmd in commands) {
            try {
                ProcessBuilder(*cmd).start()
                return true
            } catch (_: Throwable) {}
        }
        return false
    }

    private data class CallbackResult(val code: String? = null, val error: String? = null)

    private fun awaitCallback(serverSocket: ServerSocket, expectedState: String): CallbackResult {
        var client: Socket? = null
        try {
            client = serverSocket.accept()
            val reader = BufferedReader(InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))
            val requestLine = reader.readLine() ?: return CallbackResult(error = "Empty request received")

            // Parse GET /oauth2callback?... HTTP/1.1
            val parts = requestLine.split(" ")
            if (parts.size < 2) return CallbackResult(error = "Invalid HTTP request")

            val fullPath = parts[1]
            val queryIndex = fullPath.indexOf('?')
            val queryParams = mutableMapOf<String, String>()

            if (queryIndex != -1 && queryIndex < fullPath.length - 1) {
                val query = fullPath.substring(queryIndex + 1)
                for (pair in query.split("&")) {
                    val eq = pair.indexOf('=')
                    if (eq != -1) {
                        val key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8.name())
                        val value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8.name())
                        queryParams[key] = value
                    }
                }
            }

            val error = queryParams["error"]
            val code = queryParams["code"]
            val state = queryParams["state"]

            // Verify state
            if (state != null && state != expectedState) {
                sendHtmlResponse(client, false, "Security verification failed (state mismatch).")
                return CallbackResult(error = "CSRF state token mismatch.")
            }

            if (error != null) {
                sendHtmlResponse(client, false, "Authentication error: $error")
                return CallbackResult(error = error)
            }

            if (code != null) {
                sendHtmlResponse(client, true, "Authentication successful! You may close this tab and return to AdDash.")
                return CallbackResult(code = code)
            }

            sendHtmlResponse(client, false, "No authorization code found in callback.")
            return CallbackResult(error = "Missing authorization code.")
        } finally {
            try {
                client?.close()
            } catch (_: Exception) {}
        }
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

    private suspend fun exchangeCodeForToken(
        code: String,
        codeVerifier: String,
        redirectUri: String
    ): GoogleTokenResponse? {
        val response = httpClient.submitForm(
            url = TOKEN_ENDPOINT,
            formParameters = Parameters.build {
                append("client_id", getClientId())
                append("code", code)
                append("code_verifier", codeVerifier)
                append("grant_type", "authorization_code")
                append("redirect_uri", redirectUri)
            }
        )

        val text = response.bodyAsText()
        if (!response.status.isSuccess()) {
            return null
        }

        return try {
            jsonParser.decodeFromString<GoogleTokenResponse>(text)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun fetchUserInfo(accessToken: String): DesktopGoogleUserInfo {
        return try {
            val response = httpClient.get(USERINFO_ENDPOINT) {
                header("Authorization", "Bearer $accessToken")
            }
            if (response.status.isSuccess()) {
                val text = response.bodyAsText()
                jsonParser.decodeFromString<DesktopGoogleUserInfo>(text)
            } else {
                DesktopGoogleUserInfo()
            }
        } catch (_: Exception) {
            DesktopGoogleUserInfo()
        }
    }
}
