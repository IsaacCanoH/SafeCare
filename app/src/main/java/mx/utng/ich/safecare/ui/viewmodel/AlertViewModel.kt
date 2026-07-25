package mx.utng.ich.safecare.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mx.utng.ich.safecare.data.local.dao.AlertaDao
import mx.utng.ich.safecare.data.local.dao.SmartwatchDao
import mx.utng.ich.safecare.data.local.entity.AlertaConPerfil
import mx.utng.ich.safecare.data.local.entity.AlertaEntity
import mx.utng.ich.safecare.data.datalayer.WearDataLayerRepository

class AlertViewModel(
    private val alertaDao: AlertaDao,
    private val smartwatchDao: SmartwatchDao,
    context: Context
) : ViewModel() {
    private val wearDataLayerRepository = WearDataLayerRepository(context.applicationContext)
    private val _alerts = MutableStateFlow<List<AlertaConPerfil>>(emptyList())
    val alerts: StateFlow<List<AlertaConPerfil>> = _alerts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        viewModelScope.launch {
            alertaDao.observarTodas().collectLatest { storedAlerts ->
                _alerts.value = storedAlerts
            }
        }
    }

    fun sendCustomAlert(
        profileId: String,
        message: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val cleanMessage = message.trim()
        if (cleanMessage.isEmpty()) {
            onResult(Result.failure(IllegalArgumentException("Escribe un mensaje para la alerta")))
            return
        }

        viewModelScope.launch {
            val result = runCatching {
                val smartwatch = smartwatchDao.obtenerPorPerfil(profileId)
                    ?: error("Este perfil no tiene un reloj vinculado")
                val nodeId = smartwatch.dataLayerNodeId
                    ?.takeIf { it.isNotBlank() }
                    ?: error("El reloj no está disponible mediante Data Layer")
                val alert = AlertaEntity(
                    tipoAlerta = "ALERTA",
                    descripcion = cleanMessage,
                    idPerfil = profileId
                )
                wearDataLayerRepository.sendCustomAlert(nodeId, alert).getOrThrow()
                alertaDao.insertar(alert)
            }
            onResult(result)
        }
    }
}
