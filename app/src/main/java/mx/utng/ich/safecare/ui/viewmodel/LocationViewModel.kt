package mx.utng.ich.safecare.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mx.utng.ich.safecare.data.local.entity.LatestProfileLocation
import mx.utng.ich.safecare.data.remote.SupabaseClient
import mx.utng.ich.safecare.data.repository.SupabaseRepository

class LocationViewModel(
    private val repository: SupabaseRepository = SupabaseRepository()
) : ViewModel() {
    private val _latestLocationsByProfile =
        MutableStateFlow<Map<String, LatestProfileLocation>>(emptyMap())
    val latestLocationsByProfile: StateFlow<Map<String, LatestProfileLocation>> =
        _latestLocationsByProfile
    private var realtimeJob: Job? = null
    private var profileIdByWatchId: Map<String, String> = emptyMap()

    /** Carga solamente el último punto de cada reloj del cuidador. */
    fun refreshLocations(): Job? {
        val caregiverId = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id ?: return null
        return viewModelScope.launch {
            runCatching { repository.fetchLatestLocationsForCaregiver(caregiverId) }
                .onSuccess { locations ->
                    _latestLocationsByProfile.value = locations.associateBy(LatestProfileLocation::idPerfil)
                    profileIdByWatchId = locations.associate { it.idSmartwatch to it.idPerfil }
                }
                .onFailure { exception ->
                    Log.w(TAG, "No se pudieron refrescar las ubicaciones", exception)
                }
        }
    }

    /**
     * Mantiene el mapa actualizado con INSERT/UPDATE de Supabase Realtime. Un refresco
     * ligero funciona como respaldo y también recupera el estado tras una desconexión.
     */
    fun startRealtimeUpdates() {
        if (realtimeJob?.isActive == true) return
        val caregiverId = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id ?: return
        realtimeJob = viewModelScope.launch {
            launch { collectRealtimeLocations(caregiverId) }
            launch {
                while (isActive) {
                    delay(FALLBACK_REFRESH_MILLIS)
                    refreshLocations()?.join()
                }
            }
        }
    }

    private suspend fun collectRealtimeLocations(caregiverId: String) {
        while (currentCoroutineContext().isActive) {
            val channel = SupabaseClient.client.channel(
                "mobile-locations-$caregiverId-${System.nanoTime()}"
            )
            try {
                // El flujo debe registrarse antes de suscribir el canal.
                val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "Ubicacion"
                }
                coroutineScope {
                    val collector = launch { changes.collectLatest(::applyRealtimeLocation) }
                    channel.subscribe(blockUntilSubscribed = true)
                    collector.join()
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.w(TAG, "Canal Realtime desconectado; se reintentará", exception)
            } finally {
                runCatching { channel.unsubscribe() }
            }
            delay(RECONNECT_DELAY_MILLIS)
        }
    }

    /** Aplica solo la nueva fila recibida; no descarga el historial de Ubicacion. */
    private suspend fun applyRealtimeLocation(action: PostgresAction) {
        val row = when (action) {
            is PostgresAction.Insert -> action.decodeRecordOrNull<RealtimeLocationRow>()
            is PostgresAction.Update -> action.decodeRecordOrNull<RealtimeLocationRow>()
            else -> null
        } ?: return

        val profileId = profileIdByWatchId[row.watchId]
        if (profileId == null) {
            // El reloj se vinculó después de la carga inicial.
            refreshLocations()?.join()
            return
        }

        val current = _latestLocationsByProfile.value[profileId]
        if (current != null && current.fechaHora > row.timestamp) return

        val location = LatestProfileLocation(
            idPerfil = profileId,
            idUbicacion = row.id,
            latitud = row.latitude,
            longitud = row.longitude,
            fechaHora = row.timestamp,
            idSmartwatch = row.watchId
        )
        _latestLocationsByProfile.value = _latestLocationsByProfile.value + (profileId to location)
        profileIdByWatchId = profileIdByWatchId + (row.watchId to profileId)
    }

    private companion object {
        const val TAG = "LocationViewModel"
        const val FALLBACK_REFRESH_MILLIS = 30_000L
        const val RECONNECT_DELAY_MILLIS = 5_000L
    }
}

@Serializable
private data class RealtimeLocationRow(
    @SerialName("idUbicacion") val id: String,
    @SerialName("latitud") val latitude: Double,
    @SerialName("longitud") val longitude: Double,
    @SerialName("fechaHora") val timestamp: Long,
    @SerialName("idSmartwatch") val watchId: String
)
