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

class KirinScanner(
    private val context: Context,
    private val repository: BridgeRepository,
) {
    suspend fun scan(treeUri: Uri): Result<List<GameEntryEntity>> = runCatching {
        val rootPath = StoragePathMapper.primaryTreePath(context, treeUri)
            ?: error("Kirin scanning currently supports primary shared storage")
        require(rootPath == KirinPathPolicy.DEFAULT_GAMES_ROOT) {
            "Choose the Kirin/games folder, not a parent or unrelated folder"
        }
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: error("Unable to open Kirin games folder")
        val candidates = withContext(Dispatchers.IO) {
            root.listFiles().filter { it.isDirectory }.mapNotNull { directory ->
                val children = directory.listFiles()
                val hasMarker = children.any { file ->
                    val name = file.name.orEmpty()
                    file.isFile && (
                        name.equals("Game.ini", ignoreCase = true) ||
                            name.equals("Game.exe", ignoreCase = true) ||
                            name.endsWith(".rxproj", ignoreCase = true)
                        )
                }
                if (!hasMarker) return@mapNotNull null
                val folderName = directory.name?.trim().orEmpty().ifEmpty { return@mapNotNull null }
                val title = readGameTitle(children) ?: folderName
                val art = children.firstOrNull { file ->
                    file.isFile && file.name.orEmpty().lowercase() in ARTWORK_NAMES
                }?.uri?.toString()
                Triple(title, "$rootPath/$folderName", art)
            }
        }
        candidates.map { (title, rawPath, artworkUri) ->
            val canonicalPath = KirinPathPolicy.canonicalGamePath(rawPath) ?: error("Unsafe Kirin path: $rawPath")
            repository.upsertImported(
                title = title,
                source = SourceType.KIRIN,
                sourceKey = canonicalPath,
                payload = KirinPayload(canonicalPath),
                artworkUri = artworkUri,
            )
        }
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
