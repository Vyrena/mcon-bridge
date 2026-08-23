package com.vyrena.mconbridge

import android.content.Context
import com.vyrena.mconbridge.artwork.ArtworkCache
import com.vyrena.mconbridge.artwork.ArtworkRepository
import com.vyrena.mconbridge.artwork.GameTdbProvider
import com.vyrena.mconbridge.artwork.NetworkPolicy
import com.vyrena.mconbridge.artwork.SteamGridDbProvider
import com.vyrena.mconbridge.backup.BridgeBackupManager
import com.vyrena.mconbridge.data.BridgeDatabase
import com.vyrena.mconbridge.data.BridgeRepository
import com.vyrena.mconbridge.importer.ArtemisImporter
import com.vyrena.mconbridge.importer.AzaharImporter
import com.vyrena.mconbridge.importer.KirinScanner
import com.vyrena.mconbridge.launch.LaunchCoordinator
import com.vyrena.mconbridge.settings.SettingsRepository
import com.vyrena.mconbridge.export.MconExportManager
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = BridgeDatabase.create(appContext)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build()

    val repository = BridgeRepository(database.gameDao())
    val settings = SettingsRepository(appContext)
    val launcher = LaunchCoordinator(repository)
    val artemisImporter = ArtemisImporter(appContext.contentResolver, repository)
    val azaharImporter = AzaharImporter(appContext.contentResolver, repository)
    val kirinScanner = KirinScanner(appContext, repository)
    val backupManager = BridgeBackupManager(appContext.contentResolver, repository)
    val mconExporter = MconExportManager(appContext, repository)
    val artworkRepository = ArtworkRepository(
        bridgeRepository = repository,
        settings = settings,
        networkPolicy = NetworkPolicy(appContext),
        providers = listOf(
            GameTdbProvider(httpClient, settings),
            SteamGridDbProvider(httpClient, settings),
        ),
        cache = ArtworkCache(appContext, httpClient),
    )
}
