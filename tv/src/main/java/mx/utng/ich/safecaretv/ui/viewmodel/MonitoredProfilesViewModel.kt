package mx.utng.ich.safecaretv.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mx.utng.ich.safecaretv.data.profile.MonitoredProfile
import mx.utng.ich.safecaretv.data.profile.MonitoredProfilesRepository

sealed interface ProfilesUiState {
    data object Loading : ProfilesUiState
    data class Content(val profiles: List<MonitoredProfile>) : ProfilesUiState
    data class Error(val message: String) : ProfilesUiState
}

class MonitoredProfilesViewModel(
    private val repository: MonitoredProfilesRepository = MonitoredProfilesRepository()
) : ViewModel() {
    private val _state = MutableStateFlow<ProfilesUiState>(ProfilesUiState.Loading)
    val state: StateFlow<ProfilesUiState> = _state.asStateFlow()

    init {
        loadProfiles()
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                refreshProfiles(showLoading = false)
            }
        }
    }

    fun loadProfiles() {
        refreshProfiles(showLoading = true)
    }

    private fun refreshProfiles(showLoading: Boolean) {
        viewModelScope.launch {
            if (showLoading) {
                _state.value = ProfilesUiState.Loading
            }
            runCatching { repository.getProfiles() }
                .onSuccess { _state.value = ProfilesUiState.Content(it) }
                .onFailure {
                    Log.e("TvProfiles", "Error loading profiles from Supabase", it)
                    if (showLoading || _state.value !is ProfilesUiState.Content) {
                        _state.value = ProfilesUiState.Error(
                            "No se pudieron cargar los datos de Supabase: " +
                                (it.message ?: "error desconocido")
                        )
                    }
                }
        }
    }
}
