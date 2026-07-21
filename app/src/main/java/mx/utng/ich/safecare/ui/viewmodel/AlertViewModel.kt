package mx.utng.ich.safecare.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.utng.ich.safecare.data.local.entity.AlertaEntity
import mx.utng.ich.safecare.data.remote.SupabaseClient
import io.github.jan.supabase.auth.auth

class AlertViewModel : ViewModel() {
    private val _alerts = MutableStateFlow<List<AlertaEntity>>(emptyList())
    val alerts: StateFlow<List<AlertaEntity>> = _alerts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadAlerts()
    }

    fun loadAlerts() {
        // En una app real, aquí nos suscribiríamos a Supabase Realtime para recibir alertas
        // O cargaríamos el historial desde Supabase/Room.
        // Por ahora dejamos el estado preparado.
    }
}
