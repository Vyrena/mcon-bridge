package com.vyrena.mconbridge

import com.vyrena.mconbridge.export.MconExportGame
import com.vyrena.mconbridge.export.MconLibraryExport
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MconExportFormatTest {
    @Test
    fun `export contains stable link but no emulator-private launch data`() {
        val export = MconLibraryExport(
            generatedAt = 0,
            games = listOf(
                MconExportGame(
                    id = "00000000-0000-0000-0000-000000000000",
                    title = "Example",
                    launchUrl = "mconbridge://launch/00000000-0000-0000-0000-000000000000",
                ),
            ),
        )

        val encoded = Json.encodeToString(export)

        assertTrue(encoded.contains("mconbridge://launch/"))
        assertFalse(encoded.contains("gamePath"))
        assertFalse(encoded.contains("hostUuid"))
        assertFalse(encoded.contains("titleId"))
        assertFalse(encoded.contains("launchPayload"))
    }
}
