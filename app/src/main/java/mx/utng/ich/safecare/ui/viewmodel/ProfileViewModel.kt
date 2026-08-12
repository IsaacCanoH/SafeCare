package mx.utng.ich.safecare.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.utng.ich.safecare.data.datalayer.AvailableWearDevice
import mx.utng.ich.safecare.data.datalayer.WearDataLayerRepository
import mx.utng.ich.safecare.data.local.entity.PerfilMonitoreadoEntity
import mx.utng.ich.safecare.data.remote.SupabaseClient
import mx.utng.ich.safecare.data.repository.SupabaseRepository

class ProfileViewModel(context: Context, private val repository: SupabaseRepository = SupabaseRepository()) : ViewModel() {
    private val wearRepository = WearDataLayerRepository(context.applicationContext)
    private val _profiles = MutableStateFlow<List<PerfilMonitoreadoEntity>>(emptyList())
    val profiles: StateFlow<List<PerfilMonitoreadoEntity>> = _profiles
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _availableWatches = MutableStateFlow<List<AvailableWearDevice>>(emptyList())
    val availableWatches: StateFlow<List<AvailableWearDevice>> = _availableWatches
    private val _isDiscoveringWatches = MutableStateFlow(false)
    val isDiscoveringWatches: StateFlow<Boolean> = _isDiscoveringWatches
    private val _watchDiscoveryMessage = MutableStateFlow<String?>(null)
    val watchDiscoveryMessage: StateFlow<String?> = _watchDiscoveryMessage

    // Carga los perfiles que pertenecen al cuidador actual.
    fun loadProfiles(): Job? {
        val userId = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id ?: return null
        return viewModelScope.launch {
            _isLoading.value = true
            runCatching { repository.fetchProfilesForCaregiver(userId) }.onSuccess { _profiles.value = it }
            _isLoading.value = false
        }
    }

    // Busca relojes Wear OS disponibles para vincularlos.
    fun refreshAvailableWatches() = viewModelScope.launch {
        _isDiscoveringWatches.value = true
        runCatching { wearRepository.discoverAvailableWatches() }
            .onSuccess { _availableWatches.value = it; _watchDiscoveryMessage.value = if (it.isEmpty()) "No hay relojes disponibles" else null }
            .onFailure { _watchDiscoveryMessage.value = "No fue posible buscar relojes" }
        _isDiscoveringWatches.value = false
    }

    // Crea un perfil y, si se eligió, vincula su smartwatch.
    fun addProfile(nombre: String, edad: Int, tipo: String, fechaNacimiento: String?, selectedWatch: AvailableWearDevice?, onComplete: (Boolean) -> Unit) {
        val userId = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id ?: return onComplete(false)
        viewModelScope.launch {
            _isLoading.value = true
            val id = repository.createProfile(nombre, edad, tipo, userId, selectedWatch?.watchInstallationId, fechaNacimiento)
            if (id != null) {
                loadProfiles(); onComplete(true)
            } else onComplete(false)
            _isLoading.value = false
        }
    }

    // Guarda los cambios de un perfil y recarga la lista.
    fun updateProfile(idPerfil: String, nombre: String, edad: Int, fechaNacimiento: String?, onComplete: (Boolean) -> Unit) = viewModelScope.launch {
        val success = repository.updateProfile(idPerfil, nombre, edad, fechaNacimiento)
        if (success) loadProfiles()
        onComplete(success)
    }

    // Elimina un perfil y actualiza la lista mostrada.
    fun deleteProfile(idPerfil: String, onComplete: (Boolean) -> Unit) = viewModelScope.launch {
        val success = repository.deleteProfile(idPerfil)
        if (success) loadProfiles()
        onComplete(success)
    }
}
