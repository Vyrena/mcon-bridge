package com.vyrena.mconbridge.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "games",
    indices = [Index(value = ["source", "sourceKey"], unique = true)],
)
data class GameEntryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val source: SourceType,
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
