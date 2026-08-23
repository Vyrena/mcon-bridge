package com.vyrena.mconbridge

import com.vyrena.mconbridge.artwork.gameTdbIdFromProductCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameTdbProviderTest {
    @Test
    fun `accepts database id directly`() {
        assertEquals("ECLE", gameTdbIdFromProductCode("ecle"))
    }

    @Test
    fun `extracts database id from 3DS product code`() {
        assertEquals("ECLE", gameTdbIdFromProductCode("CTR-P-ECLE"))
    }

    @Test
    fun `rejects malformed product code`() {
        assertNull(gameTdbIdFromProductCode("../ECLE"))
        assertNull(gameTdbIdFromProductCode("CTR-P-TOO-LONG"))
    }
}
