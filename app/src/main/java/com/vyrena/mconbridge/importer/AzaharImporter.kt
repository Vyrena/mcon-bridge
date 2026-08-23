package com.vyrena.mconbridge.importer

import android.content.ContentResolver
import android.net.Uri
import com.vyrena.mconbridge.data.BridgeRepository
import com.vyrena.mconbridge.data.GameEntryEntity
import com.vyrena.mconbridge.data.SourceType
import com.vyrena.mconbridge.domain.AzaharPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AzaharExport(val schema: Int, val games: List<AzaharExportGame>)

@Serializable
data class AzaharExportGame(
    val title: String,
    val titleId: String,
    val productCode: String? = null,
    val region: String? = null,
    val artworkUri: String? = null,
)

class AzaharImporter(
    private val resolver: ContentResolver,
    private val repository: BridgeRepository,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun import(uri: Uri): Result<List<GameEntryEntity>> = runCatching {
        val export = withContext(Dispatchers.IO) {
            val text = resolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: error("Unable to read Azahar export")
            require(text.length <= 4 * 1024 * 1024) { "Azahar export is too large" }
            json.decodeFromString<AzaharExport>(text)
        }
        require(export.schema == 1) { "Unsupported Azahar export version ${export.schema}" }
        export.games.map { game ->
            val normalizedId = game.titleId.uppercase()
            require(normalizedId.matches(Regex("^[0-9A-F]{16}$"))) { "Invalid title ID for ${game.title}" }
            repository.upsertImported(
                title = game.title,
                source = SourceType.AZAHAR,
                sourceKey = normalizedId,
                payload = AzaharPayload(
                    titleId = normalizedId,
                    productCode = game.productCode?.uppercase(),
                    region = game.region?.uppercase(),
                ),
                artworkUri = game.artworkUri,
            )
        }
    }
}
