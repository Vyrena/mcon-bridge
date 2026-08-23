package com.vyrena.mconbridge

import com.vyrena.mconbridge.artwork.ArtworkQuery
import com.vyrena.mconbridge.artwork.LibretroCatalogEntry
import com.vyrena.mconbridge.artwork.LibretroThumbnailProvider
import com.vyrena.mconbridge.artwork.libretroSafeFilename
import com.vyrena.mconbridge.artwork.matchLibretroCatalog
import com.vyrena.mconbridge.artwork.parseLibretroCatalog
import com.vyrena.mconbridge.data.SourceType
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibretroThumbnailProviderTest {
    @Test
    fun `parses only image links from the official catalog`() {
        val html = """
            <a href="Fire%20Emblem%20-%20Awakening%20(USA).png">cover</a>
            <a href="AKB48+Me%20(Japan).png">cover</a>
            <a href="../">parent</a>
            <a href="https://attacker.example/fake.png">external</a>
        """.trimIndent()

        val entries = parseLibretroCatalog(html, LibretroThumbnailProvider.CATALOG_URL)

        assertEquals(listOf("Fire Emblem - Awakening (USA)", "AKB48+Me (Japan)"), entries.map { it.label })
    }

    @Test
    fun `matches normalized title only within the ROM region`() {
        val entries = listOf(
            entry("Fire Emblem - Awakening (Europe) (En,Fr,De,Es,It)"),
            entry("Fire Emblem - Awakening (USA)"),
            entry("Fire Emblem Fates - Birthright (USA)"),
        )
        val matches = matchLibretroCatalog(
            ArtworkQuery(
                title = "Fire Emblem Awakening",
                source = SourceType.AZAHAR,
                sourceId = "00040000000A0500",
                productCode = "CTR-P-AFEE",
                region = "US",
            ),
            entries,
        )

        assertEquals(listOf("Fire Emblem - Awakening (USA)"), matches.map { it.entry.label })
        assertEquals(0.98f, matches.single().confidence)
    }

    @Test
    fun `does not fuzzy match a different game in the same series`() {
        val matches = matchLibretroCatalog(
            ArtworkQuery("Fire Emblem Fates", SourceType.AZAHAR, "id", region = "US"),
            listOf(entry("Fire Emblem - Awakening (USA)")),
        )

        assertTrue(matches.isEmpty())
    }

    @Test
    fun `trusts ROM region metadata over a misleading filename`() {
        val matches = matchLibretroCatalog(
            ArtworkQuery(
                title = "Fire Emblem - Awakening (Europe)",
                source = SourceType.AZAHAR,
                sourceId = "id",
                region = "US",
            ),
            listOf(
                entry("Fire Emblem - Awakening (Europe)"),
                entry("Fire Emblem - Awakening (USA)"),
            ),
        )

        assertEquals(listOf("Fire Emblem - Awakening (USA)"), matches.map { it.entry.label })
    }

    @Test
    fun `uses Libretro invalid character replacement`() {
        assertEquals("Mario _ Luigi_ Dream Team", libretroSafeFilename("Mario & Luigi: Dream Team"))
    }

    private fun entry(label: String) = LibretroCatalogEntry(
        label,
        "https://thumbnails.libretro.com/Nintendo%20-%20Nintendo%203DS/Named_Boxarts/"
            .toHttpUrl().newBuilder().addPathSegment("$label.png").build(),
    )
}
