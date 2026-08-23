package com.vyrena.mconbridge.launch

import java.io.File

object KirinPathPolicy {
    const val PRIMARY_STORAGE_ROOT = "/storage/emulated/0"
    const val DEFAULT_GAMES_ROOT = "/storage/emulated/0/Kirin/games"

    fun canonicalSelectedRoot(path: String): String? {
        if (path.isBlank() || path.indexOf('\u0000') >= 0 || !path.startsWith('/')) return null
        return runCatching {
            val storageRoot = File(PRIMARY_STORAGE_ROOT).canonicalFile
            val selectedRoot = File(path).canonicalFile
            selectedRoot.path.takeIf {
                it.startsWith(storageRoot.path + File.separator)
            }
        }.getOrNull()
    }

    fun canonicalGamePath(path: String, allowedRoot: String = DEFAULT_GAMES_ROOT): String? {
        if (path.isBlank() || path.indexOf('\u0000') >= 0 || !path.startsWith('/')) return null
        return runCatching {
            val root = canonicalSelectedRoot(allowedRoot)?.let(::File) ?: return@runCatching null
            val candidate = File(path).canonicalFile
            candidate.path.takeIf { candidate == root || candidate.parentFile == root }
        }.getOrNull()
    }
}
