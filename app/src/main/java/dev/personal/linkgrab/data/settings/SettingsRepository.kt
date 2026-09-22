package dev.personal.linkgrab.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dev.personal.linkgrab.model.AppSettings
import dev.personal.linkgrab.model.AudioFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

class SettingsRepository(private val context: Context) {
    private val audio = stringPreferencesKey("audio_format")
    private val quality = intPreferencesKey("quality")
    private val ytdlpUpdates = booleanPreferencesKey("ytdlp_updates")
    private val appUpdates = booleanPreferencesKey("app_updates")
    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            audioFormat = runCatching { AudioFormat.valueOf(prefs[audio] ?: "MP3") }.getOrDefault(AudioFormat.MP3),
            qualityCeiling = prefs[quality] ?: 720,
            autoUpdateYtDlp = prefs[ytdlpUpdates] ?: true,
            autoUpdateApp = prefs[appUpdates] ?: true
        )
    }
    suspend fun setAudioFormat(value: AudioFormat) = context.dataStore.edit { it[audio] = value.name }
    suspend fun setQuality(value: Int) = context.dataStore.edit { it[quality] = value }
    suspend fun setAutoYtDlpUpdates(value: Boolean) = context.dataStore.edit { it[ytdlpUpdates] = value }
    suspend fun setAutoAppUpdates(value: Boolean) = context.dataStore.edit { it[appUpdates] = value }
}
