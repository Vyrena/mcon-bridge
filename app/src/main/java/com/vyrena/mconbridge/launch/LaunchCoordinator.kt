package com.vyrena.mconbridge.launch

import android.content.Context
import com.vyrena.mconbridge.data.BridgeRepository
import com.vyrena.mconbridge.domain.LaunchPayloadCodec
import com.vyrena.mconbridge.domain.LaunchResult

class LaunchCoordinator(
    private val repository: BridgeRepository,
    private val adapters: List<LaunchAdapter> = listOf(
        AzaharLaunchAdapter(),
        ArtemisLaunchAdapter(),
        KirinLaunchAdapter(),
    ),
) {
    suspend fun launch(context: Context, gameId: String): LaunchResult {
        val game = repository.getGame(gameId) ?: return LaunchResult.Error("Game link is not in this bridge library")
        if (!game.enabled) return LaunchResult.Error("This game is disabled in MCON Bridge")
        val payload = runCatching { LaunchPayloadCodec.decode(game.launchPayload) }
            .getOrElse { return LaunchResult.Error("Stored launch data is damaged") }
        val adapter = adapters.firstOrNull { it.supports(payload) }
            ?: return LaunchResult.Error("No safe launcher adapter supports this game")
        adapter.validate(payload)?.let { return LaunchResult.Error(it) }
        val result = adapter.launch(context, gameId, payload)
        if (result is LaunchResult.Started) {
            repository.update(game.copy(lastValidatedAt = System.currentTimeMillis()))
        }
        return result
    }
}
