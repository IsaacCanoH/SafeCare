package mx.utng.ich.safecare.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.utng.ich.safecare.data.local.dao.ZonaSeguraDao
import mx.utng.ich.safecare.data.local.entity.ZonaSeguraEntity
import mx.utng.ich.safecare.data.remote.SupabaseClient
import io.github.jan.supabase.auth.auth
import mx.utng.ich.safecare.data.repository.SupabaseRepository

class SafeZoneViewModel(
    private val zonaSeguraDao: ZonaSeguraDao,
    private val repository: SupabaseRepository = SupabaseRepository()
) : ViewModel() {
    private val _zones = MutableStateFlow<List<ZonaSeguraEntity>>(emptyList())
    val zones: StateFlow<List<ZonaSeguraEntity>> = _zones

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadZones()
    }

    fun loadZones() {
        // En una app real filtraríamos por perfil seleccionado. 
        // Aquí cargamos todas por simplicidad o la del primer perfil.
    }

    fun addZone(nombre: String, lat: Double, lng: Double, radio: Double, idPerfil: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.createSafeZone(nombre, lat, lng, radio, idPerfil)
            if (success) {
                val entity = ZonaSeguraEntity(
                    nombre = nombre,
                    latitudCentro = lat,
                    longitudCentro = lng,
                    radioMetros = radio,
                    idPerfil = idPerfil
                )
                zonaSeguraDao.insertar(entity)
                onComplete(true)
            } else {
                onComplete(false)
            }
            _isLoading.value = false
        }
    }
}
