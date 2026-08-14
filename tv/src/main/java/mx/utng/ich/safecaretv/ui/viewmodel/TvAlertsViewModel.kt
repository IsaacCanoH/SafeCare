package mx.utng.ich.safecaretv.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mx.utng.ich.safecaretv.data.alert.TvAlert
import mx.utng.ich.safecaretv.data.alert.TvAlertsRepository

class TvAlertsViewModel : ViewModel() {
    private val repository = TvAlertsRepository()
    private val _activeAlert = MutableStateFlow<TvAlert?>(null)
    val activeAlert = _activeAlert.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(5_000)
            }
        }
    }

    // Reconoce la alerta actual en Supabase para retirarla de todos los dispositivos.
    fun acknowledge() {
        _activeAlert.value?.let { alert ->
            viewModelScope.launch {
                runCatching { repository.acknowledgeAlert(alert.id) }
                    .onSuccess {
                        _activeAlert.value = null
                        refresh()
                    }
                    .onFailure { exception ->
                        Log.e("TvAlerts", "Error acknowledging alert", exception)
                    }
            }
        }
    }

    // Recarga la alerta más reciente desde el repositorio.
    private suspend fun refresh() {
        runCatching { repository.getActiveAlerts() }
            .onSuccess { alerts ->
                val currentAlert = _activeAlert.value
                val currentStillActive = currentAlert?.let { active ->
                    alerts.firstOrNull { it.id == active.id }
                }
                val newestAlert = alerts.firstOrNull()
                _activeAlert.value = when {
                    currentStillActive == null -> newestAlert
                    newestAlert?.isSos == true && !currentStillActive.isSos -> newestAlert
                    else -> currentStillActive
                }
            }
            .onFailure { Log.e("TvAlerts", "Error loading Supabase alerts", it) }
    }
}
