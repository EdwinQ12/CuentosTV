package com.ejemplo.cuentostv.remote

import com.ejemplo.cuentostv.ui.CuentosUiState
import com.ejemplo.cuentostv.ui.Narrator
import com.ejemplo.cuentostv.ui.Screen
import com.ejemplo.cuentostv.ui.StoryAudioPlayer

fun buildRemoteState(
    state: CuentosUiState,
    narrator: Narrator,
    audioPlayer: StoryAudioPlayer
): RemoteState {
    val node = state.currentNode
    val item = state.currentItem

    return RemoteState(
        screen = when (state.screen) {
            Screen.Home -> "home"
            is Screen.Detail -> "detail"
            is Screen.Reader -> "reader"
            Screen.Settings -> "settings"
            Screen.Voices -> "voices"
            Screen.Pairing -> "pairing"
        },
        storyId = item?.summary?.id,
        title = item?.summary?.title,
        description = item?.summary?.description,
        text = node?.text,
        endingTitle = node?.endingTitle,
        ending = node?.ending == true,
        choices = node?.choices?.map { it.text } ?: emptyList(),
        stories = if (state.screen == Screen.Home) {
            state.stories.map { story ->
                RemoteStory(
                    id = story.summary.id,
                    title = story.summary.title,
                    description = story.summary.description,
                    age = story.summary.age
                )
            }
        } else {
            emptyList()
        },
        hasProgress = item != null && state.progress[item.summary.id] != null,
        narrationEnabled = state.settings.narrationEnabled,
        textScale = if (state.settings.textScale.name == "LARGE") "grande" else "normal",
        speechRate = state.settings.speechRate.label,
        voiceLabel = narrator.activeVoiceLabel,
        audio = node?.audio?.let {
            RemoteAudio(
                available = audioPlayer.hasTrack,
                playing = audioPlayer.isPlaying,
                paused = audioPlayer.isPaused
            )
        },
        message = state.message
    )
}
