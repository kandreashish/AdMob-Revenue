package com.solarad.app.data

import com.solarad.app.model.AppMonetization
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object AppIconResolver {
    private val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 8_000
            socketTimeoutMillis = 10_000
        }
    }
    // One long-lived scope; SupervisorJob so one failed lookup doesn't cancel the others.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val ogImagePattern1 = Regex("""<meta\s+property=["']og:image["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val ogImagePattern2 = Regex("""<meta\s+content=["']([^"']+)["']\s+property=["']og:image["']""", RegexOption.IGNORE_CASE)
    private val json = Json { ignoreUnknownKeys = true }
    private val settings = createDefaultSettings()

    // Thread-safe caches
    private val mutex = Mutex()
    // Copy-on-write immutable snapshot: lock-free reads from any thread, writes go through [putMemory].
    @kotlin.concurrent.Volatile private var memoryCache: Map<String, String> = emptyMap()
    private fun putMemory(id: String, value: String) {
        // Writes are rare (once per app); synchronising via a tiny CAS-free copy is fine because the
        // only racing writers are resolver coroutines that already hold [mutex], or idempotent disk-hit backfills.
        memoryCache = memoryCache + (id to value)
    }

    // Sentinel stored when resolution was attempted but returned nothing,
    // so we never re-fetch the same app repeatedly.
    private const val SENTINEL_NO_ICON = "__NO_ICON__"

    private val _iconUpdates = MutableStateFlow<Map<String, String>>(emptyMap())
    val iconUpdates: StateFlow<Map<String, String>> = _iconUpdates.asStateFlow()

    // Tracks IDs currently being fetched (mutex-protected)
    private val inFlight = mutableSetOf<String>()

    fun getCachedIconUrl(app: AppMonetization): String? {
        if (!app.iconUrl.isNullOrBlank()) return app.iconUrl
        val cached = memoryCache[app.id]
            ?: settings?.getStringOrNull("app_icon_${app.id}")
            ?: app.appStoreId?.let { settings?.getStringOrNull("app_icon_store_$it") }
        if (cached != null) {
            if (memoryCache[app.id] != cached) putMemory(app.id, cached)
            // Return null for the sentinel so callers still show the monogram
            return if (cached == SENTINEL_NO_ICON) null else cached
        }
        return null
    }

    fun requestIconResolve(app: AppMonetization) {
        // Already have a URL (or already know there is none)
        val rawCached = memoryCache[app.id]
            ?: settings?.getStringOrNull("app_icon_${app.id}")
            ?: app.appStoreId?.let { settings?.getStringOrNull("app_icon_store_$it") }
        if (rawCached != null) return  // covers both real URLs and the sentinel

        if (!app.iconUrl.isNullOrBlank()) return

        val key = app.id.ifBlank { app.name }

        scope.launch {
            val alreadyRunning = mutex.withLock {
                if (inFlight.contains(key)) true
                else { inFlight.add(key); false }
            }
            if (alreadyRunning) return@launch

            try {
                val resolvedUrl = resolveStoreIcon(app)
                mutex.withLock {
                    if (!resolvedUrl.isNullOrBlank()) {
                        putMemory(app.id, resolvedUrl)
                        settings?.putString("app_icon_${app.id}", resolvedUrl)
                        app.appStoreId?.let { sId ->
                            settings?.putString("app_icon_store_$sId", resolvedUrl)
                        }
                        _iconUpdates.update { it + (app.id to resolvedUrl) }
                    } else {
                        // Record that we tried and found nothing — prevents infinite retries
                        putMemory(app.id, SENTINEL_NO_ICON)
                        settings?.putString("app_icon_${app.id}", SENTINEL_NO_ICON)
                    }
                }
            } catch (_: Throwable) {
                // Network/parse error — do NOT cache the sentinel so we retry next time
            } finally {
                mutex.withLock { inFlight.remove(key) }
            }
        }
    }

    private suspend fun resolveStoreIcon(app: AppMonetization): String? {
        val isIos = app.platform.equals("iOS", ignoreCase = true)
        val storeId = app.appStoreId?.trim()

        if (isIos) {
            // 1. Try Apple iTunes lookup by numeric store ID or bundle ID
            if (!storeId.isNullOrBlank()) {
                val isNumeric = storeId.all { it.isDigit() }
                val itunesUrl = if (isNumeric) {
                    "https://itunes.apple.com/lookup?id=$storeId"
                } else {
                    "https://itunes.apple.com/lookup?bundleId=$storeId"
                }
                val icon = fetchItunesIcon(itunesUrl)
                if (icon != null) return icon
            }

            // 2. Fallback: iTunes search by app name
            if (app.name.isNotBlank()) {
                val searchUrl = "https://itunes.apple.com/search?term=${encodeParam(app.name)}&entity=software&limit=1"
                val icon = fetchItunesIcon(searchUrl)
                if (icon != null) return icon
            }
        } else {
            // Android: try Google Play icon via open-graph scrape
            if (!storeId.isNullOrBlank() && storeId.contains(".")) {
                val icon = fetchPlayStoreIcon(storeId)
                if (icon != null) return icon
            }

            // Fallback: iTunes search for cross-platform icon
            if (app.name.isNotBlank()) {
                val searchUrl = "https://itunes.apple.com/search?term=${encodeParam(app.name)}&entity=software&limit=1"
                val icon = fetchItunesIcon(searchUrl)
                if (icon != null) return icon
            }
        }
        return null
    }

    private suspend fun fetchItunesIcon(url: String): String? {
        return try {
            val response = client.get(url)
            if (!response.status.isSuccess()) return null
            val body = response.bodyAsText()
            val root = json.parseToJsonElement(body).jsonObject
            val results = root["results"]?.jsonArray ?: return null
            if (results.isEmpty()) return null
            val first = results[0].jsonObject
            val url512 = first["artworkUrl512"]?.jsonPrimitive?.content
            val url100 = first["artworkUrl100"]?.jsonPrimitive?.content
            val url60 = first["artworkUrl60"]?.jsonPrimitive?.content
            url512 ?: url100 ?: url60
        } catch (_: Throwable) {
            null
        }
    }

    private suspend fun fetchPlayStoreIcon(packageId: String): String? {
        return try {
            // Use the undocumented but stable Play Store icon endpoint directly —
            // much more reliable than HTML-scraping the JS-rendered store page.
            val directUrl = "https://play-lh.googleusercontent.com/app/icon?appid=$packageId"
            val directResponse = client.get(directUrl) {
                header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
            }
            if (directResponse.status.isSuccess()) return directUrl

            // Fallback: open-graph scrape of the store page
            val storeUrl = "https://play.google.com/store/apps/details?id=$packageId&hl=en"
            val response = client.get(storeUrl) {
                header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36")
            }
            if (!response.status.isSuccess()) return null
            val html = response.bodyAsText()

            val iconUrl = (ogImagePattern1.find(html) ?: ogImagePattern2.find(html))
                ?.groupValues?.get(1)
                ?.replace("&amp;", "&")
            if (!iconUrl.isNullOrBlank() &&
                (iconUrl.contains("googleusercontent.com") || iconUrl.contains("play-lh"))
            ) {
                iconUrl
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun encodeParam(text: String): String = text.trim().replace(" ", "+")
}
