package com.vyrena.mconbridge.launch

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.vyrena.mconbridge.domain.ArtemisPayload
import com.vyrena.mconbridge.domain.LaunchPayload
import com.vyrena.mconbridge.domain.LaunchResult
import java.io.File

class ArtemisLaunchAdapter : LaunchAdapter {
    override fun supports(payload: LaunchPayload): Boolean = payload is ArtemisPayload

    override fun validate(payload: LaunchPayload): String? {
        val artemis = payload as? ArtemisPayload ?: return "Invalid Artemis launch data"
        if (artemis.hostUuid.isBlank() || artemis.hostUuid.length > 128) return "Artemis host UUID is missing or invalid"
        val identities = listOf(artemis.appUuid, artemis.appName, artemis.appId).count { !it.isNullOrBlank() }
        if (identities == 0) return "Artemis game identifier is missing"
        if (listOf(artemis.hostName, artemis.appUuid, artemis.appName, artemis.appId)
                .filterNotNull().any { it.contains('\n') || it.contains('\r') }
        ) return "Artemis launch data contains an invalid newline"
        if (artemis.appId != null && !artemis.appId.matches(Regex("^[0-9]{1,10}$"))) return "Artemis app ID is invalid"
        return null
    }

    override fun launch(context: Context, gameId: String, payload: LaunchPayload): LaunchResult {
        val artemis = payload as ArtemisPayload
        validate(artemis)?.let { return LaunchResult.Error(it) }
        return runCatching {
            val directory = File(context.cacheDir, "art-launchers").apply { mkdirs() }
            val launcherFile = File(directory, "$gameId.art")
            launcherFile.writeText(ArtemisArtFile.encode(artemis), Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", launcherFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                component = ComponentName(PACKAGE, TRAMPOLINE_ACTIVITY)
                setDataAndType(uri, "application/octet-stream")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.grantUriPermission(PACKAGE, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(intent)
        }.fold(
            onSuccess = { LaunchResult.Started },
            onFailure = { LaunchResult.Error(it.message ?: "Unable to launch Artemis") },
        )
    }

    companion object {
        const val PACKAGE = "com.limelight.noir"
        const val TRAMPOLINE_ACTIVITY = "com.limelight.ShortcutTrampoline"
    }
}
