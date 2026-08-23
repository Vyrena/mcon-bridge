package com.vyrena.mconbridge.importer

import com.vyrena.mconbridge.domain.ArtemisPayload

data class ParsedArtemisGame(val title: String, val sourceKey: String, val payload: ArtemisPayload)

object ArtemisArtParser {
    private val linePattern = Regex("^\\[([a-z_]+)]\\s+(.+)$")

    fun parse(text: String): ParsedArtemisGame {
        require(text.length <= 64 * 1024) { ".art file is too large" }
        val values = text.lineSequence()
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith('#') }
            .associate { line ->
                val match = linePattern.matchEntire(line) ?: error("Invalid .art line: $line")
                match.groupValues[1] to match.groupValues[2].trim()
            }
        val hostUuid = values["host_uuid"]?.takeIf { it.isNotBlank() } ?: error("Missing host_uuid")
        val appUuid = values["app_uuid"]?.takeIf { it.isNotBlank() }
        val appId = values["app_id"]?.takeIf { it.isNotBlank() }
        val appName = values["app_name"]?.takeIf { it.isNotBlank() }
        require(appUuid != null || appId != null || appName != null) { "Missing Artemis app identifier" }
        val identity = appUuid ?: appId ?: appName!!.lowercase()
        val title = appName ?: "Artemis game $identity"
        return ParsedArtemisGame(
            title = title,
            sourceKey = "$hostUuid:$identity",
            payload = ArtemisPayload(
                hostUuid = hostUuid,
                hostName = values["host_name"],
                appUuid = appUuid,
                appName = appName,
                appId = appId,
            ),
        )
    }
}
