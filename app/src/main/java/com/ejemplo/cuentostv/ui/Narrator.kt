package com.ejemplo.cuentostv.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

data class TtsVoice(
    val name: String,
    val label: String,
    val online: Boolean
)

class Narrator(context: Context) {

    var isAvailable by mutableStateOf(true)
        private set

    var voices by mutableStateOf<List<TtsVoice>>(emptyList())
        private set

    var activeVoiceLabel by mutableStateOf("Automática")
        private set

    private var tts: TextToSpeech? = null
    private var ready = false
    private var pendingText: String? = null
    private var requestedVoice: String? = null
    private var requestedRate: Float = 1f
    private var voiceApplied = false

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
            loadVoices(engine)
            engine.setSpeechRate(requestedRate)
            applyVoice()

            pendingText?.let { text ->
                pendingText = null
                speak(text)
            }
        }
    }

    fun configure(voiceName: String?, speechRate: Float) {
        val voiceChanged = voiceName != requestedVoice
        requestedVoice = voiceName
        requestedRate = speechRate

        val engine = tts ?: return
        runCatching { engine.setSpeechRate(speechRate) }
        if (voiceChanged || !voiceApplied) {
            applyVoice()
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        if (!ready) {
            pendingText = text
            return
        }
        tts?.stop()
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cuento")
        if (result == TextToSpeech.ERROR) {
            isAvailable = false
        }
    }

    fun speakSample() {
        speak("Hola. Así se leerán los cuentos de CuentosTV.")
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

    private fun loadVoices(engine: TextToSpeech) {
        val spanishVoices = runCatching { engine.voices.orEmpty() }
            .getOrDefault(emptySet())
            .filter { it.locale.language.equals("es", ignoreCase = true) }
            .distinctBy { it.name }
            .sortedWith(
                compareBy(
                    { it.isNetworkConnectionRequired },
                    { -it.quality },
                    { it.name }
                )
            )

        val counters = mutableMapOf<String, Int>()
        voices = spanishVoices.map { voice ->
            val language = voice.locale.getDisplayName(Locale("es"))
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es")) else it.toString() }
            val index = (counters[language] ?: 0) + 1
            counters[language] = index
            TtsVoice(
                name = voice.name,
                label = "$language · Voz $index",
                online = voice.isNetworkConnectionRequired
            )
        }

        if (requestedVoice != null && voices.none { it.name == requestedVoice }) {
            requestedVoice = null
        }
    }

    private fun applyVoice() {
        val engine = tts ?: return
        val voice: Voice? = requestedVoice?.let { name ->
            runCatching { engine.voices?.firstOrNull { it.name == name } }.getOrNull()
        }

        runCatching {
            engine.voice = voice ?: engine.defaultVoice
        }

        activeVoiceLabel = voice
            ?.let { selected -> voices.firstOrNull { it.name == selected.name }?.label }
            ?: "Automática"
        voiceApplied = true
    }
}
