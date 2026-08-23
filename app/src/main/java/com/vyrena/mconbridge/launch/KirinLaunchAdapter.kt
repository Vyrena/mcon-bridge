package com.vyrena.mconbridge.launch

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.vyrena.mconbridge.domain.KirinPayload
import com.vyrena.mconbridge.domain.LaunchPayload
import com.vyrena.mconbridge.domain.LaunchResult

class KirinLaunchAdapter : LaunchAdapter {
    override fun supports(payload: LaunchPayload): Boolean = payload is KirinPayload

    override fun validate(payload: LaunchPayload): String? {
        val kirin = payload as? KirinPayload ?: return "Invalid Kirin launch data"
        val selectedRoot = kirin.selectedRoot ?: KirinPathPolicy.DEFAULT_GAMES_ROOT
        return if (KirinPathPolicy.canonicalGamePath(kirin.gamePath, selectedRoot) == null) {
            "Kirin game path is outside the folder selected during the scan"
        } else null
    }

    override fun launch(context: Context, gameId: String, payload: LaunchPayload): LaunchResult {
        val kirin = payload as KirinPayload
        val selectedRoot = kirin.selectedRoot ?: KirinPathPolicy.DEFAULT_GAMES_ROOT
        val canonicalPath = KirinPathPolicy.canonicalGamePath(kirin.gamePath, selectedRoot)
            ?: return LaunchResult.Error("Kirin game path is invalid or outside the folder selected during the scan")
        val intent = Intent(ACTION).apply {
            component = ComponentName(PACKAGE, MAIN_ACTIVITY)
            putExtra(EXTRA_GAME_PATH, canonicalPath)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching { context.startActivity(intent) }
            .fold({ LaunchResult.Started }, { LaunchResult.Error(it.message ?: "Unable to launch Kirin") })
    }

    companion object {
        const val PACKAGE = "com.gmax.kirin"
        const val MAIN_ACTIVITY = "com.gmax.kirin.MainActivity"
        const val ACTION = "com.gmax.kirin.action.LAUNCH_GAME_SHORTCUT"
        const val EXTRA_GAME_PATH = "shortcut_game_path"
    }
}
