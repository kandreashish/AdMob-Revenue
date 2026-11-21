package com.solarad.app.data

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class IosFileCacheStore(private val dir: String) : CacheFileStore {
    private fun path(name: String) = "$dir/$name"

    override fun read(name: String): String? =
        NSString.stringWithContentsOfFile(path(name), NSUTF8StringEncoding, null)

    override fun exists(name: String): Boolean =
        NSFileManager.defaultManager.fileExistsAtPath(path(name))

    override fun write(name: String, content: String): Boolean =
        // atomically = true → Foundation writes to a temp file and renames
        NSString.create(string = content).writeToFile(path(name), true, NSUTF8StringEncoding, null)

    override fun delete(name: String) {
        NSFileManager.defaultManager.removeItemAtPath(path(name), null)
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun createCacheFileStore(): CacheFileStore? {
    val base = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
        .firstOrNull() as? String ?: return null
    val dir = "$base/addash_cache"
    NSFileManager.defaultManager.createDirectoryAtPath(dir, true, null, null)
    return IosFileCacheStore(dir)
}
