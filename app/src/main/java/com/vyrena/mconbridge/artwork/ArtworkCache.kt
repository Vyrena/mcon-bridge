package com.vyrena.mconbridge.artwork

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FilterOutputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

class ArtworkCache(
    context: Context,
    private val client: OkHttpClient,
) {
    private val resolver: ContentResolver = context.contentResolver
    private val directory = File(context.filesDir, "artwork").apply { mkdirs() }

    suspend fun cache(candidate: ArtworkCandidate): CachedArtwork = withContext(Dispatchers.IO) {
        require(candidate.imageUrl.startsWith("https://")) { "Artwork downloads require HTTPS" }
        client.newCall(Request.Builder().url(candidate.imageUrl).build()).execute().use { response ->
            if (!response.isSuccessful) error("Artwork download failed (${response.code})")
            val contentType = response.header("Content-Type").orEmpty().substringBefore(';')
            require(contentType in SUPPORTED_MIME_TYPES) { "Unsupported artwork type: $contentType" }
            val length = response.body?.contentLength() ?: -1
            require(length <= MAX_BYTES) { "Artwork is larger than 10 MB" }
            val stream = response.body?.byteStream() ?: error("Artwork download returned no image")
            val file = storeVerified(stream, contentType)
            CachedArtwork(file.toUri().toString(), candidate.provider, candidate.attribution, candidate.sourceUrl)
        }
    }

    suspend fun cacheLocal(uri: Uri): CachedArtwork = withContext(Dispatchers.IO) {
        val contentType = resolver.getType(uri)?.substringBefore(';') ?: "image/jpeg"
        require(contentType in SUPPORTED_MIME_TYPES) { "Choose a PNG, JPEG, WebP, or GIF image" }
        val stream = resolver.openInputStream(uri) ?: error("Unable to read selected image")
        val file = stream.use { storeVerified(it, contentType) }
        CachedArtwork(file.toUri().toString(), "Local", "User-selected artwork", null)
    }

    suspend fun saveToDevice(cachedUri: String, destination: Uri): Long = withContext(Dispatchers.IO) {
        val sourceUri = cachedUri.toUri()
        require(sourceUri.scheme == ContentResolver.SCHEME_FILE) { "Artwork is not available in the verified cache" }
        val source = File(sourceUri.path ?: error("Artwork cache path is missing")).canonicalFile
        require(source.path.startsWith(directory.canonicalPath + File.separator) && source.isFile) {
            "Artwork is missing from the verified cache"
        }
        require(source.length() in 1..MAX_BYTES) { "Artwork file size is invalid" }

        val bitmap = decodeForExport(source)
        var bytesWritten = 0L
        try {
            val output = resolver.openOutputStream(destination, "wt") ?: error("Unable to create the artwork file")
            object : FilterOutputStream(output) {
                override fun write(value: Int) {
                    out.write(value)
                    bytesWritten++
                }

                override fun write(buffer: ByteArray, offset: Int, length: Int) {
                    out.write(buffer, offset, length)
                    bytesWritten += length
                }
            }.use { destinationStream ->
                require(bitmap.compress(Bitmap.CompressFormat.PNG, 100, destinationStream)) {
                    "Unable to encode the artwork as PNG"
                }
            }
        } finally {
            bitmap.recycle()
        }

        val exportedBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(destination)?.use { BitmapFactory.decodeStream(it, null, exportedBounds) }
            ?: error("Unable to verify the saved artwork")
        require(exportedBounds.outWidth > 0 && exportedBounds.outHeight > 0 && exportedBounds.outMimeType == "image/png") {
            "Saved artwork could not be verified as PNG"
        }
        require(bytesWritten > 0) { "Saved artwork is empty" }
        bytesWritten
    }

    suspend fun prune(limitMb: Int, protectedUris: Set<String>): Long = withContext(Dispatchers.IO) {
        val limitBytes = limitMb.coerceIn(32, 2048).toLong() * 1024 * 1024
        val files = directory.listFiles().orEmpty().filter(File::isFile).sortedBy(File::lastModified)
        var total = files.sumOf(File::length)
        var removed = 0L
        files.forEach { file ->
            if (total <= limitBytes) return@forEach
            if (file.toUri().toString() in protectedUris) return@forEach
            val size = file.length()
            if (file.delete()) {
                total -= size
                removed += size
            }
        }
        removed
    }

    fun sizeBytes(): Long = directory.listFiles().orEmpty().sumOf(File::length)

    private fun decodeForExport(source: File): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.path, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Cached artwork could not be decoded" }

        var sampleSize = 1
        while (bounds.outWidth / sampleSize > MAX_EXPORT_DIMENSION ||
            bounds.outHeight / sampleSize > MAX_EXPORT_DIMENSION
        ) {
            sampleSize *= 2
        }
        return BitmapFactory.decodeFile(
            source.path,
            BitmapFactory.Options().apply { inSampleSize = sampleSize },
        ) ?: error("Cached artwork could not be decoded")
    }

    private fun storeVerified(input: InputStream, contentType: String): File {
        val temporary = File.createTempFile("incoming-", ".part", directory)
        val digest = MessageDigest.getInstance("SHA-256")
        try {
            FileOutputStream(temporary).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    require(total <= MAX_BYTES) { "Artwork is larger than 10 MB" }
                    digest.update(buffer, 0, read)
                    output.write(buffer, 0, read)
                }
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(temporary.path, bounds)
            require(bounds.outWidth in 1..MAX_DIMENSION && bounds.outHeight in 1..MAX_DIMENSION) {
                "Artwork dimensions are invalid or too large"
            }
            val extension = EXTENSIONS.getValue(contentType)
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            val destination = File(directory, "$hash.$extension")
            if (destination.exists()) temporary.delete()
            else require(temporary.renameTo(destination)) { "Unable to store artwork" }
            destination.setLastModified(System.currentTimeMillis())
            return destination
        } catch (error: Throwable) {
            temporary.delete()
            throw error
        }
    }

    companion object {
        private const val MAX_BYTES = 10L * 1024 * 1024
        private const val MAX_DIMENSION = 8192
        private const val MAX_EXPORT_DIMENSION = 2048
        private val EXTENSIONS = mapOf(
            "image/png" to "png",
            "image/jpeg" to "jpg",
            "image/webp" to "webp",
            "image/gif" to "gif",
        )
        private val SUPPORTED_MIME_TYPES = EXTENSIONS.keys
    }
}
