package com.solarad.app.data

import com.solarad.app.model.AppMonetization
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object AppIconResolver {
    private val client = HttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val settings = createDefaultSettings()
    private val memoryCache = mutableMapOf<String, String>()
    private val _iconUpdates = MutableStateFlow<Map<String, String>>(emptyMap())
    val iconUpdates: StateFlow<Map<String, String>> = _iconUpdates.asStateFlow()

    private val inFlight = mutableSetOf<String>()

    fun getCachedIconUrl(app: AppMonetization): String? {
        if (!app.iconUrl.isNullOrBlank()) return app.iconUrl
        val cached = memoryCache[app.id]
            ?: settings?.getStringOrNull("app_icon_${app.id}")
            ?: (app.appStoreId?.let { settings?.getStringOrNull("app_icon_store_$it") })
        if (cached != null) {
            memoryCache[app.id] = cached
            return cached
        }
        return null
    }

    fun requestIconResolve(app: AppMonetization) {
        val existing = getCachedIconUrl(app)
        if (existing != null) return

        val key = app.id.ifBlank { app.name }
        if (inFlight.contains(key)) return
        inFlight.add(key)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resolvedUrl = resolveStoreIcon(app)
                if (!resolvedUrl.isNullOrBlank()) {
                    memoryCache[app.id] = resolvedUrl
                    settings?.putString("app_icon_${app.id}", resolvedUrl)
                    val sId = app.appStoreId
                    if (sId != null) {
                        settings?.putString("app_icon_store_$sId", resolvedUrl)
                    }
                    val updated = _iconUpdates.value.toMutableMap()
                    updated[app.id] = resolvedUrl
                    _iconUpdates.value = updated
                }
            } catch (_: Throwable) {
                // Ignore resolution failure; fallback to monogram
            } finally {
                inFlight.remove(key)
            }
        }
    }

    private suspend fun resolveStoreIcon(app: AppMonetization): String? {
        val isIos = app.platform.equals("iOS", ignoreCase = true)
        val storeId = app.appStoreId?.trim()

        if (isIos) {
            // 1. Try Apple iTunes lookup by store ID
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

            // 2. Try iTunes search by app name
            if (app.name.isNotBlank()) {
                val searchUrl = "https://itunes.apple.com/search?term=${encodeParam(app.name)}&entity=software&limit=1"
                val icon = fetchItunesIcon(searchUrl)
                if (icon != null) return icon
            }
        } else {
            // Android Google Play lookup
            if (!storeId.isNullOrBlank() && storeId.contains(".")) {
                val playUrl = "https://play.google.com/store/apps/details?id=$storeId&hl=en"
                val icon = fetchPlayStoreIcon(playUrl)
                if (icon != null) return icon
            }

            // Fallback: Check iTunes for cross-platform matching icon
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

    private suspend fun fetchPlayStoreIcon(url: String): String? {
        return try {
            val response = client.get(url) {
                header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
            }
            if (!response.status.isSuccess()) return null
            val html = response.bodyAsText()
            // Look for <meta property="og:image" content="...">
            val metaPattern = Regex("""<meta\s+property=["']og:image["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            val match = metaPattern.find(html) ?: Regex("""<meta\s+content=["']([^"']+)["']\s+property=["']og:image["']""", RegexOption.IGNORE_CASE).find(html)
            val iconUrl = match?.groupValues?.get(1)?.replace("&amp;", "&")
            if (!iconUrl.isNullOrBlank() && (iconUrl.contains("googleusercontent.com") || iconUrl.contains("play-lh"))) {
                iconUrl
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun encodeParam(text: String): String {
        return text.trim().replace(" ", "+")
    }
}
