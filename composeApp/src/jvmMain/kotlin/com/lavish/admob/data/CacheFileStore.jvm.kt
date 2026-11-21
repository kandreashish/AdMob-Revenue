package com.lavish.admob.data

import java.io.File

actual fun createCacheFileStore(): CacheFileStore? = try {
    val home = System.getProperty("user.home") ?: return null
    JvmFileCacheStore(File(home, ".addash/cache"))
} catch (_: Throwable) { null }

internal class JvmFileCacheStore(private val dir: File) : CacheFileStore {
    init { dir.mkdirs() }
    override fun read(name: String): String? = try {
        File(dir, name).takeIf { it.isFile }?.readText(Charsets.UTF_8)
    } catch (_: Throwable) { null }

    override fun exists(name: String): Boolean = File(dir, name).isFile

    override fun write(name: String, content: String): Boolean = try {
        val tmp = File(dir, "$name.tmp")
        tmp.writeText(content, Charsets.UTF_8)
        java.nio.file.Files.move(
            tmp.toPath(), File(dir, name).toPath(),
            java.nio.file.StandardCopyOption.REPLACE_EXISTING,
            java.nio.file.StandardCopyOption.ATOMIC_MOVE
        )
        true
    } catch (_: Throwable) { false }

    override fun delete(name: String) {
        try { File(dir, name).delete(); File(dir, "$name.tmp").delete() } catch (_: Throwable) {}
    }
}
