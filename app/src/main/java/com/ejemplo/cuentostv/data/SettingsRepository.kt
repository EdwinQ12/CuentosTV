package com.ejemplo.cuentostv.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore by preferencesDataStore(name = "settings")

enum class TextScale {
    NORMAL,
    LARGE
}

enum class SpeechRate(val value: Float, val label: String) {
    SLOW(0.85f, "Lenta"),
    NORMAL(1.0f, "Normal"),
    FAST(1.15f, "Rápida")
}

data class AppSettings(
    val narrationEnabled: Boolean = true,
    val textScale: TextScale = TextScale.NORMAL,
    val voiceName: String? = null,
    val speechRate: SpeechRate = SpeechRate.NORMAL
)

class SettingsRepository(private val context: Context) {

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { preferences ->
        AppSettings(
            narrationEnabled = preferences[NARRATION] ?: true,
            textScale = runCatching {
                TextScale.valueOf(preferences[TEXT_SCALE] ?: TextScale.NORMAL.name)
            }.getOrDefault(TextScale.NORMAL),
            voiceName = preferences[VOICE],
            speechRate = runCatching {
                SpeechRate.valueOf(preferences[SPEECH_RATE] ?: SpeechRate.NORMAL.name)
            }.getOrDefault(SpeechRate.NORMAL)
        )
    }

    suspend fun setNarrationEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[NARRATION] = enabled
        }
    }

    suspend fun setTextScale(scale: TextScale) {
        context.settingsDataStore.edit { preferences ->
            preferences[TEXT_SCALE] = scale.name
        }
    }

    suspend fun setVoice(name: String?) {
        context.settingsDataStore.edit { preferences ->
            if (name == null) {
                preferences.remove(VOICE)
            } else {
                preferences[VOICE] = name
            }
        }
    }

    suspend fun setSpeechRate(rate: SpeechRate) {
        context.settingsDataStore.edit { preferences ->
            preferences[SPEECH_RATE] = rate.name
        }
    }

    private companion object {
        val NARRATION = booleanPreferencesKey("narration_enabled")
        val TEXT_SCALE = stringPreferencesKey("text_scale")
        val VOICE = stringPreferencesKey("voice_name")
        val SPEECH_RATE = stringPreferencesKey("speech_rate")
    }
}
