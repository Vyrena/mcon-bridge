package com.vyrena.mconbridge.artwork

import com.vyrena.mconbridge.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class GameTdbProvider(
    private val client: OkHttpClient,
    private val settings: SettingsRepository,
) : ArtworkProvider {
    override val name = "GameTDB"

    override suspend fun search(query: ArtworkQuery): List<ArtworkCandidate> = withContext(Dispatchers.IO) {
        val productCode = query.productCode?.let(::gameTdbIdFromProductCode)
            ?: return@withContext emptyList()
        val configuredRegions = settings.settings.first().gameTdbRegions
        val regions = listOfNotNull(query.region?.uppercase()) + configuredRegions
        regions.distinct().flatMap { region ->
            listOf("coverHQ", "cover").mapNotNull { kind ->
                val extension = "jpg"
                val imageUrl = "https://art.gametdb.com/3ds/$kind/$region/$productCode.$extension"
                val exists = runCatching {
                    client.newCall(Request.Builder().url(imageUrl).head().build()).execute().use {
                        it.isSuccessful && it.header("Content-Type").orEmpty().startsWith("image/")
                    }
                }.getOrDefault(false)
                if (!exists) return@mapNotNull null
                ArtworkCandidate(
                    id = "gametdb:$kind:$region:$productCode",
                    provider = name,
                    title = query.title,
                    imageUrl = imageUrl,
                    thumbnailUrl = imageUrl,
                    attribution = "Cover provided by GameTDB ($region)",
                    sourceUrl = "https://www.gametdb.com/3DS/$productCode",
                    confidence = 1f,
                )
            }
        }
    }
}

internal fun gameTdbIdFromProductCode(value: String): String? {
    val normalized = value.trim().uppercase()
    if (normalized.matches(Regex("^[A-Z0-9]{4}$"))) return normalized
    return Regex("^CTR-[A-Z0-9]-([A-Z0-9]{4})$").matchEntire(normalized)?.groupValues?.get(1)
}
