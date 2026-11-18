package com.ejemplo.cuentostv.remote

import kotlinx.serialization.Serializable

@Serializable
data class RemoteStory(
    val id: String,
    val title: String,
    val description: String,
    val age: String
)

@Serializable
data class RemoteAudio(
    val available: Boolean,
    val playing: Boolean,
    val paused: Boolean
)

@Serializable
data class RemoteState(
    val screen: String,
    val storyId: String? = null,
    val title: String? = null,
    val description: String? = null,
    val text: String? = null,
    val endingTitle: String? = null,
    val ending: Boolean = false,
    val choices: List<String> = emptyList(),
    val stories: List<RemoteStory> = emptyList(),
    val hasProgress: Boolean = false,
    val narrationEnabled: Boolean = true,
    val textScale: String = "normal",
    val speechRate: String = "Normal",
    val voiceLabel: String = "Automática",
    val audio: RemoteAudio? = null,
    val message: String? = null
)

@Serializable
data class RemoteRequest(
    val token: String = "",
    val action: String = "",
    val index: Int? = null,
    val storyId: String? = null,
    val code: String = ""
)

@Serializable
data class PairResponse(val token: String)

@Serializable
data class OkResponse(val ok: Boolean = true)

@Serializable
data class ErrorResponse(val error: String)
