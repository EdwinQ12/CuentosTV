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

data class AppSettings(
    val narrationEnabled: Boolean = true,
    val textScale: TextScale = TextScale.NORMAL
)

class SettingsRepository(private val context: Context) {

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { preferences ->
        AppSettings(
            narrationEnabled = preferences[NARRATION] ?: true,
            textScale = runCatching {
                TextScale.valueOf(preferences[TEXT_SCALE] ?: TextScale.NORMAL.name)
            }.getOrDefault(TextScale.NORMAL)
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

    private companion object {
        val NARRATION = booleanPreferencesKey("narration_enabled")
        val TEXT_SCALE = stringPreferencesKey("text_scale")
    }
}
