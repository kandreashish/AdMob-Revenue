package com.solarad.app.data

import java.io.File

internal class JavaFileCacheStore(private val dir: File) : CacheFileStore {
    init { dir.mkdirs() }
    override fun read(name: String): String? = try {
        File(dir, name).takeIf { it.isFile }?.readText(Charsets.UTF_8)
    } catch (_: Throwable) { null }

    override fun exists(name: String): Boolean = File(dir, name).isFile

    override fun write(name: String, content: String): Boolean = try {
        val tmp = File(dir, "$name.tmp")
        tmp.writeText(content, Charsets.UTF_8)
        val target = File(dir, name)
        if (!tmp.renameTo(target)) {
            target.delete()
            tmp.renameTo(target)
        } else true
    } catch (_: Throwable) { false }

    override fun delete(name: String) {
        try { File(dir, name).delete(); File(dir, "$name.tmp").delete() } catch (_: Throwable) {}
    }
}

actual fun createCacheFileStore(): CacheFileStore? =
    androidAppContext()?.let { JavaFileCacheStore(File(it.filesDir, "addash_cache")) }
