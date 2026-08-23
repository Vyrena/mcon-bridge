package com.vyrena.mconbridge

import android.content.Context
import com.vyrena.mconbridge.backup.BridgeBackupManager
import com.vyrena.mconbridge.data.BridgeDatabase
import com.vyrena.mconbridge.data.BridgeRepository
import com.vyrena.mconbridge.importer.ArtemisImporter
import com.vyrena.mconbridge.importer.AzaharImporter
import com.vyrena.mconbridge.importer.KirinScanner
import com.vyrena.mconbridge.launch.LaunchCoordinator

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = BridgeDatabase.create(appContext)

    val repository = BridgeRepository(database.gameDao())
    val launcher = LaunchCoordinator(repository)
    val artemisImporter = ArtemisImporter(appContext.contentResolver, repository)
    val azaharImporter = AzaharImporter(appContext.contentResolver, repository)
    val kirinScanner = KirinScanner(appContext, repository)
    val backupManager = BridgeBackupManager(appContext.contentResolver, repository)
}
