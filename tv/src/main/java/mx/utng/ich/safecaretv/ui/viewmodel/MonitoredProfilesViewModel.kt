package mx.utng.ich.safecaretv.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecordOrNull
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mx.utng.ich.safecaretv.data.profile.MonitoredProfilesRepository
import mx.utng.ich.safecaretv.data.remote.TvSupabaseClient

sealed interface ProfilesUiState {
    data object Loading : ProfilesUiState
    /**
     * Estado de UI que representa el contenido cargado exitosamente.
     */
    data class Content(val profiles: List<mx.utng.ich.safecaretv.data.profile.MonitoredProfile>) : ProfilesUiState
    /**
     * Estado de UI que representa un error ocurrido durante la carga de datos.
     */
    data class Error(val message: String) : ProfilesUiState
}

/**
 * Componente arquitectÃ³nico (ViewModel) que maneja la lÃ³gica de negocio para la lista interactiva de perfiles.
 *  * Retiene el estado de la vista durante los cambios de configuraciÃ³n y reacciona a los flujos de datos provenientes del repositorio.
 */
class MonitoredProfilesViewModel(
    private val repository: MonitoredProfilesRepository = MonitoredProfilesRepository()
) : ViewModel() {
    private val _state = MutableStateFlow<ProfilesUiState>(ProfilesUiState.Loading)
    val state: StateFlow<ProfilesUiState> = _state.asStateFlow()
    private var realtimeJob: Job? = null

    init {
        loadProfiles()
        viewModelScope.launch {
            // Respaldo: mantiene perfiles/zonas/estado correctos si se perdió algún evento.
            while (isActive) {
                delay(FALLBACK_REFRESH_MILLIS)
                refreshProfiles(showLoading = false)
            }
        }
        startRealtimeLocationUpdates()
    }

    fun loadProfiles() {
        refreshProfiles(showLoading = true)
    }

    private fun refreshProfiles(showLoading: Boolean) {
        viewModelScope.launch {
            if (showLoading) _state.value = ProfilesUiState.Loading
            runCatching { repository.getProfiles() }
                .onSuccess { _state.value = ProfilesUiState.Content(it) }
                .onFailure {
                    Log.e(TAG, "Error loading profiles from Supabase", it)
                    if (showLoading || _state.value !is ProfilesUiState.Content) {
                        _state.value = ProfilesUiState.Error(
                            "No se pudieron cargar los datos de Supabase: " +
                                (it.message ?: "error desconocido")
                        )
                    }
                }
        }
    }

    private fun startRealtimeLocationUpdates() {
        if (realtimeJob?.isActive == true) return
        realtimeJob = viewModelScope.launch { collectRealtimeLocations() }
    }

    private suspend fun collectRealtimeLocations() {
        while (currentCoroutineContext().isActive) {
            val channel = TvSupabaseClient.client.channel("tv-locations-${System.nanoTime()}")
            try {
                val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "Ubicacion"
                }
                coroutineScope {
                    val collector = launch { changes.collect(::applyRealtimeLocation) }
                    channel.subscribe(blockUntilSubscribed = true)
                    collector.join()
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.w(TAG, "Canal Realtime de ubicación desconectado; se reintentará", exception)
            } finally {
                runCatching { channel.unsubscribe() }
            }
            delay(RECONNECT_DELAY_MILLIS)
        }
    }

    /** Actualiza en memoria únicamente el perfil dueño de la nueva coordenada. */
    private fun applyRealtimeLocation(action: PostgresAction) {
        val row = when (action) {
            is PostgresAction.Insert -> action.decodeRecordOrNull<TvRealtimeLocationRow>()
            is PostgresAction.Update -> action.decodeRecordOrNull<TvRealtimeLocationRow>()
            else -> null
        } ?: return

        val content = _state.value as? ProfilesUiState.Content ?: return
        val profiles = content.profiles.map { profile ->
            if (row.watchId !in profile.watchIds ||
                (profile.locationTimestamp != null && profile.locationTimestamp > row.timestamp)
            ) {
                profile
            } else {
                profile.copy(
                    latitude = row.latitude,
                    longitude = row.longitude,
                    locationTimestamp = row.timestamp
                )
            }
        }
        if (profiles != content.profiles) _state.value = ProfilesUiState.Content(profiles)
    }

    private companion object {
        const val TAG = "TvProfiles"
        const val FALLBACK_REFRESH_MILLIS = 30_000L
        const val RECONNECT_DELAY_MILLIS = 5_000L
    }
}

@Serializable
private data class TvRealtimeLocationRow(
    @SerialName("latitud") val latitude: Double,
    @SerialName("longitud") val longitude: Double,
    @SerialName("fechaHora") val timestamp: Long,
    @SerialName("idSmartwatch") val watchId: String
)