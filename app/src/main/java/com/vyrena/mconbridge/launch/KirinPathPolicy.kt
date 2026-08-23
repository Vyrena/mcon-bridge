package com.vyrena.mconbridge.launch

import java.io.File

object KirinPathPolicy {
    const val DEFAULT_GAMES_ROOT = "/storage/emulated/0/Kirin/games"

    fun canonicalGamePath(path: String, allowedRoot: String = DEFAULT_GAMES_ROOT): String? {
        if (path.isBlank() || path.indexOf('\u0000') >= 0 || !path.startsWith('/')) return null
        return runCatching {
            val root = File(allowedRoot).canonicalFile
            val candidate = File(path).canonicalFile
            candidate.path.takeIf { candidate.parentFile == root }
        }.getOrNull()
    }
}
