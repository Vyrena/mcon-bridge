package com.vyrena.mconbridge.launch

import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.vyrena.mconbridge.domain.AzaharPayload
import com.vyrena.mconbridge.domain.LaunchPayload
import com.vyrena.mconbridge.domain.LaunchResult

class AzaharLaunchAdapter : LaunchAdapter {
    override fun supports(payload: LaunchPayload): Boolean = payload is AzaharPayload

    override fun validate(payload: LaunchPayload): String? {
        val azahar = payload as? AzaharPayload ?: return "Invalid Azahar launch data"
        if (!TITLE_ID.matches(azahar.titleId)) {
            return "Azahar title ID must contain 16 hexadecimal characters"
        }
        val uri = azahar.gameUri?.takeIf(String::isNotBlank)?.toUri()
            ?: return "Choose the Azahar ROM again so the ordinary Azahar app can launch it"
        if (uri.scheme != "content") return "Azahar ROM access is invalid; choose the ROM again"
        val filename = azahar.filename?.takeIf(String::isNotBlank)
            ?: return "Azahar ROM filename is missing; choose the ROM again"
        val extension = azahar.fileType?.lowercase()?.takeIf(SUPPORTED_EXTENSIONS::contains)
            ?: filename.substringAfterLast('.', "").lowercase().takeIf(SUPPORTED_EXTENSIONS::contains)
            ?: return "This file type is not supported by Azahar"
        return if (extension.isBlank()) "This file type is not supported by Azahar" else null
    }

    override fun launch(context: Context, gameId: String, payload: LaunchPayload): LaunchResult {
        val azahar = payload as AzaharPayload
        validate(azahar)?.let { return LaunchResult.Error(it) }

        val uri = requireNotNull(azahar.gameUri).toUri()
        val intent = Intent(Intent.ACTION_VIEW).apply {
            component = ComponentName(PACKAGE, EMULATION_ACTIVITY)
            data = uri
            clipData = ClipData.newUri(context.contentResolver, "Azahar game", uri)
            putExtra("launched_from_shortcut", true)
            putExtra("launchedFromShortcut", true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (intent.resolveActivity(context.packageManager) == null) {
            return LaunchResult.Error("Ordinary Azahar is not installed")
        }
        return runCatching {
            context.grantUriPermission(PACKAGE, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(intent)
        }.fold(
            onSuccess = { LaunchResult.Started },
            onFailure = { LaunchResult.Error(it.message ?: "Unable to launch ordinary Azahar") },
        )
    }

    companion object {
        const val PACKAGE = "org.azahar_emu.azahar"
        const val EMULATION_ACTIVITY = "org.citra.citra_emu.activities.EmulationActivity"
        val SUPPORTED_EXTENSIONS = setOf(
            "3dsx", "app", "axf", "cci", "cxi", "elf", "z3dsx", "zcci", "zcxi", "3ds",
        )
        private val TITLE_ID = Regex("^[0-9a-fA-F]{16}$")
    }
}
