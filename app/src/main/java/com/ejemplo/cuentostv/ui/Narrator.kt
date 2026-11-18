package com.ejemplo.cuentostv.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

class Narrator(context: Context) {

    var isAvailable by mutableStateOf(true)
        private set

    private var tts: TextToSpeech? = null
    private var ready = false
    private var pendingText: String? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status != TextToSpeech.SUCCESS) {
                isAvailable = false
                return@TextToSpeech
            }

            val engine = tts ?: return@TextToSpeech
            val spanish = engine.setLanguage(Locale("es", "ES"))
            if (spanish == TextToSpeech.LANG_MISSING_DATA ||
                spanish == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                val fallback = engine.setLanguage(Locale("es"))
                isAvailable = fallback != TextToSpeech.LANG_MISSING_DATA &&
                    fallback != TextToSpeech.LANG_NOT_SUPPORTED
            } else {
                isAvailable = true
            }

            ready = true
            pendingText?.let { text ->
                pendingText = null
                speak(text)
            }
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        pendingText = text
        if (!ready) return
        pendingText = null
        tts?.stop()
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cuento")
        if (result == TextToSpeech.ERROR) {
            isAvailable = false
        }
    }

    fun stop() {
        pendingText = null
        tts?.stop()
    }

    fun shutdown() {
        pendingText = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
