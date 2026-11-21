package com.solarad.app.data

/**
 * Small file-backed store for large cache blobs (report history) that don't belong in
 * SharedPreferences / NSUserDefaults (which keep the whole store in memory and rewrite it on every change).
 * Writes are atomic (temp file + rename) so a crash mid-write never leaves a corrupt cache.
 */
interface CacheFileStore {
    fun read(name: String): String?
    fun exists(name: String): Boolean
    fun write(name: String, content: String): Boolean
    fun delete(name: String)
}

/** Platform default; returns null when no writable location is available (falls back to Settings). */
expect fun createCacheFileStore(): CacheFileStore?
