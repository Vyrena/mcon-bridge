package com.vyrena.mconbridge.data

import com.vyrena.mconbridge.domain.LaunchPayload
import com.vyrena.mconbridge.domain.LaunchPayloadCodec
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class BridgeRepository(private val dao: GameDao) {
    fun observeGames(): Flow<List<GameEntryEntity>> = dao.observeAll()

    suspend fun getGames(): List<GameEntryEntity> = dao.getAll()

    suspend fun getGame(id: String): GameEntryEntity? = dao.getById(id)

    suspend fun upsertImported(
        title: String,
        source: SourceType,
        sourceKey: String,
        payload: LaunchPayload,
        artworkUri: String? = null,
    ): GameEntryEntity {
        val now = System.currentTimeMillis()
        val existing = dao.getBySourceKey(source, sourceKey)
        val entity = GameEntryEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            title = title.trim().ifEmpty { "Untitled game" },
            source = source,
            sourceKey = sourceKey,
            launchPayload = LaunchPayloadCodec.encode(payload),
            artworkUri = artworkUri ?: existing?.artworkUri,
            artworkProvider = existing?.artworkProvider,
            artworkAttribution = existing?.artworkAttribution,
            artworkSourceUrl = existing?.artworkSourceUrl,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            lastValidatedAt = existing?.lastValidatedAt,
            enabled = true,
        )
        dao.upsert(entity)
        return entity
    }

    suspend fun update(entity: GameEntryEntity) = dao.upsert(entity.copy(updatedAt = System.currentTimeMillis()))

    suspend fun delete(entity: GameEntryEntity) = dao.delete(entity)

    suspend fun replaceAll(entities: List<GameEntryEntity>) {
        dao.replaceAll(entities)
    }
}
