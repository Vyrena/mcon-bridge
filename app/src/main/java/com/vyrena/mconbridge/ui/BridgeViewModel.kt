package com.vyrena.mconbridge.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vyrena.mconbridge.MconBridgeApplication
import com.vyrena.mconbridge.artwork.ArtworkCandidate
import com.vyrena.mconbridge.data.GameEntryEntity
import com.vyrena.mconbridge.data.SourceType
import com.vyrena.mconbridge.domain.AzaharPayload
import com.vyrena.mconbridge.domain.BridgeLink
import com.vyrena.mconbridge.domain.LaunchResult
import com.vyrena.mconbridge.settings.BridgeSettings
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface BridgeEvent {
    data class Message(val text: String) : BridgeEvent
    data class StartIntent(val intent: Intent) : BridgeEvent
}

data class ArtworkPickerState(
    val game: GameEntryEntity,
    val loading: Boolean = true,
    val candidates: List<ArtworkCandidate> = emptyList(),
    val error: String? = null,
)

class BridgeViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as MconBridgeApplication).container
    private val eventChannel = Channel<BridgeEvent>(Channel.BUFFERED)

    val games: StateFlow<List<GameEntryEntity>> = container.repository.observeGames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val settings: StateFlow<BridgeSettings> = container.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BridgeSettings())
    val events = eventChannel.receiveAsFlow()

    val busy = MutableStateFlow<String?>(null)
    val artworkPicker = MutableStateFlow<ArtworkPickerState?>(null)
    val cacheSizeBytes = MutableStateFlow(container.artworkRepository.cacheSizeBytes())

    fun importAzahar(uri: Uri) = runBusy("Importing Azahar library") {
        container.azaharImporter.import(uri).fold(
            onSuccess = { emit("Imported ${it.size} Azahar game${plural(it.size)}") },
            onFailure = { emit(it.message ?: "Azahar import failed") },
        )
    }

    fun addAzahar(title: String, titleId: String, productCode: String?, region: String?) =
        runBusy("Adding Azahar game") {
            val normalizedId = titleId.trim().uppercase()
            if (!normalizedId.matches(Regex("^[0-9A-F]{16}$"))) {
                emit("Azahar title ID must contain exactly 16 hexadecimal characters")
                return@runBusy
            }
            container.repository.upsertImported(
                title = title,
                source = SourceType.AZAHAR,
                sourceKey = normalizedId,
                payload = AzaharPayload(
                    titleId = normalizedId,
                    productCode = productCode?.trim()?.uppercase()?.takeIf(String::isNotEmpty),
                    region = region?.trim()?.uppercase()?.takeIf(String::isNotEmpty),
                ),
            )
            emit("Added ${title.trim().ifEmpty { "Untitled game" }}")
        }

    fun importArtemis(uris: List<Uri>) = runBusy("Importing Artemis launchers") {
        container.artemisImporter.import(uris).fold(
            onSuccess = { emit("Imported ${it.size} Artemis game${plural(it.size)}") },
            onFailure = { emit(it.message ?: "Artemis import failed") },
        )
    }

    fun scanKirin(uri: Uri) = runBusy("Scanning Kirin games") {
        container.kirinScanner.scan(uri).fold(
            onSuccess = { emit("Found ${it.size} Kirin game${plural(it.size)}; no save files were changed") },
            onFailure = { emit(it.message ?: "Kirin scan failed") },
        )
    }

    fun launch(game: GameEntryEntity) = viewModelScope.launch {
        when (val result = container.launcher.launch(getApplication(), game.id)) {
            LaunchResult.Started -> Unit
            is LaunchResult.Error -> emit(result.message)
        }
    }

    fun copyLink(game: GameEntryEntity) {
        container.mconExporter.copyLink(game)
        emitNow("Copied ${BridgeLink.build(game.id)}")
    }

    fun exportMcon() = runBusy("Preparing MCON library") {
        container.mconExporter.buildLibraryIntent().fold(
            onSuccess = {
                eventChannel.send(BridgeEvent.StartIntent(it.intent))
                emit(
                    if (it.directToMcon) "Sending ${it.gameCount} games to MCON"
                    else "MCON does not advertise bulk import; choose a compatible target or use Copy link",
                )
            },
            onFailure = { emit(it.message ?: "Unable to prepare MCON export") },
        )
    }

    fun exportBackup(uri: Uri) = runBusy("Creating backup") {
        container.backupManager.export(uri).fold(
            onSuccess = { emit("Backed up $it game${plural(it)}") },
            onFailure = { emit(it.message ?: "Backup failed") },
        )
    }

    fun restoreBackup(uri: Uri) = runBusy("Restoring backup") {
        container.backupManager.restore(uri).fold(
            onSuccess = { emit("Restored $it game${plural(it)}") },
            onFailure = { emit(it.message ?: "Restore failed") },
        )
    }

    fun delete(game: GameEntryEntity) = viewModelScope.launch {
        container.repository.delete(game)
        emit("Removed ${game.title} from the bridge; emulator files and saves were untouched")
    }

    fun searchArtwork(game: GameEntryEntity) {
        artworkPicker.value = ArtworkPickerState(game)
        viewModelScope.launch {
            container.artworkRepository.search(game).fold(
                onSuccess = {
                    artworkPicker.value = ArtworkPickerState(
                        game = game,
                        loading = false,
                        candidates = it,
                        error = if (it.isEmpty()) "No online matches. Try a local image or add a SteamGridDB key." else null,
                    )
                },
                onFailure = {
                    artworkPicker.value = ArtworkPickerState(game, loading = false, error = it.message)
                },
            )
        }
    }

    fun applyArtwork(candidate: ArtworkCandidate) {
        val state = artworkPicker.value ?: return
        artworkPicker.value = state.copy(loading = true, error = null)
        viewModelScope.launch {
            container.artworkRepository.apply(state.game, candidate).fold(
                onSuccess = {
                    artworkPicker.value = null
                    cacheSizeBytes.value = container.artworkRepository.cacheSizeBytes()
                    emit("Artwork saved with ${candidate.provider} attribution")
                },
                onFailure = {
                    artworkPicker.value = state.copy(loading = false, error = it.message)
                },
            )
        }
    }

    fun applyLocalArtwork(game: GameEntryEntity, uri: Uri) = runBusy("Importing local artwork") {
        container.artworkRepository.applyLocal(game, uri).fold(
            onSuccess = {
                cacheSizeBytes.value = container.artworkRepository.cacheSizeBytes()
                emit("Local artwork applied to ${game.title}")
            },
            onFailure = { emit(it.message ?: "Unable to import local artwork") },
        )
    }

    fun closeArtworkPicker() {
        artworkPicker.value = null
    }

    fun saveSettings(wifiOnly: Boolean, cacheMb: Int, regions: String, steamGridDbKey: String?) =
        runBusy("Saving settings") {
            container.settings.setWifiOnly(wifiOnly)
            container.settings.setArtworkCacheMb(cacheMb)
            container.settings.setGameTdbRegions(regions.split(',').map(String::trim).filter(String::isNotEmpty))
            steamGridDbKey?.let { container.settings.setSteamGridDbKey(it) }
            container.artworkRepository.prune(cacheMb)
            cacheSizeBytes.value = container.artworkRepository.cacheSizeBytes()
            emit("Artwork settings saved")
        }

    fun clearSteamGridDbKey() = runBusy("Removing SteamGridDB key") {
        container.settings.setSteamGridDbKey("")
        emit("SteamGridDB key removed")
    }

    fun pruneCache() = runBusy("Pruning artwork cache") {
        val removed = container.artworkRepository.prune(settings.value.artworkCacheMb)
        cacheSizeBytes.value = container.artworkRepository.cacheSizeBytes()
        emit("Removed ${removed / 1024} KB of unused artwork")
    }

    private fun runBusy(label: String, block: suspend () -> Unit) {
        if (busy.value != null) return
        viewModelScope.launch {
            busy.value = label
            try {
                block()
            } finally {
                busy.value = null
            }
        }
    }

    private suspend fun emit(message: String) = eventChannel.send(BridgeEvent.Message(message))

    private fun emitNow(message: String) {
        eventChannel.trySend(BridgeEvent.Message(message))
    }

    private fun plural(count: Int) = if (count == 1) "" else "s"
}
