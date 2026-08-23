package com.vyrena.mconbridge.importer

import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.vyrena.mconbridge.data.BridgeRepository
import com.vyrena.mconbridge.data.GameEntryEntity
import com.vyrena.mconbridge.data.SourceType
import com.vyrena.mconbridge.domain.AzaharPayload
import com.vyrena.mconbridge.domain.LaunchPayloadCodec
import com.vyrena.mconbridge.launch.AzaharLaunchAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest

class AzaharRomImporter(
    private val resolver: ContentResolver,
    private val repository: BridgeRepository,
) {
    suspend fun import(uris: List<Uri>): Result<List<GameEntryEntity>> = runCatching {
        require(uris.isNotEmpty()) { "Choose at least one Azahar ROM" }
        require(uris.size <= 500) { "Choose no more than 500 ROMs at once" }
        uris.map { uri -> importOne(uri) }
    }

    suspend fun link(game: GameEntryEntity, uri: Uri): Result<GameEntryEntity> = runCatching {
        require(game.source == SourceType.AZAHAR) { "Only Azahar entries can be linked to a ROM" }
        val existing = LaunchPayloadCodec.decode(game.launchPayload) as? AzaharPayload
            ?: error("Stored Azahar launch data is damaged")
        val rom = readRom(uri)
        val updated = game.copy(
            launchPayload = LaunchPayloadCodec.encode(
                existing.copy(
                    titleId = rom.titleId.takeUnless { it == ZERO_TITLE_ID } ?: existing.titleId,
                    productCode = rom.productCode ?: existing.productCode,
                    region = rom.region ?: existing.region,
                    gameUri = uri.toString(),
                    filename = rom.filename,
                    fileType = rom.extension,
                ),
            ),
        )
        repository.update(updated)
        updated
    }

    suspend fun refreshMetadata(game: GameEntryEntity): Result<GameEntryEntity> = runCatching {
        require(game.source == SourceType.AZAHAR) { "Only Azahar entries have ROM metadata" }
        val existing = LaunchPayloadCodec.decode(game.launchPayload) as? AzaharPayload
            ?: error("Stored Azahar launch data is damaged")
        if (!existing.productCode.isNullOrBlank()) return@runCatching game
        val uri = existing.gameUri?.toUri() ?: error("Choose the Azahar ROM again to refresh its metadata")
        val rom = readRom(uri)
        val updated = game.copy(
            launchPayload = LaunchPayloadCodec.encode(
                existing.copy(
                    titleId = rom.titleId.takeUnless { it == ZERO_TITLE_ID } ?: existing.titleId,
                    productCode = rom.productCode,
                    region = rom.region,
                    filename = rom.filename,
                    fileType = rom.extension,
                ),
            ),
        )
        repository.update(updated)
        updated
    }

    private suspend fun importOne(uri: Uri): GameEntryEntity {
        val rom = readRom(uri)
        val sourceKey = if (rom.titleId != ZERO_TITLE_ID) rom.titleId else "uri:${sha256(uri.toString())}"
        val title = rom.filename.substringBeforeLast('.').ifBlank { rom.filename }
        return repository.upsertImported(
            title = title,
            source = SourceType.AZAHAR,
            sourceKey = sourceKey,
            payload = AzaharPayload(
                titleId = rom.titleId,
                productCode = rom.productCode,
                region = rom.region,
                gameUri = uri.toString(),
                filename = rom.filename,
                fileType = rom.extension,
            ),
        )
    }

    private suspend fun readRom(uri: Uri): RomMetadata {
        require(uri.scheme == ContentResolver.SCHEME_CONTENT) { "Choose ROMs with Android's file picker" }
        runCatching {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val filename = queryDisplayName(uri)?.trim()?.takeIf(String::isNotEmpty)
            ?: error("A selected Azahar ROM has no filename")
        val extension = filename.substringAfterLast('.', "").lowercase()
        require(extension in AzaharLaunchAdapter.SUPPORTED_EXTENSIONS) {
            "$filename is not a playable Azahar ROM"
        }
        val header = withContext(Dispatchers.IO) {
            resolver.openInputStream(uri)?.use(AzaharTitleIdReader::readMetadata)
        }
        return RomMetadata(
            filename = filename,
            extension = extension,
            titleId = header?.titleId ?: ZERO_TITLE_ID,
            productCode = header?.productCode,
            region = header?.productCode?.let(::gameTdbRegionFromProductCode),
        )
    }

    private fun queryDisplayName(uri: Uri): String? = resolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null,
    )?.use { cursor: Cursor ->
        if (!cursor.moveToFirst()) null else cursor.getString(0)
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    companion object {
        const val ZERO_TITLE_ID = "0000000000000000"
    }

    private data class RomMetadata(
        val filename: String,
        val extension: String,
        val titleId: String,
        val productCode: String?,
        val region: String?,
    )
}

data class AzaharHeaderMetadata(val titleId: String, val productCode: String?)

object AzaharTitleIdReader {
    private const val HEADER_SIZE = 0x200
    private const val MEDIA_UNIT = 0x200L

    fun read(input: InputStream): String? = readMetadata(input)?.titleId

    fun readMetadata(input: InputStream): AzaharHeaderMetadata? {
        val firstHeader = input.readExactly(HEADER_SIZE) ?: return null
        return when (firstHeader.magicAt(0x100)) {
            "NCCH" -> firstHeader.metadata()
            "NCSD" -> {
                val partitionOffset = firstHeader.uint32LittleEndian(0x120) * MEDIA_UNIT
                if (partitionOffset < HEADER_SIZE || partitionOffset > 64L * 1024L * 1024L) return null
                if (!input.skipExactly(partitionOffset - HEADER_SIZE)) return null
                val partitionHeader = input.readExactly(HEADER_SIZE) ?: return null
                if (partitionHeader.magicAt(0x100) == "NCCH") partitionHeader.metadata() else null
            }
            else -> null
        }
    }

    private fun ByteArray.metadata() = AzaharHeaderMetadata(
        titleId = programId(),
        productCode = productCode(),
    )

    private fun ByteArray.programId(): String = (0 until 8)
        .map { this[0x118 + it] }
        .reversed()
        .joinToString("") { "%02X".format(it.toInt() and 0xFF) }

    private fun ByteArray.productCode(): String? {
        val bytes = copyOfRange(0x150, 0x160)
        val length = bytes.indexOf(0).takeIf { it >= 0 } ?: bytes.size
        if (length == 0 || bytes.take(length).any { (it.toInt() and 0xFF) !in 0x20..0x7E }) return null
        return String(bytes, 0, length, Charsets.US_ASCII)
            .trim()
            .uppercase()
            .takeIf { it.matches(Regex("^(CTR|KTR)-[A-Z0-9]-[A-Z0-9]{4}$")) }
    }

    private fun ByteArray.magicAt(offset: Int): String =
        String(this, offset, 4, Charsets.US_ASCII)

    private fun ByteArray.uint32LittleEndian(offset: Int): Long =
        (this[offset].toLong() and 0xFF) or
            ((this[offset + 1].toLong() and 0xFF) shl 8) or
            ((this[offset + 2].toLong() and 0xFF) shl 16) or
            ((this[offset + 3].toLong() and 0xFF) shl 24)

    private fun InputStream.readExactly(size: Int): ByteArray? {
        val buffer = ByteArray(size)
        var read = 0
        while (read < size) {
            val count = read(buffer, read, size - read)
            if (count < 0) return null
            if (count > 0) {
                read += count
            } else {
                val byte = read()
                if (byte < 0) return null
                buffer[read++] = byte.toByte()
            }
        }
        return buffer
    }

    private fun InputStream.skipExactly(size: Long): Boolean {
        var remaining = size
        while (remaining > 0) {
            val skipped = skip(remaining)
            if (skipped > 0) {
                remaining -= skipped
            } else if (read() >= 0) {
                remaining--
            } else {
                return false
            }
        }
        return true
    }
}

internal fun gameTdbRegionFromProductCode(productCode: String): String? = when (productCode.trim().uppercase().lastOrNull()) {
    'E' -> "US"
    'P' -> "EN"
    'J' -> "JA"
    'K' -> "KO"
    else -> null
}
