package com.vyrena.mconbridge.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.core.net.toUri
import com.vyrena.mconbridge.data.BridgeRepository
import com.vyrena.mconbridge.data.GameEntryEntity
import com.vyrena.mconbridge.data.SourceType
import com.vyrena.mconbridge.domain.ArtemisPayload
import com.vyrena.mconbridge.domain.AzaharPayload
import com.vyrena.mconbridge.domain.KirinPayload
import com.vyrena.mconbridge.domain.LaunchPayloadCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable
data class BridgeBackupFile(val schema: Int = 1, val exportedAt: Long, val games: List<BackupGame>)

@Serializable
data class BackupGame(
    val id: String,
    val title: String,
    val source: String,
    val sourceKey: String,
    val launchPayload: String,
    val artworkUri: String? = null,
    val artworkProvider: String? = null,
    val artworkAttribution: String? = null,
    val artworkSourceUrl: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val lastValidatedAt: Long? = null,
    val enabled: Boolean = true,
)

class BridgeBackupManager(
    private val resolver: ContentResolver,
    private val repository: BridgeRepository,
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun export(uri: Uri): Result<Int> = runCatching {
        val games = repository.getGames()
        val file = BridgeBackupFile(
            exportedAt = System.currentTimeMillis(),
            games = games.map(GameEntryEntity::toBackup),
        )
        withContext(Dispatchers.IO) {
            resolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use {
                it.write(json.encodeToString(BridgeBackupFile.serializer(), file))
            } ?: error("Unable to create bridge backup")
        }
        games.size
    }

    suspend fun restore(uri: Uri): Result<Int> = runCatching {
        val file = withContext(Dispatchers.IO) {
            val text = resolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: error("Unable to read bridge backup")
            require(text.length <= 16 * 1024 * 1024) { "Bridge backup is too large" }
            json.decodeFromString(BridgeBackupFile.serializer(), text)
        }
        require(file.schema == 1) { "Unsupported bridge backup version ${file.schema}" }
        require(file.games.size <= 10_000) { "Bridge backup contains too many games" }
        val games = file.games.map { it.toEntity().withReadableArtwork() }
        require(games.distinctBy(GameEntryEntity::id).size == games.size) { "Bridge backup contains duplicate game IDs" }
        require(games.distinctBy { it.source to it.sourceKey }.size == games.size) {
            "Bridge backup contains duplicate emulator entries"
        }
        repository.replaceAll(games)
        games.size
    }

    private fun GameEntryEntity.withReadableArtwork(): GameEntryEntity {
        val value = artworkUri ?: return this
        val uri = value.toUri()
        val readable = runCatching {
            when (uri.scheme) {
                "file" -> uri.path?.let(::File)?.isFile == true
                "content" -> resolver.openFileDescriptor(uri, "r")?.use { true } ?: false
                else -> false
            }
        }.getOrDefault(false)
        return if (readable) this else copy(artworkUri = null)
    }
}

private fun GameEntryEntity.toBackup() = BackupGame(
    id, title, source.name, sourceKey, launchPayload, artworkUri, artworkProvider,
    artworkAttribution, artworkSourceUrl, createdAt, updatedAt, lastValidatedAt, enabled,
)

private fun BackupGame.toEntity(): GameEntryEntity {
    require(runCatching { UUID.fromString(id) }.isSuccess) { "Bridge backup contains an invalid game ID" }
    require(title.isNotBlank() && title.length <= 512) { "Bridge backup contains an invalid title" }
    require(sourceKey.isNotBlank() && sourceKey.length <= 1024) { "Bridge backup contains an invalid source key" }
    val parsedSource = SourceType.valueOf(source)
    val payload = LaunchPayloadCodec.decode(launchPayload)
    require(
        (parsedSource == SourceType.AZAHAR && payload is AzaharPayload) ||
            (parsedSource == SourceType.ARTEMIS && payload is ArtemisPayload) ||
            (parsedSource == SourceType.KIRIN && payload is KirinPayload),
    ) { "Bridge backup source does not match its launch data" }
    return GameEntryEntity(
        id, title, parsedSource, sourceKey, launchPayload, artworkUri,
        artworkProvider, artworkAttribution, artworkSourceUrl, createdAt, updatedAt,
        lastValidatedAt, enabled,
    )
}
