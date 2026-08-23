package com.vyrena.mconbridge.ui

import android.app.Application
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.vyrena.mconbridge.artwork.ArtworkCandidate
import com.vyrena.mconbridge.data.GameEntryEntity
import com.vyrena.mconbridge.data.SourceType
import java.text.NumberFormat

private enum class BridgeTab { LIBRARY, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BridgeApp(viewModel: BridgeViewModel) {
    val games by viewModel.games.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val artworkPicker by viewModel.artworkPicker.collectAsStateWithLifecycle()
    val cacheBytes by viewModel.cacheSizeBytes.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(BridgeTab.LIBRARY) }
    var localArtworkTarget by remember { mutableStateOf<GameEntryEntity?>(null) }
    var artworkSaveTarget by remember { mutableStateOf<GameEntryEntity?>(null) }
    var azaharRelinkTarget by remember { mutableStateOf<GameEntryEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<GameEntryEntity?>(null) }

    val azaharRomPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) viewModel.importAzaharRoms(uris)
    }
    val azaharRelinkPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val game = azaharRelinkTarget
        if (uri != null && game != null) viewModel.linkAzaharRom(game, uri)
        azaharRelinkTarget = null
    }
    val artemisPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) viewModel.importArtemis(uris)
    }
    val kirinPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            runCatching {
                viewModel.getApplication<Application>().contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.scanKirin(it)
        }
    }
    val localArtworkPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val game = localArtworkTarget
        if (uri != null && game != null) viewModel.applyLocalArtwork(game, uri)
        localArtworkTarget = null
    }
    val artworkSavePicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/*")) { uri ->
        val game = artworkSaveTarget
        if (uri != null && game != null) viewModel.saveArtwork(game, uri)
        artworkSaveTarget = null
    }
    val backupPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(viewModel::exportBackup)
    }
    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::restoreBackup)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("MCON Bridge", fontWeight = FontWeight.Black)
                        Text("Azahar · Artemis · Kirin", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(onClick = { backupPicker.launch("mcon-bridge-backup.json") }) {
                        Icon(Icons.Default.Backup, contentDescription = "Back up bridge library")
                    }
                    IconButton(onClick = { restorePicker.launch(arrayOf("application/json")) }) {
                        Icon(Icons.Default.Restore, contentDescription = "Restore bridge library")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == BridgeTab.LIBRARY,
                    onClick = { tab = BridgeTab.LIBRARY },
                    icon = { Icon(Icons.Default.Gamepad, null) },
                    label = { Text("Library") },
                )
                NavigationBarItem(
                    selected = tab == BridgeTab.SETTINGS,
                    onClick = { tab = BridgeTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, null) },
                    label = { Text("Settings") },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                BridgeTab.LIBRARY -> LibraryScreen(
                    games = games,
                    onAddAzaharRoms = { azaharRomPicker.launch(arrayOf("*/*")) },
                    onImportArtemis = { artemisPicker.launch(arrayOf("application/octet-stream", "text/plain", "*/*")) },
                    onScanKirin = { kirinPicker.launch(null) },
                    onExportMcon = viewModel::exportMcon,
                    onLaunch = viewModel::launch,
                    onCopy = viewModel::copyLink,
                    onRelinkAzahar = {
                        azaharRelinkTarget = it
                        azaharRelinkPicker.launch(arrayOf("*/*"))
                    },
                    onSearchArtwork = viewModel::searchArtwork,
                    onLocalArtwork = {
                        localArtworkTarget = it
                        localArtworkPicker.launch(arrayOf("image/*"))
                    },
                    onSaveArtwork = {
                        artworkSaveTarget = it
                        artworkSavePicker.launch(suggestedArtworkFilename(it))
                    },
                    onDelete = { deleteTarget = it },
                )
                BridgeTab.SETTINGS -> SettingsScreen(
                    settings = settings,
                    cacheBytes = cacheBytes,
                    onSave = viewModel::saveSettings,
                    onClearKey = viewModel::clearSteamGridDbKey,
                    onPrune = viewModel::pruneCache,
                )
            }
            if (busy != null) {
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(28.dp))
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 3.dp)
                    Text(busy.orEmpty())
                }
            }
        }
    }

    artworkPicker?.let { state ->
        ArtworkPickerDialog(state, viewModel::applyArtwork, viewModel::closeArtworkPicker)
    }

    deleteTarget?.let { game ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Remove ${game.title}?") },
            text = { Text("This removes only the bridge entry. The emulator game and all save data remain untouched.") },
            confirmButton = {
                Button(onClick = { viewModel.delete(game); deleteTarget = null }) { Text("Remove entry") }
            },
            dismissButton = { OutlinedButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun LibraryScreen(
    games: List<GameEntryEntity>,
    onAddAzaharRoms: () -> Unit,
    onImportArtemis: () -> Unit,
    onScanKirin: () -> Unit,
    onExportMcon: () -> Unit,
    onLaunch: (GameEntryEntity) -> Unit,
    onCopy: (GameEntryEntity) -> Unit,
    onRelinkAzahar: (GameEntryEntity) -> Unit,
    onSearchArtwork: (GameEntryEntity) -> Unit,
    onLocalArtwork: (GameEntryEntity) -> Unit,
    onSaveArtwork: (GameEntryEntity) -> Unit,
    onDelete: (GameEntryEntity) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { FilledTonalButton(onClick = onAddAzaharRoms) { Icon(Icons.Default.Add, null); Spacer(Modifier.size(8.dp)); Text("Add Azahar ROMs") } }
            item { FilledTonalButton(onClick = onImportArtemis) { Icon(Icons.Default.Download, null); Spacer(Modifier.size(8.dp)); Text("Artemis .art") } }
            item { FilledTonalButton(onClick = onScanKirin) { Icon(Icons.Default.CloudDownload, null); Spacer(Modifier.size(8.dp)); Text("Choose Kirin folder") } }
            item { Button(onClick = onExportMcon, enabled = games.isNotEmpty()) { Icon(Icons.Default.Upload, null); Spacer(Modifier.size(8.dp)); Text("Export to MCON") } }
        }
        if (games.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Default.Gamepad, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(18.dp))
                Text("Build your MCON library", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Choose ordinary Azahar ROMs, import Artemis launchers, or select any Kirin library or game folder. The bridge keeps read-only access and does not copy games or saves.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(168.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(games, key = GameEntryEntity::id) { game ->
                    GameCard(game, onLaunch, onCopy, onRelinkAzahar, onSearchArtwork, onLocalArtwork, onSaveArtwork, onDelete)
                }
            }
        }
    }
}

@Composable
private fun GameCard(
    game: GameEntryEntity,
    onLaunch: (GameEntryEntity) -> Unit,
    onCopy: (GameEntryEntity) -> Unit,
    onRelinkAzahar: (GameEntryEntity) -> Unit,
    onSearchArtwork: (GameEntryEntity) -> Unit,
    onLocalArtwork: (GameEntryEntity) -> Unit,
    onSaveArtwork: (GameEntryEntity) -> Unit,
    onDelete: (GameEntryEntity) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (game.artworkUri != null) {
            AsyncImage(
                model = game.artworkUri,
                contentDescription = "${game.title} cover",
                modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.Gamepad, null, Modifier.size(58.dp), tint = MaterialTheme.colorScheme.primary) }
        }
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(game.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
            AssistChip(onClick = {}, label = { Text(game.source.displayName()) })
            game.artworkAttribution?.let {
                Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
            }
            Button(onClick = { onLaunch(game) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.size(8.dp)); Text("Launch")
            }
            if (game.source == SourceType.AZAHAR) {
                OutlinedButton(onClick = { onRelinkAzahar(game) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.FolderOpen, null); Spacer(Modifier.size(8.dp)); Text("Choose ROM")
                }
            }
            if (game.artworkUri != null) {
                OutlinedButton(onClick = { onSaveArtwork(game) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Download, null); Spacer(Modifier.size(8.dp)); Text("Save box art")
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = { onCopy(game) }) { Icon(Icons.Default.ContentCopy, "Copy MCON link") }
                IconButton(onClick = { onSearchArtwork(game) }) { Icon(Icons.Default.ImageSearch, "Search box art") }
                IconButton(onClick = { onLocalArtwork(game) }) { Icon(Icons.Default.AddPhotoAlternate, "Choose local box art") }
                IconButton(onClick = { onDelete(game) }) { Icon(Icons.Default.Delete, "Remove bridge entry") }
            }
        }
    }
}

internal fun suggestedArtworkFilename(game: GameEntryEntity): String {
    val extension = game.artworkUri
        ?.substringAfterLast('.', "")
        ?.lowercase()
        ?.takeIf { it in setOf("png", "jpg", "jpeg", "webp", "gif") }
        ?: "jpg"
    val safeTitle = game.title
        .replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_")
        .trim(' ', '.')
        .take(120)
        .ifBlank { "game" }
    return "$safeTitle-box-art.$extension"
}

@Composable
private fun SettingsScreen(
    settings: com.vyrena.mconbridge.settings.BridgeSettings,
    cacheBytes: Long,
    onSave: (Boolean, Int, String, String?) -> Unit,
    onClearKey: () -> Unit,
    onPrune: () -> Unit,
) {
    var wifiOnly by rememberSaveable(settings.wifiOnly) { mutableStateOf(settings.wifiOnly) }
    var cacheMb by rememberSaveable(settings.artworkCacheMb) { mutableIntStateOf(settings.artworkCacheMb) }
    var regions by rememberSaveable(settings.gameTdbRegions) { mutableStateOf(settings.gameTdbRegions.joinToString(",")) }
    var apiKey by rememberSaveable { mutableStateOf("") }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { Text("Artwork downloads", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Wi-Fi only", fontWeight = FontWeight.Bold)
                    Text("Prevents online cover searches and downloads on cellular data.", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = wifiOnly, onCheckedChange = { wifiOnly = it })
            }
        }
        item {
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("SteamGridDB API key") },
                placeholder = { Text(if (settings.hasSteamGridDbKey) "A key is saved securely" else "Required for SteamGridDB") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
            )
        }
        if (settings.hasSteamGridDbKey) {
            item { OutlinedButton(onClick = onClearKey) { Text("Remove saved SteamGridDB key") } }
        }
        item {
            OutlinedTextField(
                value = regions,
                onValueChange = { regions = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("GameTDB region order") },
                supportingText = { Text("Comma-separated, for example US,EN,JA") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                singleLine = true,
            )
        }
        item {
            OutlinedTextField(
                value = cacheMb.toString(),
                onValueChange = { cacheMb = it.toIntOrNull()?.coerceIn(32, 2048) ?: cacheMb },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Artwork cache limit (MB)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onSave(wifiOnly, cacheMb, regions, apiKey.takeIf(String::isNotBlank)) }) { Text("Save settings") }
                OutlinedButton(onClick = onPrune) { Text("Prune cache") }
            }
        }
        item { Text("Current cache: ${NumberFormat.getIntegerInstance().format(cacheBytes / 1024)} KB") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Save-data safety", fontWeight = FontWeight.Bold)
                    Text("MCON Bridge never writes emulator saves. Azahar ROM access is read-only and is forwarded only to the ordinary Azahar app when you launch a game.")
                }
            }
        }
    }
}

@Composable
private fun ArtworkPickerDialog(
    state: ArtworkPickerState,
    onSelect: (ArtworkCandidate) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Box art for ${state.game.title}") },
        text = {
            when {
                state.loading -> Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.error != null && state.candidates.isEmpty() -> Text(state.error)
                else -> LazyColumn(Modifier.fillMaxWidth().height(420.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.candidates, key = ArtworkCandidate::id) { candidate ->
                        Card(onClick = { onSelect(candidate) }, modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = candidate.thumbnailUrl ?: candidate.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(74.dp, 110.dp),
                                    contentScale = ContentScale.Crop,
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(candidate.title, fontWeight = FontWeight.Bold, maxLines = 2)
                                    Text(candidate.provider, color = MaterialTheme.colorScheme.primary)
                                    Text("Match ${(candidate.confidence * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                                    Text(candidate.attribution, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Close") } },
    )
}

private fun SourceType.displayName() = name.lowercase().replaceFirstChar(Char::uppercase)
