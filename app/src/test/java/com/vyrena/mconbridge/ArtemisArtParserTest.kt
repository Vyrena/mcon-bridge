package com.vyrena.mconbridge

import com.vyrena.mconbridge.importer.ArtemisArtParser
import com.vyrena.mconbridge.launch.ArtemisArtFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtemisArtParserTest {
    @Test
    fun parsesOfficialLauncherFormat() {
        val parsed = ArtemisArtParser.parse(
            """
            # Artemis app entry
            [host_uuid] host-1
            [host_name] Desktop
            [app_uuid] app-2
            [app_name] Hollow Knight
            [app_id] 42
            """.trimIndent(),
        )
        assertEquals("Hollow Knight", parsed.title)
        assertEquals("host-1:app-2", parsed.sourceKey)
        assertTrue(ArtemisArtFile.encode(parsed.payload).contains("[app_uuid] app-2"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMissingAppIdentity() {
        ArtemisArtParser.parse("[host_uuid] host-1")
    }
}
