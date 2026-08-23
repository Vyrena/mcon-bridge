package com.vyrena.mconbridge.importer

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.vyrena.mconbridge.data.BridgeRepository
import com.vyrena.mconbridge.data.GameEntryEntity
import com.vyrena.mconbridge.data.SourceType
import com.vyrena.mconbridge.domain.KirinPayload
import com.vyrena.mconbridge.launch.KirinPathPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class KirinScanner(
    private val context: Context,
    private val repository: BridgeRepository,
) {
    suspend fun scan(treeUri: Uri): Result<List<GameEntryEntity>> = runCatching {
        val rootPath = StoragePathMapper.primaryTreePath(context, treeUri)
            ?: error("Kirin scanning currently supports primary shared storage")
        val selectedRoot = KirinPathPolicy.canonicalSelectedRoot(rootPath)
            ?: error("Choose a folder inside internal shared storage, not the storage root")
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: error("Unable to open the selected Kirin folder")
        val candidates = withContext(Dispatchers.IO) {
            val rootChildren = root.listFiles()
            val selectedGame = candidateFor(
                directory = root,
                children = rootChildren,
                path = selectedRoot,
                fallbackTitle = File(selectedRoot).name,
            )
            if (selectedGame != null) {
                listOf(selectedGame)
            } else {
                rootChildren.filter { it.isDirectory }.mapNotNull { directory ->
                    val folderName = directory.name?.trim().orEmpty().ifEmpty { return@mapNotNull null }
                    candidateFor(
                        directory = directory,
                        children = directory.listFiles(),
                        path = "$selectedRoot/$folderName",
                        fallbackTitle = folderName,
                    )
                }
            }
        }
        candidates.map { (title, rawPath, artworkUri) ->
            val canonicalPath = KirinPathPolicy.canonicalGamePath(rawPath, selectedRoot)
                ?: error("Unsafe Kirin path: $rawPath")
            repository.upsertImported(
                title = title,
                source = SourceType.KIRIN,
                sourceKey = canonicalPath,
                payload = KirinPayload(canonicalPath, selectedRoot),
                artworkUri = artworkUri,
            )
        }
    }

    private fun candidateFor(
        directory: DocumentFile,
        children: Array<DocumentFile>,
        path: String,
        fallbackTitle: String,
    ): Triple<String, String, String?>? {
        val hasMarker = children.any { file ->
            val name = file.name.orEmpty()
            file.isFile && (
                name.equals("Game.ini", ignoreCase = true) ||
                    name.equals("Game.exe", ignoreCase = true) ||
                    name.endsWith(".rxproj", ignoreCase = true)
                )
        }
        if (!directory.isDirectory || !hasMarker) return null
        val title = readGameTitle(children) ?: fallbackTitle
        val art = children.firstOrNull { file ->
            file.isFile && file.name.orEmpty().lowercase() in ARTWORK_NAMES
        }?.uri?.toString()
        return Triple(title, path, art)
    }

    private fun readGameTitle(children: Array<DocumentFile>): String? {
        val ini = children.firstOrNull { it.isFile && it.name.equals("Game.ini", ignoreCase = true) } ?: return null
        return runCatching {
            context.contentResolver.openInputStream(ini.uri)?.bufferedReader()?.useLines { lines ->
                lines.firstNotNullOfOrNull { line ->
                    line.substringAfter("Title=", "").trim().takeIf { it.isNotEmpty() && line.trim().startsWith("Title=") }
                }
            }
        }.getOrNull()
    }

    companion object {
        private val ARTWORK_NAMES = setOf(
            "cover.png", "cover.jpg", "cover.jpeg",
            "icon.png", "icon.jpg", "icon.jpeg",
            "title.png", "title.jpg", "title.jpeg",
        )
    }
}
