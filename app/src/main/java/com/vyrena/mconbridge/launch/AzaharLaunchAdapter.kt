package com.vyrena.mconbridge.launch

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.vyrena.mconbridge.domain.AzaharPayload
import com.vyrena.mconbridge.domain.LaunchPayload
import com.vyrena.mconbridge.domain.LaunchResult

class AzaharLaunchAdapter : LaunchAdapter {
    override fun supports(payload: LaunchPayload): Boolean = payload is AzaharPayload

    override fun validate(payload: LaunchPayload): String? {
        val value = (payload as? AzaharPayload)?.titleId ?: return "Invalid Azahar launch data"
        return if (TITLE_ID.matches(value)) null else "Azahar title ID must contain 16 hexadecimal characters"
    }

    override fun launch(context: Context, gameId: String, payload: LaunchPayload): LaunchResult {
        val azahar = payload as AzaharPayload
        validate(azahar)?.let { return LaunchResult.Error(it) }
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("azahar-mcon://game/${azahar.titleId.uppercase()}"),
        ).apply {
            setPackage(PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (intent.resolveActivity(context.packageManager) == null) {
            return LaunchResult.Error("Azahar MCON is not installed, or this version cannot open game links")
        }
        return runCatching { context.startActivity(intent) }
            .fold({ LaunchResult.Started }, { LaunchResult.Error(it.message ?: "Unable to launch Azahar") })
    }

    companion object {
        const val PACKAGE = "org.azahar_emu.azahar.mcon"
        private val TITLE_ID = Regex("^[0-9a-fA-F]{16}$")
    }
}
