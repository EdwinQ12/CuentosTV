package com.ejemplo.cuentostv.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ejemplo.cuentostv.data.AppSettings
import com.ejemplo.cuentostv.data.AudioSource
import com.ejemplo.cuentostv.data.ProgressRepository
import com.ejemplo.cuentostv.data.SpeechRate
import com.ejemplo.cuentostv.data.SettingsRepository
import com.ejemplo.cuentostv.data.Story
import com.ejemplo.cuentostv.data.StoryItem
import com.ejemplo.cuentostv.data.StoryNode
import com.ejemplo.cuentostv.data.StoryRepository
import com.ejemplo.cuentostv.data.StoryTheme
import com.ejemplo.cuentostv.data.TextScale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Home : Screen
    data class Detail(val storyId: String) : Screen
    data class Reader(val storyId: String) : Screen
    data object Settings : Screen
    data object Voices : Screen
    data object Pairing : Screen
}

data class CuentosUiState(
    val screen: Screen = Screen.Home,
    val stories: List<StoryItem> = emptyList(),
    val progress: Map<String, String> = emptyMap(),
    val lastStoryId: String? = null,
    val currentItem: StoryItem? = null,
    val story: Story? = null,
    val nodeId: String? = null,
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val message: String? = null,
    val remoteAvailable: Boolean = false
) {
    val currentNode: StoryNode?
        get() = story?.nodes?.get(nodeId)

    val isEnding: Boolean
        get() = currentNode?.ending == true

    val currentTheme: StoryTheme
        get() = currentNode?.theme
            ?: currentItem?.summary?.theme
            ?: StoryTheme.FOREST
}

class CuentosViewModel(application: Application) : AndroidViewModel(application) {

    private val storyRepository = StoryRepository(application)
    private val progressRepository = ProgressRepository(application)
    private val settingsRepository = SettingsRepository(application)

    private val _uiState = MutableStateFlow(CuentosUiState())
    val uiState: StateFlow<CuentosUiState> = _uiState.asStateFlow()

    init {
        loadInitialBooks()
        observeProgress()
        observeSettings()
    }

    private fun loadInitialBooks() {
        viewModelScope.launch {
            val items = storyRepository.loadInitialItems()
            _uiState.update {
                it.copy(
                    stories = items,
                    isLoading = false,
                    remoteAvailable = items.any { item -> item.remote },
                    message = if (items.isEmpty()) {
                        "No se encontraron cuentos. Revisa los archivos locales."
                    } else {
                        it.message
                    }
                )
            }
        }
    }

    private fun observeProgress() {
        viewModelScope.launch {
            progressRepository.progressFlow.collect { progress ->
                _uiState.update { it.copy(progress = progress) }
            }
        }
        viewModelScope.launch {
            progressRepository.lastStoryFlow.collect { lastStory ->
                _uiState.update { it.copy(lastStoryId = lastStory) }
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun openStory(storyId: String, autoStart: Boolean = false) {
        val item = _uiState.value.stories.firstOrNull { it.summary.id == storyId } ?: return
        _uiState.update {
            it.copy(
                screen = Screen.Detail(storyId),
                currentItem = item,
                story = null,
                nodeId = null,
                isLoading = true,
                message = null
            )
        }

        viewModelScope.launch {
            val story = storyRepository.loadStory(item)
            if (story == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "No se pudo abrir el cuento. Revisa el archivo JSON."
                    )
                }
                return@launch
            }

            val savedNode = _uiState.value.progress[storyId]
            val startNode = savedNode?.takeIf { it in story.nodes } ?: story.start
            val nodeId = if (autoStart) startNode else null

            _uiState.update {
                it.copy(
                    story = story,
                    nodeId = nodeId,
                    isLoading = false,
                    screen = if (autoStart) Screen.Reader(storyId) else Screen.Detail(storyId)
                )
            }

            if (autoStart) {
                progressRepository.saveProgress(storyId, startNode)
                progressRepository.setLastStory(storyId)
            }
        }
    }

    fun startStory(fromBeginning: Boolean) {
        val state = _uiState.value
        val item = state.currentItem ?: return
        val story = state.story ?: return
        val savedNode = state.progress[item.summary.id]
        val startNode = if (!fromBeginning && savedNode != null && savedNode in story.nodes) {
            savedNode
        } else {
            story.start
        }

        _uiState.update {
            it.copy(
                screen = Screen.Reader(item.summary.id),
                nodeId = startNode,
                message = null
            )
        }
        recordProgress(story, item.summary.id, startNode)
    }

    fun choose(nextNode: String) {
        val state = _uiState.value
        val story = state.story ?: return
        val item = state.currentItem ?: return
        if (nextNode !in story.nodes) return

        _uiState.update { it.copy(nodeId = nextNode, message = null) }
        recordProgress(story, item.summary.id, nextNode)
    }

    private fun recordProgress(story: Story, storyId: String, nodeId: String) {
        if (story.nodes[nodeId]?.ending == true) {
            viewModelScope.launch {
                progressRepository.clearStory(storyId)
                progressRepository.setLastStory(storyId)
            }
        } else {
            viewModelScope.launch {
                progressRepository.saveProgress(storyId, nodeId)
                progressRepository.setLastStory(storyId)
            }
        }
    }

    fun restartStory() {
        startStory(fromBeginning = true)
    }

    fun continueLastStory() {
        val storyId = _uiState.value.lastStoryId ?: return
        openStory(storyId, autoStart = true)
    }

    fun openSettings() {
        _uiState.update { it.copy(screen = Screen.Settings, message = null) }
    }

    fun openVoices() {
        _uiState.update { it.copy(screen = Screen.Voices, message = null) }
    }

    fun openPairing() {
        _uiState.update { it.copy(screen = Screen.Pairing, message = null) }
    }

    fun toggleNarration() {
        val newValue = !_uiState.value.settings.narrationEnabled
        viewModelScope.launch {
            settingsRepository.setNarrationEnabled(newValue)
        }
    }

    fun toggleTextScale() {
        val current = _uiState.value.settings.textScale
        val next = if (current == TextScale.NORMAL) TextScale.LARGE else TextScale.NORMAL
        viewModelScope.launch {
            settingsRepository.setTextScale(next)
        }
    }

    fun selectVoice(name: String?) {
        viewModelScope.launch {
            settingsRepository.setVoice(name)
        }
    }

    fun cycleSpeechRate() {
        val rates = SpeechRate.entries
        val current = _uiState.value.settings.speechRate
        val next = rates[(current.ordinal + 1) % rates.size]
        viewModelScope.launch {
            settingsRepository.setSpeechRate(next)
        }
    }

    suspend fun loadImage(item: StoryItem, name: String): Bitmap? =
        storyRepository.loadImage(item, name)

    suspend fun loadAudio(item: StoryItem, name: String): AudioSource? =
        storyRepository.audioSource(item, name)

    fun refreshRemote() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true, message = null) }
        viewModelScope.launch {
            val result = storyRepository.refreshRemote()
            val items = storyRepository.loadInitialItems()
            _uiState.update {
                it.copy(
                    stories = items,
                    isRefreshing = false,
                    remoteAvailable = items.any { story -> story.remote },
                    message = result.message
                )
            }
        }
    }

    fun back() {
        val screen = _uiState.value.screen
        _uiState.update {
            when (screen) {
                is Screen.Reader -> it.copy(screen = Screen.Detail(screen.storyId))
                is Screen.Detail -> it.copy(
                    screen = Screen.Home,
                    currentItem = null,
                    story = null,
                    nodeId = null
                )
                Screen.Settings -> it.copy(screen = Screen.Home)
                Screen.Voices -> it.copy(screen = Screen.Settings)
                Screen.Pairing -> it.copy(screen = Screen.Settings)
                Screen.Home -> it
            }
        }
    }

    fun goHome() {
        _uiState.update {
            it.copy(
                screen = Screen.Home,
                currentItem = null,
                story = null,
                nodeId = null,
                message = null
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
