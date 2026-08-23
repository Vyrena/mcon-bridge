package com.vyrena.mconbridge

import com.vyrena.mconbridge.artwork.ArtworkMatchScorer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkMatchScorerTest {
    @Test
    fun exactNormalizedTitleIsHighConfidence() {
        assertEquals(1f, ArtworkMatchScorer.score("Pokémon: Ultra Sun", "Pokemon Ultra Sun"))
    }

    @Test
    fun unrelatedTitleIsNotAutoMatchQuality() {
        assertTrue(ArtworkMatchScorer.score("Pokemon Ultra Sun", "Hollow Knight") < 0.45f)
    }
}
