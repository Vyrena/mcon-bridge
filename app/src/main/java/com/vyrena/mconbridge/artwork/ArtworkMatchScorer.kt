package com.vyrena.mconbridge.artwork

import java.text.Normalizer

object ArtworkMatchScorer {
    fun score(query: String, candidate: String): Float {
        val left = normalize(query)
        val right = normalize(candidate)
        if (left.isEmpty() || right.isEmpty()) return 0f
        if (left == right) return 1f
        if (left.startsWith(right) || right.startsWith(left)) return 0.9f
        if (left.contains(right) || right.contains(left)) return 0.78f
        val leftTokens = left.split(' ').toSet()
        val rightTokens = right.split(' ').toSet()
        val union = leftTokens union rightTokens
        if (union.isEmpty()) return 0f
        return ((leftTokens intersect rightTokens).size.toFloat() / union.size).coerceIn(0f, 0.75f)
    }

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
}
