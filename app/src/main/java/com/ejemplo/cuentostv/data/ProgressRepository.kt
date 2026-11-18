package com.ejemplo.cuentostv.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.progressDataStore by preferencesDataStore(name = "progress")

class ProgressRepository(private val context: Context) {

    val progressFlow: Flow<Map<String, String>> = context.progressDataStore.data.map { preferences ->
        preferences.asMap()
            .mapNotNull { (key, value) ->
                if (key.name.startsWith("progress_") && value is String) {
                    key.name.removePrefix("progress_") to value
                } else {
                    null
                }
            }
            .toMap()
    }

    val lastStoryFlow: Flow<String?> = context.progressDataStore.data.map { preferences ->
        preferences[LAST_STORY]
    }

    suspend fun saveProgress(storyId: String, nodeId: String) {
        context.progressDataStore.edit { preferences ->
            preferences[stringPreferencesKey("progress_$storyId")] = nodeId
        }
    }

    suspend fun setLastStory(storyId: String) {
        context.progressDataStore.edit { preferences ->
            preferences[LAST_STORY] = storyId
        }
    }

    suspend fun clearStory(storyId: String) {
        context.progressDataStore.edit { preferences ->
            preferences.remove(stringPreferencesKey("progress_$storyId"))
            if (preferences[LAST_STORY] == storyId) {
                preferences.remove(LAST_STORY)
            }
        }
    }

    private companion object {
        val LAST_STORY = stringPreferencesKey("last_story")
    }
}
