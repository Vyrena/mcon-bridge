package com.vyrena.mconbridge.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("bridge_settings")

data class BridgeSettings(
    val wifiOnly: Boolean = true,
    val artworkCacheMb: Int = 256,
    val gameTdbRegions: List<String> = listOf("US", "EN"),
    val hasSteamGridDbKey: Boolean = false,
)

class SettingsRepository(private val context: Context) {
    private val cipher = SecretCipher()

    val settings: Flow<BridgeSettings> = context.dataStore.data.map { preferences ->
        BridgeSettings(
            wifiOnly = preferences[WIFI_ONLY] ?: true,
            artworkCacheMb = (preferences[CACHE_MB] ?: 256).coerceIn(32, 2048),
            gameTdbRegions = preferences[GAMETDB_REGIONS]
                ?.split(',')?.map(String::trim)?.filter(String::isNotEmpty)
                ?.takeIf(List<String>::isNotEmpty) ?: listOf("US", "EN"),
            hasSteamGridDbKey = !preferences[STEAMGRIDDB_KEY].isNullOrBlank(),
        )
    }

    suspend fun setWifiOnly(value: Boolean) = context.dataStore.edit { it[WIFI_ONLY] = value }

    suspend fun setArtworkCacheMb(value: Int) = context.dataStore.edit { it[CACHE_MB] = value.coerceIn(32, 2048) }

    suspend fun setGameTdbRegions(regions: List<String>) = context.dataStore.edit {
        it[GAMETDB_REGIONS] = regions.map(String::uppercase).distinct().joinToString(",")
    }

    suspend fun setSteamGridDbKey(value: String) = context.dataStore.edit { preferences ->
        if (value.isBlank()) preferences.remove(STEAMGRIDDB_KEY)
        else preferences[STEAMGRIDDB_KEY] = cipher.encrypt(value.trim())
    }

    suspend fun steamGridDbKey(): String? = context.dataStore.data.first()[STEAMGRIDDB_KEY]?.let(cipher::decrypt)

    companion object {
        private val WIFI_ONLY = booleanPreferencesKey("artwork_wifi_only")
        private val CACHE_MB = intPreferencesKey("artwork_cache_mb")
        private val GAMETDB_REGIONS = stringPreferencesKey("gametdb_regions")
        private val STEAMGRIDDB_KEY = stringPreferencesKey("steamgriddb_key_encrypted")
    }
}
