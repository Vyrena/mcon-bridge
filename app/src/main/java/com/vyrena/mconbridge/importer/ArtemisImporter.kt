package com.vyrena.mconbridge.importer

import android.content.ContentResolver
import android.net.Uri
import com.vyrena.mconbridge.data.BridgeRepository
import com.vyrena.mconbridge.data.GameEntryEntity
import com.vyrena.mconbridge.data.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ArtemisImporter(
    private val resolver: ContentResolver,
    private val repository: BridgeRepository,
) {
    suspend fun import(uris: List<Uri>): Result<List<GameEntryEntity>> = runCatching {
        require(uris.isNotEmpty()) { "Choose at least one Artemis .art file" }
        uris.map { uri ->
            val parsed = withContext(Dispatchers.IO) {
                val text = resolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: error("Unable to read $uri")
                ArtemisArtParser.parse(text)
            }
            repository.upsertImported(
                title = parsed.title,
                source = SourceType.ARTEMIS,
                sourceKey = parsed.sourceKey,
                payload = parsed.payload,
            )
        }
    }
}
