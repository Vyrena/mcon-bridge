package com.vyrena.mconbridge

import com.vyrena.mconbridge.launch.KirinPathPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KirinPathPolicyTest {
    @Test
    fun acceptsDefaultGameDirectory() {
        assertEquals(
            "/storage/emulated/0/Kirin/games/Pokemon Uranium",
            KirinPathPolicy.canonicalGamePath("/storage/emulated/0/Kirin/games/Pokemon Uranium"),
        )
        assertNull(KirinPathPolicy.canonicalGamePath("/storage/emulated/0/Kirin/games/../saves"))
        assertNull(KirinPathPolicy.canonicalGamePath("/storage/emulated/0/Kirin/games/Folder/Nested"))
        assertNull(KirinPathPolicy.canonicalGamePath("/storage/emulated/0/Other/Game"))
    }

    @Test
    fun acceptsUserSelectedLibraryOrGameFolder() {
        val selectedRoot = "/storage/emulated/0/Download/RPG Maker"
        assertEquals(
            "/storage/emulated/0/Download/RPG Maker/My Game",
            KirinPathPolicy.canonicalGamePath(
                "/storage/emulated/0/Download/RPG Maker/My Game",
                selectedRoot,
            ),
        )
        assertEquals(selectedRoot, KirinPathPolicy.canonicalGamePath(selectedRoot, selectedRoot))
        assertNull(
            KirinPathPolicy.canonicalGamePath(
                "/storage/emulated/0/Download/Another Game",
                selectedRoot,
            ),
        )
        assertNull(KirinPathPolicy.canonicalSelectedRoot("/storage/emulated/0"))
    }
}
