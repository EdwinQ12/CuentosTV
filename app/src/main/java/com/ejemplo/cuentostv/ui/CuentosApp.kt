package com.ejemplo.cuentostv.ui

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import com.ejemplo.cuentostv.remote.RemoteRequest
import com.ejemplo.cuentostv.remote.RemoteServer
import com.ejemplo.cuentostv.remote.buildRemoteState
import com.ejemplo.cuentostv.ui.screens.DetailScreen
import com.ejemplo.cuentostv.ui.screens.HomeScreen
import com.ejemplo.cuentostv.ui.screens.PairingScreen
import com.ejemplo.cuentostv.ui.screens.ReaderScreen
import com.ejemplo.cuentostv.ui.screens.SettingsScreen
import com.ejemplo.cuentostv.ui.screens.VoicesScreen
import com.ejemplo.cuentostv.ui.theme.CuentosTvTheme
import com.ejemplo.cuentostv.ui.theme.TvSurface
import kotlinx.coroutines.delay

@Composable
fun CuentosApp() {
    val context = LocalContext.current
    val viewModel: CuentosViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val narrator = remember { Narrator(context) }
    val audioPlayer = remember { StoryAudioPlayer(context) }
    val remoteServer = remember {
        RemoteServer(
            context = context.applicationContext,
            stateProvider = { buildRemoteState(viewModel.uiState.value, narrator, audioPlayer) },
            actionHandler = { request ->
                Handler(Looper.getMainLooper()).post {
                    handleRemoteAction(request, viewModel, audioPlayer)
                }
            }
        )
    }
    val remoteInfo by remoteServer.info.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        remoteServer.start()
        onDispose {
            remoteServer.stop()
            narrator.shutdown()
            audioPlayer.release()
        }
    }

    LaunchedEffect(state.settings.voiceName, state.settings.speechRate) {
        narrator.configure(
            voiceName = state.settings.voiceName,
            speechRate = state.settings.speechRate.value
        )
    }

    BackHandler(enabled = state.screen != Screen.Home) {
        viewModel.back()
    }

    LaunchedEffect(state.message) {
        if (state.message != null) {
            delay(5_000)
            viewModel.clearMessage()
        }
    }

    CuentosTvTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            when (val screen = state.screen) {
                Screen.Home -> HomeScreen(
                    state = state,
                    loadImage = viewModel::loadImage,
                    onStory = { storyId -> viewModel.openStory(storyId) },
                    onContinue = viewModel::continueLastStory,
                    onRefresh = viewModel::refreshRemote,
                    onSettings = viewModel::openSettings
                )

                is Screen.Detail -> {
                    if (state.currentItem != null) {
                        DetailScreen(
                            state = state,
                            loadImage = viewModel::loadImage,
                            onStart = { viewModel.startStory(fromBeginning = false) },
                            onRestart = { viewModel.startStory(fromBeginning = true) },
                            onBack = viewModel::back
                        )
                    } else {
                        HomeScreen(
                            state = state,
                            loadImage = viewModel::loadImage,
                            onStory = { storyId -> viewModel.openStory(storyId) },
                            onContinue = viewModel::continueLastStory,
                            onRefresh = viewModel::refreshRemote,
                            onSettings = viewModel::openSettings
                        )
                    }
                }

                is Screen.Reader -> {
                    if (state.currentItem != null) {
                        ReaderScreen(
                            state = state,
                            narrator = narrator,
                            audioPlayer = audioPlayer,
                            loadImage = viewModel::loadImage,
                            loadAudio = viewModel::loadAudio,
                            onChoose = viewModel::choose,
                            onRestart = viewModel::restartStory,
                            onHome = viewModel::goHome,
                            onBack = viewModel::back,
                            onToggleNarration = viewModel::toggleNarration
                        )
                    } else {
                        HomeScreen(
                            state = state,
                            loadImage = viewModel::loadImage,
                            onStory = { storyId -> viewModel.openStory(storyId) },
                            onContinue = viewModel::continueLastStory,
                            onRefresh = viewModel::refreshRemote,
                            onSettings = viewModel::openSettings
                        )
                    }
                }

                Screen.Settings -> SettingsScreen(
                    settings = state.settings,
                    voiceLabel = narrator.activeVoiceLabel,
                    onToggleNarration = viewModel::toggleNarration,
                    onOpenVoices = viewModel::openVoices,
                    onCycleRate = viewModel::cycleSpeechRate,
                    onTestVoice = narrator::speakSample,
                    onToggleTextScale = viewModel::toggleTextScale,
                    onOpenPairing = viewModel::openPairing,
                    onBack = viewModel::back
                )

                Screen.Voices -> VoicesScreen(
                    voices = narrator.voices,
                    selected = state.settings.voiceName,
                    onSelect = viewModel::selectVoice,
                    onTest = narrator::speakSample,
                    onSystemSettings = {
                        val opened = runCatching {
                            context.startActivity(
                                Intent("com.android.settings.TTS_SETTINGS")
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }.isSuccess
                        if (!opened) {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                            Toast.makeText(
                                context,
                                "Abre Ajustes > Sistema > Idiomas > Salida de texto a voz",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    onBack = viewModel::back
                )

                Screen.Pairing -> PairingScreen(
                    info = remoteInfo,
                    onBack = viewModel::back
                )
            }

            state.message?.let { message ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(24.dp)
                        .fillMaxWidth(0.75f)
                        .background(
                            color = TvSurface.copy(alpha = 0.95f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(horizontal = 22.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = message,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

private fun handleRemoteAction(
    request: RemoteRequest,
    viewModel: CuentosViewModel,
    audioPlayer: StoryAudioPlayer
) {
    when (request.action) {
        "open_story" -> request.storyId?.let(viewModel::openStory)
        "start_new" -> viewModel.startStory(fromBeginning = true)
        "continue" -> viewModel.startStory(fromBeginning = false)
        "choose" -> {
            val index = request.index ?: return
            val node = viewModel.uiState.value.currentNode
            node?.choices?.getOrNull(index)?.let { viewModel.choose(it.next) }
        }
        "back" -> viewModel.back()
        "home" -> viewModel.goHome()
        "restart" -> viewModel.restartStory()
        "settings" -> viewModel.openSettings()
        "toggle_narration" -> viewModel.toggleNarration()
        "toggle_text" -> viewModel.toggleTextScale()
        "cycle_rate" -> viewModel.cycleSpeechRate()
        "audio_toggle" -> audioPlayer.toggle()
    }
}
