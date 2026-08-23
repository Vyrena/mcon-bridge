package com.vyrena.mconbridge

import com.vyrena.mconbridge.launch.KirinPathPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KirinPathPolicyTest {
    @Test
    fun acceptsOnlyChildGameDirectories() {
        assertEquals(
            "/storage/emulated/0/Kirin/games/Pokemon Uranium",
            KirinPathPolicy.canonicalGamePath("/storage/emulated/0/Kirin/games/Pokemon Uranium"),
        )
        assertNull(KirinPathPolicy.canonicalGamePath("/storage/emulated/0/Kirin/games/../saves"))
        assertNull(KirinPathPolicy.canonicalGamePath("/storage/emulated/0/Kirin/games/Folder/Nested"))
        assertNull(KirinPathPolicy.canonicalGamePath("/storage/emulated/0/Kirin/games"))
        assertNull(KirinPathPolicy.canonicalGamePath("/storage/emulated/0/Other/Game"))
    }
}
