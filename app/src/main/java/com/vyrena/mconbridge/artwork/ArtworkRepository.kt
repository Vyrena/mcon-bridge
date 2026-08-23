package com.vyrena.mconbridge.artwork

import android.net.Uri
import com.vyrena.mconbridge.data.BridgeRepository
import com.vyrena.mconbridge.data.GameEntryEntity
import com.vyrena.mconbridge.domain.AzaharPayload
import com.vyrena.mconbridge.domain.LaunchPayloadCodec
import com.vyrena.mconbridge.settings.SettingsRepository
import kotlinx.coroutines.flow.first

class ArtworkRepository(
    private val bridgeRepository: BridgeRepository,
    private val settings: SettingsRepository,
    private val networkPolicy: NetworkPolicy,
    private val providers: List<ArtworkProvider>,
    private val cache: ArtworkCache,
) {
    suspend fun search(game: GameEntryEntity): Result<List<ArtworkCandidate>> = runCatching {
        val currentSettings = settings.settings.first()
        require(networkPolicy.canDownload(currentSettings.wifiOnly)) {
            if (currentSettings.wifiOnly) "Connect to Wi-Fi or disable Wi-Fi-only artwork downloads" else "No network connection"
        }
        val payload = LaunchPayloadCodec.decode(game.launchPayload)
        val query = ArtworkQuery(
            title = game.title,
            source = game.source,
            sourceId = game.sourceKey,
            productCode = (payload as? AzaharPayload)?.productCode,
            region = (payload as? AzaharPayload)?.region,
        )
        providers.flatMap { provider -> runCatching { provider.search(query) }.getOrDefault(emptyList()) }
            .distinctBy(ArtworkCandidate::imageUrl)
            .sortedWith(compareByDescending<ArtworkCandidate> { it.confidence }.thenByDescending { it.height ?: 0 })
    }

    suspend fun apply(game: GameEntryEntity, candidate: ArtworkCandidate): Result<GameEntryEntity> = runCatching {
        val currentSettings = settings.settings.first()
        require(networkPolicy.canDownload(currentSettings.wifiOnly)) { "Artwork download is blocked by the network setting" }
        val cached = cache.cache(candidate)
        val updated = game.copy(
            artworkUri = cached.uri,
            artworkProvider = cached.provider,
            artworkAttribution = cached.attribution,
            artworkSourceUrl = cached.sourceUrl,
        )
        bridgeRepository.update(updated)
        prune(currentSettings.artworkCacheMb)
        updated
    }

    suspend fun applyLocal(game: GameEntryEntity, uri: Uri): Result<GameEntryEntity> = runCatching {
        val cached = cache.cacheLocal(uri)
        val updated = game.copy(
            artworkUri = cached.uri,
            artworkProvider = cached.provider,
            artworkAttribution = cached.attribution,
            artworkSourceUrl = null,
        )
        bridgeRepository.update(updated)
        prune(settings.settings.first().artworkCacheMb)
        updated
    }

    suspend fun prune(limitMb: Int): Long {
        val protectedUris = bridgeRepository.getGames().mapNotNull(GameEntryEntity::artworkUri).toSet()
        return cache.prune(limitMb, protectedUris)
    }

    fun cacheSizeBytes(): Long = cache.sizeBytes()
}
