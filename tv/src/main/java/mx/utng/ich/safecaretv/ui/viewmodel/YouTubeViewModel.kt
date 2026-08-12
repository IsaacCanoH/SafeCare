package mx.utng.ich.safecaretv.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mx.utng.ich.safecaretv.data.youtube.YouTubeRepository
import mx.utng.ich.safecaretv.data.youtube.YouTubeVideo

sealed interface YouTubeUiState {
    data object Loading : YouTubeUiState
    data class Content(val videos: List<YouTubeVideo>) : YouTubeUiState
    data class Error(val message: String) : YouTubeUiState
}

class YouTubeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = YouTubeRepository(application)
    private val _state = MutableStateFlow<YouTubeUiState>(YouTubeUiState.Loading)
    val state: StateFlow<YouTubeUiState> = _state.asStateFlow()

    init {
        loadRecommendations()
    }

    // Carga las recomendaciones de video para la pantalla principal.
    fun loadRecommendations() {
        viewModelScope.launch {
            _state.value = YouTubeUiState.Loading
            runCatching { repository.getCareRecommendations() }
                .onSuccess { videos ->
                    _state.value = if (videos.isEmpty()) {
                        YouTubeUiState.Error("YouTube no encontró recomendaciones disponibles")
                    } else {
                        YouTubeUiState.Content(videos)
                    }
                }
                .onFailure { error ->
                    _state.value = YouTubeUiState.Error(
                        error.message ?: "No se pudieron cargar las recomendaciones"
                    )
                }
        }
    }

    // Libera recursos al destruir el ViewModel.
    override fun onCleared() {
        repository.close()
        super.onCleared()
    }
}
