package com.vyrena.mconbridge

import com.vyrena.mconbridge.domain.AzaharPayload
import com.vyrena.mconbridge.domain.LaunchPayloadCodec
import com.vyrena.mconbridge.importer.AzaharTitleIdReader
import com.vyrena.mconbridge.launch.AzaharLaunchAdapter
import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AzaharTitleIdReaderTest {
    @Test
    fun `reads program id from NCCH`() {
        val header = ncch("0004000000123400")

        assertEquals(
            "0004000000123400",
            AzaharTitleIdReader.read(ByteArrayInputStream(header)),
        )
    }

    @Test
    fun `reads program id from first NCSD partition`() {
        val ncsd = ByteArray(0x200).apply {
            putMagic(0x100, "NCSD")
            this[0x120] = 1
        }
        val image = ncsd + ncch("0004000000ABCDEF")

        assertEquals(
            "0004000000ABCDEF",
            AzaharTitleIdReader.read(ByteArrayInputStream(image)),
        )
    }

    @Test
    fun `rejects an unrelated file`() {
        assertNull(AzaharTitleIdReader.read(ByteArrayInputStream(ByteArray(0x200))))
    }

    @Test
    fun `legacy Azahar metadata decodes but requires a ROM relink`() {
        val payload = LaunchPayloadCodec.decode(
            """{"type":"azahar","titleId":"0004000000123400"}""",
        ) as AzaharPayload

        assertNull(payload.gameUri)
        assertTrue(AzaharLaunchAdapter().validate(payload)!!.contains("Choose the Azahar ROM"))
    }

    private fun ncch(titleId: String): ByteArray = ByteArray(0x200).apply {
        putMagic(0x100, "NCCH")
        titleId.chunked(2).map { it.toInt(16).toByte() }.reversed().forEachIndexed { index, byte ->
            this[0x118 + index] = byte
        }
    }

    private fun ByteArray.putMagic(offset: Int, value: String) {
        value.toByteArray(Charsets.US_ASCII).copyInto(this, offset)
    }
}
