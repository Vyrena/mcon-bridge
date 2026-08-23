package com.vyrena.mconbridge

import com.vyrena.mconbridge.data.GameEntryEntity
import com.vyrena.mconbridge.data.SourceType
import com.vyrena.mconbridge.ui.suggestedArtworkFilename
import org.junit.Assert.assertEquals
import org.junit.Test

class ArtworkExportTest {
    @Test
    fun `saved artwork always uses png extension`() {
        val game = game(title = "Fire Emblem - Awakening (USA)", artworkUri = "file:///cache/cover.webp")

        assertEquals("Fire Emblem - Awakening (USA)-box-art.png", suggestedArtworkFilename(game))
    }

    @Test
    fun `saved artwork filename removes unsafe characters`() {
        val game = game(title = "A/B: C?", artworkUri = "file:///cache/cover.jpg")

        assertEquals("A_B_ C_-box-art.png", suggestedArtworkFilename(game))
    }

    private fun game(title: String, artworkUri: String) = GameEntryEntity(
        id = "game",
        title = title,
        source = SourceType.AZAHAR,
        sourceKey = "source",
        launchPayload = "payload",
        artworkUri = artworkUri,
        createdAt = 0,
        updatedAt = 0,
    )
}
