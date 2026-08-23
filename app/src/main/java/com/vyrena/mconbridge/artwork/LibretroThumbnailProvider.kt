package com.vyrena.mconbridge.artwork

import com.vyrena.mconbridge.data.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.Normalizer

class LibretroThumbnailProvider(
    private val client: OkHttpClient,
) : ArtworkProvider {
    override val name = "Libretro Thumbnails"

    private val catalogMutex = Mutex()
    @Volatile private var catalogCache: CatalogCache? = null

    override suspend fun search(query: ArtworkQuery): List<ArtworkCandidate> = withContext(Dispatchers.IO) {
        if (query.source != SourceType.AZAHAR) return@withContext emptyList()

        val exactLabel = libretroSafeFilename(query.title)
        val exactUrl = urlForLabel(exactLabel)
        if (regionIsCompatible(query, exactLabel) && isImage(exactUrl)) {
            return@withContext listOf(candidate(exactLabel, exactUrl, 1f))
        }

        matchLibretroCatalog(query, catalog()).map { match ->
            candidate(match.entry.label, match.entry.url, match.confidence)
        }
    }

    private fun candidate(label: String, url: HttpUrl, confidence: Float) = ArtworkCandidate(
        id = "libretro:${url.encodedPath}",
        provider = name,
        title = label,
        imageUrl = url.toString(),
        thumbnailUrl = url.toString(),
        attribution = "Box art via Libretro Thumbnails; artwork belongs to its publisher",
        sourceUrl = REPOSITORY_URL,
        confidence = confidence,
    )

    private fun isImage(url: HttpUrl): Boolean = runCatching {
        client.newCall(Request.Builder().url(url).head().build()).execute().use { response ->
            response.isSuccessful && response.header("Content-Type").orEmpty().startsWith("image/")
        }
    }.getOrDefault(false)

    private suspend fun catalog(): List<LibretroCatalogEntry> {
        val now = System.currentTimeMillis()
        catalogCache?.takeIf { now - it.loadedAt < CATALOG_TTL_MS }?.let { return it.entries }
        return catalogMutex.withLock {
            catalogCache?.takeIf { now - it.loadedAt < CATALOG_TTL_MS }?.entries ?: run {
                val entries = fetchCatalog()
                catalogCache = CatalogCache(now, entries)
                entries
            }
        }
    }

    private fun fetchCatalog(): List<LibretroCatalogEntry> {
        val request = Request.Builder().url(CATALOG_URL).get().build()
        val html = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Libretro catalog request failed (${response.code})")
            val contentType = response.header("Content-Type").orEmpty().substringBefore(';')
            require(contentType == "text/html") { "Libretro returned an unexpected catalog type" }
            response.body?.byteStream()?.use { it.readUtf8Limited(MAX_CATALOG_BYTES) }
                ?: error("Libretro returned no catalog")
        }
        return parseLibretroCatalog(html, CATALOG_URL)
            .takeIf(List<LibretroCatalogEntry>::isNotEmpty)
            ?: error("Libretro returned an empty box-art catalog")
    }

    private fun urlForLabel(label: String): HttpUrl = CATALOG_URL.newBuilder()
        .addPathSegment("$label.png")
        .build()

    private data class CatalogCache(val loadedAt: Long, val entries: List<LibretroCatalogEntry>)

    companion object {
        internal val CATALOG_URL =
            "https://thumbnails.libretro.com/Nintendo%20-%20Nintendo%203DS/Named_Boxarts/".toHttpUrl()
        private const val REPOSITORY_URL =
            "https://github.com/libretro-thumbnails/Nintendo_-_Nintendo_3DS"
        private const val MAX_CATALOG_BYTES = 2 * 1024 * 1024
        private const val CATALOG_TTL_MS = 24L * 60 * 60 * 1000
    }
}

internal data class LibretroCatalogEntry(val label: String, val url: HttpUrl)

internal data class LibretroCatalogMatch(
    val entry: LibretroCatalogEntry,
    val confidence: Float,
)

internal fun parseLibretroCatalog(html: String, catalogUrl: HttpUrl): List<LibretroCatalogEntry> =
    Regex("href=\\\"([^\\\"]+\\.png)\\\"", RegexOption.IGNORE_CASE)
        .findAll(html)
        .mapNotNull { match ->
            val url = catalogUrl.resolve(match.groupValues[1]) ?: return@mapNotNull null
            if (url.scheme != "https" || url.host != catalogUrl.host ||
                !url.encodedPath.startsWith(catalogUrl.encodedPath)
            ) {
                return@mapNotNull null
            }
            val filename = url.pathSegments.lastOrNull() ?: return@mapNotNull null
            val label = filename.removeSuffix(".png").takeIf(String::isNotBlank) ?: return@mapNotNull null
            LibretroCatalogEntry(label, url)
        }
        .distinctBy { it.url }
        .toList()

internal fun matchLibretroCatalog(
    query: ArtworkQuery,
    entries: List<LibretroCatalogEntry>,
): List<LibretroCatalogMatch> {
    val exactLabel = libretroSafeFilename(query.title)
    entries.filter { it.label.equals(exactLabel, ignoreCase = true) && regionIsCompatible(query, it.label) }
        .takeIf(List<LibretroCatalogEntry>::isNotEmpty)
        ?.let { exact -> return exact.map { LibretroCatalogMatch(it, 1f) } }

    val baseTitle = normalizedShortTitle(query.title)
    if (baseTitle.isBlank()) return emptyList()
    val preferredRegion = preferredCatalogRegion(query)
    val sameGame = entries.filter { normalizedShortTitle(it.label) == baseTitle }
    if (sameGame.isEmpty()) return emptyList()

    val compatible = if (preferredRegion == null) {
        sameGame
    } else {
        sameGame.filter { entry ->
            val candidateRegion = regionInTitle(entry.label)
            candidateRegion == preferredRegion || candidateRegion == "World" || candidateRegion == null
        }
    }
    return compatible
        .sortedWith(
            compareByDescending<LibretroCatalogEntry> { regionInTitle(it.label) == preferredRegion }
                .thenBy { it.label.length },
        )
        .take(8)
        .map { entry ->
            val candidateRegion = regionInTitle(entry.label)
            val confidence = when {
                preferredRegion != null && candidateRegion == preferredRegion -> 0.98f
                preferredRegion != null && candidateRegion == "World" -> 0.95f
                preferredRegion != null && candidateRegion == null -> 0.93f
                else -> 0.9f
            }
            LibretroCatalogMatch(entry, confidence)
        }
}

internal fun libretroSafeFilename(value: String): String = value
    .trim()
    .replace(Regex("[&*/:`<>?\\\\|\"]"), "_")

private fun normalizedShortTitle(value: String): String {
    val withoutTags = value.substringBefore('(').substringBefore('[').substringBefore('{')
    return Normalizer.normalize(withoutTags, Normalizer.Form.NFKD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
}

private fun regionInTitle(value: String): String? {
    val regions = listOf("USA", "Europe", "Japan", "Korea", "Taiwan", "China", "Australia", "World")
    return regions.firstOrNull { region ->
        Regex("\\(${Regex.escape(region)}\\)", RegexOption.IGNORE_CASE).containsMatchIn(value)
    }
}

private fun catalogRegion(region: String?): String? = when (region?.uppercase()) {
    "US" -> "USA"
    "EN", "EU" -> "Europe"
    "JA", "JP" -> "Japan"
    "KO", "KR" -> "Korea"
    "ZHTW", "TW" -> "Taiwan"
    "ZHCN", "CN" -> "China"
    else -> null
}

private fun preferredCatalogRegion(query: ArtworkQuery): String? =
    catalogRegion(query.region) ?: regionInTitle(query.title)

private fun regionIsCompatible(query: ArtworkQuery, candidateLabel: String): Boolean {
    val preferredRegion = preferredCatalogRegion(query) ?: return true
    val candidateRegion = regionInTitle(candidateLabel)
    return candidateRegion == null || candidateRegion == preferredRegion || candidateRegion == "World"
}

private fun InputStream.readUtf8Limited(maxBytes: Int): String {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        total += read
        require(total <= maxBytes) { "Libretro catalog is too large" }
        output.write(buffer, 0, read)
    }
    return output.toString(Charsets.UTF_8.name())
}
