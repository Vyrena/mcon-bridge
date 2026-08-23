package com.vyrena.mconbridge.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<GameEntryEntity>>

    @Query("SELECT * FROM games ORDER BY title COLLATE NOCASE")
    suspend fun getAll(): List<GameEntryEntity>

    @Query("SELECT * FROM games WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): GameEntryEntity?

    @Query("SELECT * FROM games WHERE source = :source AND sourceKey = :sourceKey LIMIT 1")
    suspend fun getBySourceKey(source: SourceType, sourceKey: String): GameEntryEntity?

    @Upsert
    suspend fun upsert(game: GameEntryEntity)

    @Upsert
    suspend fun upsertAll(games: List<GameEntryEntity>)

    @Delete
    suspend fun delete(game: GameEntryEntity)

    @Query("DELETE FROM games")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(games: List<GameEntryEntity>) {
        deleteAll()
        upsertAll(games)
    }
}
