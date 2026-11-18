package com.ejemplo.cuentostv.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ejemplo.cuentostv.data.AudioSource
import java.io.File

class StoryAudioPlayer(private val context: Context) {

    var isPlaying by mutableStateOf(false)
        private set

    var isPaused by mutableStateOf(false)
        private set

    var hasTrack by mutableStateOf(false)
        private set

    private var player: MediaPlayer? = null
    private var lastSource: AudioSource? = null

    fun play(source: AudioSource, onComplete: () -> Unit = {}) {
        stop()
        lastSource = source

        val media = MediaPlayer()
        media.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )

        val prepared = runCatching {
            when (source) {
                is AudioSource.Asset -> media.setDataSource(assetFile(source.path).absolutePath)
                is AudioSource.LocalFile -> media.setDataSource(source.file.absolutePath)
            }
            media.setOnCompletionListener {
                isPlaying = false
                isPaused = false
                onComplete()
            }
            media.setOnErrorListener { _, _, _ ->
                isPlaying = false
                isPaused = false
                hasTrack = false
                true
            }
            media.prepare()
            media.start()
        }.isSuccess

        if (!prepared) {
            media.release()
            hasTrack = false
            return
        }

        player = media
        hasTrack = true
        isPlaying = true
        isPaused = false
    }

    fun toggle() {
        when {
            isPlaying -> pause()
            isPaused -> resume()
            else -> lastSource?.let { play(it) }
        }
    }

    fun pause() {
        if (!isPlaying) return
        runCatching { player?.pause() }
        isPlaying = false
        isPaused = true
    }

    fun resume() {
        if (!isPaused) return
        runCatching { player?.start() }
        isPlaying = true
        isPaused = false
    }

    fun stop() {
        player?.let { media ->
            runCatching { if (media.isPlaying) media.stop() }
            media.release()
        }
        player = null
        isPlaying = false
        isPaused = false
        hasTrack = false
    }

    fun release() {
        stop()
        lastSource = null
    }

    private fun assetFile(path: String): File {
        val name = path.substringAfterLast('/')
        val cache = File(context.cacheDir, "narration_${name.hashCode()}_$name")
        if (!cache.exists() || cache.length() == 0L) {
            context.assets.open(path).use { input ->
                cache.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return cache
    }
}
