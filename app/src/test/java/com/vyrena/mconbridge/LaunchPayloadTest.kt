package com.vyrena.mconbridge

import com.vyrena.mconbridge.domain.ArtemisPayload
import com.vyrena.mconbridge.domain.KirinPayload
import com.vyrena.mconbridge.domain.LaunchPayloadCodec
import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchPayloadTest {
    @Test
    fun serializedPayloadRoundTrips() {
        val payload = ArtemisPayload(
            hostUuid = "host-123",
            hostName = "Living room PC",
            appUuid = "app-456",
            appName = "Example Game",
            appId = "42",
        )
        assertEquals(payload, LaunchPayloadCodec.decode(LaunchPayloadCodec.encode(payload)))
    }

    @Test
    fun oldKirinPayloadDefaultsToOriginalGamesFolder() {
        assertEquals(
            KirinPayload("/storage/emulated/0/Kirin/games/My Game"),
            LaunchPayloadCodec.decode(
                """{"type":"kirin","gamePath":"/storage/emulated/0/Kirin/games/My Game"}""",
            ),
        )
    }
}
