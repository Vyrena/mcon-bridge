package com.vyrena.mconbridge.backup

import android.content.ContentResolver
import android.net.Uri
import com.vyrena.mconbridge.data.BridgeRepository
import com.vyrena.mconbridge.data.GameEntryEntity
import com.vyrena.mconbridge.data.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
        val games = file.games.map(BackupGame::toEntity)
        repository.replaceAll(games)
        games.size
    }
}

private fun GameEntryEntity.toBackup() = BackupGame(
    id, title, source.name, sourceKey, launchPayload, artworkUri, artworkProvider,
    artworkAttribution, artworkSourceUrl, createdAt, updatedAt, lastValidatedAt, enabled,
)

private fun BackupGame.toEntity() = GameEntryEntity(
    id, title, SourceType.valueOf(source), sourceKey, launchPayload, artworkUri,
    artworkProvider, artworkAttribution, artworkSourceUrl, createdAt, updatedAt,
    lastValidatedAt, enabled,
)
