package com.vyrena.mconbridge.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.vyrena.mconbridge.data.BridgeRepository
import com.vyrena.mconbridge.data.GameEntryEntity
import com.vyrena.mconbridge.domain.BridgeLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class MconLibraryExport(
    val schema: String = "com.vyrena.mconbridge.library/1",
    val generatedAt: Long,
    val sourceApp: String = "MCON Bridge",
    val games: List<MconExportGame>,
)

@Serializable
data class MconExportGame(
    val id: String,
    val title: String,
    val launchUrl: String,
    val artworkUri: String? = null,
    val artworkAttribution: String? = null,
)

data class MconExportIntent(val intent: Intent, val directToMcon: Boolean, val gameCount: Int)

class MconExportManager(
    private val context: Context,
    private val repository: BridgeRepository,
) {
    private val json = Json { prettyPrint = true; encodeDefaults = true }

    fun copyLink(game: GameEntryEntity) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText(game.title, BridgeLink.build(game.id)))
    }

    suspend fun buildLibraryIntent(selectedIds: Set<String>? = null): Result<MconExportIntent> = runCatching {
        val games = repository.getGames().filter { selectedIds == null || it.id in selectedIds }
        require(games.isNotEmpty()) { "There are no games to export" }
        val shareableArtwork = games.associateWith { toShareableArtworkUri(it.artworkUri) }
        val export = MconLibraryExport(
            generatedAt = System.currentTimeMillis(),
            games = games.map { game ->
                MconExportGame(
                    id = game.id,
                    title = game.title,
                    launchUrl = BridgeLink.build(game.id),
                    artworkUri = shareableArtwork[game]?.toString(),
                    artworkAttribution = game.artworkAttribution,
                )
            },
        )
        val exportUri = withContext(Dispatchers.IO) {
            val directory = File(context.filesDir, "exports").apply { mkdirs() }
            val file = File(directory, "mcon-bridge-library.json")
            file.writeText(json.encodeToString(MconLibraryExport.serializer(), export), Charsets.UTF_8)
            FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        }
        val clipData = ClipData.newUri(context.contentResolver, "MCON Bridge library", exportUri).apply {
            shareableArtwork.values.filterNotNull().distinct().forEach { addItem(ClipData.Item(it)) }
        }
        val baseIntent = Intent(Intent.ACTION_SEND).apply {
            type = MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, exportUri)
            putExtra(Intent.EXTRA_TITLE, "MCON Bridge library")
            putExtra(Intent.EXTRA_TEXT, "Import ${games.size} game${if (games.size == 1) "" else "s"} from MCON Bridge")
            this.clipData = clipData
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val directIntent = Intent(baseIntent).setPackage(MCON_PACKAGE)
        val supportsDirectImport = directIntent.resolveActivity(context.packageManager) != null
        MconExportIntent(
            intent = if (supportsDirectImport) directIntent else Intent.createChooser(baseIntent, "Export game library"),
            directToMcon = supportsDirectImport,
            gameCount = games.size,
        )
    }

    private fun toShareableArtworkUri(value: String?): Uri? {
        val uri = value?.toUri() ?: return null
        if (uri.scheme == "content") return uri
        if (uri.scheme != "file") return null
        val file = File(uri.path ?: return null)
        val artworkRoot = File(context.filesDir, "artwork").canonicalFile
        val canonical = runCatching { file.canonicalFile }.getOrNull() ?: return null
        if (!canonical.path.startsWith(artworkRoot.path + File.separator)) return null
        return FileProvider.getUriForFile(context, "${context.packageName}.files", canonical)
    }

    companion object {
        const val MCON_PACKAGE = "com.ohsnap.mconutilities"
        const val MIME_TYPE = "application/vnd.vyrena.mconbridge.library+json"
    }
}
