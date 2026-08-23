package com.vyrena.mconbridge.artwork

import com.vyrena.mconbridge.data.SourceType

data class ArtworkQuery(
    val title: String,
    val source: SourceType,
    val sourceId: String,
    val productCode: String? = null,
    val region: String? = null,
)

data class ArtworkCandidate(
    val id: String,
    val provider: String,
    val title: String,
    val imageUrl: String,
    val thumbnailUrl: String? = null,
    val attribution: String,
    val sourceUrl: String,
    val width: Int? = null,
    val height: Int? = null,
    val confidence: Float,
)

data class CachedArtwork(
    val uri: String,
    val provider: String,
    val attribution: String,
    val sourceUrl: String?,
)

interface ArtworkProvider {
    val name: String
    suspend fun search(query: ArtworkQuery): List<ArtworkCandidate>
}
