package com.vyrena.mconbridge.domain

import android.net.Uri
import java.net.URI
import java.util.UUID

object BridgeLink {
    const val SCHEME = "mconbridge"
    const val HOST = "launch"

    fun build(id: String): String {
        require(isUuid(id)) { "Game ID must be a UUID" }
        return "$SCHEME://$HOST/$id"
    }

    fun parse(uri: Uri?): String? = uri?.toString()?.let(::parse)

    fun parse(raw: String): String? = runCatching {
        val uri = URI(raw)
        if (uri.scheme != SCHEME || uri.host != HOST || uri.query != null || uri.fragment != null) return null
        val segments = uri.path.orEmpty().split('/').filter(String::isNotEmpty)
        if (segments.size != 1) return null
        segments.single().takeIf(::isUuid)
    }.getOrNull()

    private fun isUuid(value: String): Boolean = runCatching { UUID.fromString(value) }.isSuccess
}
