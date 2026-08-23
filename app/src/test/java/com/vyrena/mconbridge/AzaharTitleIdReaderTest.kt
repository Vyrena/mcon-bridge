package com.vyrena.mconbridge

import com.vyrena.mconbridge.domain.AzaharPayload
import com.vyrena.mconbridge.domain.LaunchPayloadCodec
import com.vyrena.mconbridge.importer.AzaharTitleIdReader
import com.vyrena.mconbridge.importer.gameTdbRegionFromProductCode
import com.vyrena.mconbridge.launch.AzaharLaunchAdapter
import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AzaharTitleIdReaderTest {
    @Test
    fun `reads program id from NCCH`() {
        val header = ncch("0004000000123400", "CTR-P-AFEE")

        assertEquals(
            "0004000000123400",
            AzaharTitleIdReader.read(ByteArrayInputStream(header)),
        )
        assertEquals(
            "CTR-P-AFEE",
            AzaharTitleIdReader.readMetadata(ByteArrayInputStream(header))?.productCode,
        )
    }

    @Test
    fun `reads program id from first NCSD partition`() {
        val ncsd = ByteArray(0x200).apply {
            putMagic(0x100, "NCSD")
            this[0x120] = 1
        }
        val image = ncsd + ncch("0004000000ABCDEF", "CTR-P-ABCP")

        assertEquals(
            "0004000000ABCDEF",
            AzaharTitleIdReader.read(ByteArrayInputStream(image)),
        )
        assertEquals(
            "CTR-P-ABCP",
            AzaharTitleIdReader.readMetadata(ByteArrayInputStream(image))?.productCode,
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

    @Test
    fun `maps common product destinations to GameTDB regions`() {
        assertEquals("US", gameTdbRegionFromProductCode("CTR-P-AFEE"))
        assertEquals("EN", gameTdbRegionFromProductCode("CTR-P-ABCP"))
        assertEquals("JA", gameTdbRegionFromProductCode("CTR-P-ABCJ"))
        assertEquals("KO", gameTdbRegionFromProductCode("CTR-P-ABCK"))
    }

    private fun ncch(titleId: String, productCode: String = "CTR-P-TEST"): ByteArray = ByteArray(0x200).apply {
        putMagic(0x100, "NCCH")
        titleId.chunked(2).map { it.toInt(16).toByte() }.reversed().forEachIndexed { index, byte ->
            this[0x118 + index] = byte
        }
        putMagic(0x150, productCode)
    }

    private fun ByteArray.putMagic(offset: Int, value: String) {
        value.toByteArray(Charsets.US_ASCII).copyInto(this, offset)
    }
}
