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

/**
 * ViewModel que gestiona la lista de perfiles monitoreados y la vinculación de smartwatches.
 *
 * Expone la lista de perfiles del cuidador actual y coordina las operaciones de alta,
 * edición, consulta y eliminación a través del repositorio de Supabase. También permite
 * descubrir relojes Wear OS disponibles para vincularlos a los perfiles.
 *
 * @param context Contexto de Android para inicializar el repositorio de la Data Layer.
 * @param repository Repositorio de Supabase para operaciones de persistencia.
 */
class ProfileViewModel(context: Context, private val repository: SupabaseRepository = SupabaseRepository()) : ViewModel() {
    private val wearRepository = WearDataLayerRepository(context.applicationContext)
    private val _profiles = MutableStateFlow<List<PerfilMonitoreadoEntity>>(emptyList())
    /** Flujo observable con la lista de perfiles monitoreados del cuidador. */
    val profiles: StateFlow<List<PerfilMonitoreadoEntity>> = _profiles
    private val _isLoading = MutableStateFlow(false)
    /** Flujo observable que indica si hay una operación de carga en curso. */
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _availableWatches = MutableStateFlow<List<AvailableWearDevice>>(emptyList())
    /** Flujo observable con la lista de relojes Wear OS disponibles para vincular. */
    val availableWatches: StateFlow<List<AvailableWearDevice>> = _availableWatches
    private val _isDiscoveringWatches = MutableStateFlow(false)
    /** Flujo observable que indica si se están buscando relojes disponibles. */
    val isDiscoveringWatches: StateFlow<Boolean> = _isDiscoveringWatches
    private val _watchDiscoveryMessage = MutableStateFlow<String?>(null)
    /** Flujo observable con mensaje informativo sobre el resultado de la búsqueda de relojes. */
    val watchDiscoveryMessage: StateFlow<String?> = _watchDiscoveryMessage

    /**
     * Carga los perfiles que pertenecen al cuidador actualmente autenticado.
     *
     * @return [Job] de la corrutina lanzada, o `null` si no hay sesión activa.
     */
    fun loadProfiles(): Job? {
        val userId = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id ?: return null
        return viewModelScope.launch {
            _isLoading.value = true
            runCatching { repository.fetchProfilesForCaregiver(userId) }.onSuccess { _profiles.value = it }
            _isLoading.value = false
        }
    }

    /**
     * Busca relojes Wear OS disponibles para vincularlos a un perfil.
     *
     * Actualiza [availableWatches] con los dispositivos encontrados y
     * [watchDiscoveryMessage] con un mensaje informativo si no hay resultados.
     */
    fun refreshAvailableWatches() = viewModelScope.launch {
        _isDiscoveringWatches.value = true
        runCatching { wearRepository.discoverAvailableWatches() }
            .onSuccess { _availableWatches.value = it; _watchDiscoveryMessage.value = if (it.isEmpty()) "No hay relojes disponibles" else null }
            .onFailure { _watchDiscoveryMessage.value = "No fue posible buscar relojes" }
        _isDiscoveringWatches.value = false
    }

    /**
     * Crea un perfil monitoreado y, si se eligió un reloj, lo vincula al perfil.
     *
     * @param nombre Nombre de la persona monitoreada.
     * @param edad Edad de la persona.
     * @param tipo Tipo de perfil ("Menor de edad", "Adulto mayor", "Cuidador").
     * @param fechaNacimiento Fecha de nacimiento en formato texto, o `null`.
     * @param selectedWatch Dispositivo Wear OS seleccionado para vincular, o `null`.
     * @param onComplete Callback con `true` si la creación fue exitosa.
     */
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

    /**
     * Guarda los cambios de un perfil existente y recarga la lista.
     *
     * @param idPerfil Identificador del perfil a actualizar.
     * @param nombre Nuevo nombre de la persona monitoreada.
     * @param edad Nueva edad de la persona.
     * @param fechaNacimiento Nueva fecha de nacimiento, o `null`.
     * @param onComplete Callback con `true` si la actualización fue exitosa.
     */
    fun updateProfile(idPerfil: String, nombre: String, edad: Int, fechaNacimiento: String?, onComplete: (Boolean) -> Unit) = viewModelScope.launch {
        val success = repository.updateProfile(idPerfil, nombre, edad, fechaNacimiento)
        if (success) loadProfiles()
        onComplete(success)
    }

    /**
     * Elimina un perfil y actualiza la lista mostrada al usuario.
     *
     * @param idPerfil Identificador del perfil a eliminar.
     * @param onComplete Callback con `true` si la eliminación fue exitosa.
     */
    fun deleteProfile(idPerfil: String, onComplete: (Boolean) -> Unit) = viewModelScope.launch {
        val success = repository.deleteProfile(idPerfil)
        if (success) loadProfiles()
        onComplete(success)
    }
}
