package com.vyrena.mconbridge

import com.vyrena.mconbridge.domain.BridgeLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class BridgeLinkTest {
    @Test
    fun roundTripStableLink() {
        val id = UUID.randomUUID().toString()
        assertEquals(id, BridgeLink.parse(BridgeLink.build(id)))
    }

    @Test
    fun rejectsExtraDataAndNonUuidIds() {
        assertNull(BridgeLink.parse("mconbridge://launch/not-an-id"))
        assertNull(BridgeLink.parse("mconbridge://launch/${UUID.randomUUID()}?package=evil"))
        assertNull(BridgeLink.parse("https://launch/${UUID.randomUUID()}"))
    }
}
