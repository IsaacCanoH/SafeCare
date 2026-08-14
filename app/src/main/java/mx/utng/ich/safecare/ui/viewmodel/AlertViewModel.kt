package mx.utng.ich.safecare.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.utng.ich.safecare.data.datalayer.WearDataLayerRepository
import mx.utng.ich.safecare.data.local.entity.AlertaConPerfil
import mx.utng.ich.safecare.data.local.entity.AlertaEntity
import mx.utng.ich.safecare.data.remote.SupabaseClient
import mx.utng.ich.safecare.data.repository.SupabaseRepository

class AlertViewModel(
    context: Context,
    private val repository: SupabaseRepository = SupabaseRepository()
) : ViewModel() {
    private val wearRepository = WearDataLayerRepository(context.applicationContext)
    private val _alerts = MutableStateFlow<List<AlertaConPerfil>>(emptyList())
    val alerts: StateFlow<List<AlertaConPerfil>> = _alerts
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private var realtimeJob: Job? = null

    // Carga las alertas junto con el nombre de cada perfil.
    fun refreshAlerts(): Job? {
        val caregiverId = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id ?: return null
        return viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                val profiles = repository.fetchProfilesForCaregiver(caregiverId).associateBy { it.idPerfil }
                repository.fetchAlertsForCaregiver(caregiverId)
                    .map { AlertaConPerfil(it, profiles[it.idPerfil]?.nombre) }
                    .sortedByDescending { it.alerta.fechaHora }
            }.onSuccess { _alerts.value = it }
            _isLoading.value = false
        }
    }

    // Escucha nuevas alertas remotas y refresca la pantalla.
    fun startRealtimeUpdates() {
        if (realtimeJob != null) return
        val caregiverId = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id ?: return
        val channel = SupabaseClient.client.channel("mobile-alerts-$caregiverId")
        realtimeJob = viewModelScope.launch {
            runCatching {
                channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "Alerta"
                }.collectLatest {
                    refreshAlerts()?.join()
                }
            }.onFailure { exception ->
                Log.e(TAG, "Realtime alerts", exception)
            }
        }
        viewModelScope.launch {
            runCatching { channel.subscribe(blockUntilSubscribed = true) }
                .onFailure { exception -> Log.e(TAG, "Realtime alerts subscribe", exception) }
        }
    }

    // Reconoce una alerta para ocultarla de los avisos pendientes en todos los dispositivos.
    fun acknowledgeAlert(alertId: String) {
        viewModelScope.launch {
            runCatching {
                check(repository.acknowledgeAlert(alertId)) {
                    "No se pudo reconocer la alerta"
                }
            }.onSuccess {
                refreshAlerts()?.join()
            }.onFailure { exception ->
                Log.e(TAG, "Acknowledge alert", exception)
            }
        }
    }

    // Envía una alerta personalizada al reloj del perfil elegido.
    fun sendCustomAlert(profileId: String, message: String, onResult: (Result<Unit>) -> Unit) {
        val cleanMessage = message.trim()
        if (cleanMessage.isEmpty()) return onResult(Result.failure(IllegalArgumentException("Escribe un mensaje para la alerta")))
        viewModelScope.launch {
            val result = runCatching {
                val alert = AlertaEntity(tipoAlerta = "ALERTA", descripcion = cleanMessage, idPerfil = profileId)
                check(repository.saveAlert(alert)) { "No se pudo guardar la alerta en Supabase" }
                val serial = repository.fetchWatchSerial(profileId)
                    ?: error("Este perfil no tiene reloj vinculado")
                val watch = wearRepository.discoverAvailableWatches()
                    .firstOrNull { it.watchInstallationId == serial }
                    ?: error("El reloj no está disponible")
                wearRepository.sendCustomAlert(watch.nodeId, alert).getOrThrow()
                refreshAlerts()
                Unit
            }
            onResult(result)
        }
    }
    private companion object {
        const val TAG = "AlertViewModel"
    }
}
