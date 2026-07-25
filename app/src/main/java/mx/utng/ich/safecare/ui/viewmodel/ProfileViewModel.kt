package mx.utng.ich.safecare.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.utng.ich.safecare.data.local.dao.PerfilMonitoreadoDao
import mx.utng.ich.safecare.data.local.dao.SmartwatchDao
import mx.utng.ich.safecare.data.local.entity.PerfilMonitoreadoEntity
import mx.utng.ich.safecare.data.local.entity.SmartwatchEntity
import mx.utng.ich.safecare.data.remote.SupabaseClient
import io.github.jan.supabase.auth.auth
import mx.utng.ich.safecare.data.repository.SupabaseRepository
import mx.utng.ich.safecare.data.datalayer.AvailableWearDevice
import mx.utng.ich.safecare.data.datalayer.WearDataLayerRepository
import android.util.Log

class ProfileViewModel(
    private val perfilDao: PerfilMonitoreadoDao,
    private val smartwatchDao: SmartwatchDao,
    context: Context,
    private val repository: SupabaseRepository = SupabaseRepository()
) : ViewModel() {
    private val wearRepository = WearDataLayerRepository(context)
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

    init {
        loadProfiles()
    }

    fun loadProfiles() {
        val userId = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = perfilDao.obtenerPorCuidador(userId)
                _profiles.value = result
            } catch (e: Exception) {
                _profiles.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshAvailableWatches() {
        viewModelScope.launch {
            _isDiscoveringWatches.value = true
            _watchDiscoveryMessage.value = null
            try {
                _availableWatches.value = wearRepository.discoverAvailableWatches()
                    .filter { device ->
                        smartwatchDao.obtenerPorWatchId(device.watchInstallationId)
                            ?.idPerfil == null
                    }
                if (_availableWatches.value.isEmpty()) {
                    _watchDiscoveryMessage.value =
                        "No hay relojes SafeCare disponibles sin vincular"
                }
            } catch (exception: Exception) {
                Log.e("ProfileVM", "Wear discovery failed", exception)
                _availableWatches.value = emptyList()
                _watchDiscoveryMessage.value =
                    "No fue posible buscar relojes. Verifica la conexión con Wear OS"
            } finally {
                _isDiscoveringWatches.value = false
            }
        }
    }

    fun addProfile(
        nombre: String,
        edad: Int,
        tipo: String,
        fechaNacimiento: String?,
        selectedWatch: AvailableWearDevice?,
        onComplete: (Boolean) -> Unit
    ) {
        val userId = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id 
        if (userId == null) {
            Log.e("ProfileVM", "Cannot add profile: User not logged in (Session is null)")
            onComplete(false)
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            Log.d("ProfileVM", "Attempting to create profile for user: $userId")
            
            try {
                val idPerfil = repository.createProfile(
                    nombre,
                    edad,
                    tipo,
                    userId,
                    selectedWatch?.watchInstallationId,
                    fechaNacimiento
                )
                if (idPerfil != null) {
                    Log.d("ProfileVM", "Supabase profile created: $idPerfil. Now saving to Room...")
                    
                    val entity = PerfilMonitoreadoEntity(
                        idPerfil = idPerfil,
                        nombre = nombre,
                        edad = edad,
                        fechaNacimiento = fechaNacimiento,
                        tipoPerfil = tipo,
                        idCuidador = userId
                    )
                    perfilDao.insertar(entity)

                    if (selectedWatch != null) {
                        smartwatchDao.insertar(
                            SmartwatchEntity(
                                idSmartwatch = selectedWatch.watchInstallationId,
                                numeroSerie = selectedWatch.watchInstallationId,
                                watchInstallationId = selectedWatch.watchInstallationId,
                                nombreDispositivo = selectedWatch.displayName,
                                modelo = selectedWatch.model,
                                dataLayerNodeId = selectedWatch.nodeId,
                                bateria = selectedWatch.batteryLevel.coerceAtLeast(0),
                                conexion = if (selectedWatch.isNearby) "bluetooth" else "online",
                                idPerfil = idPerfil
                            )
                        )
                        wearRepository.linkProfile(selectedWatch, entity)
                            .onFailure { exception ->
                                Log.e("ProfileVM", "Profile saved but watch sync failed", exception)
                                _watchDiscoveryMessage.value =
                                    "El perfil se guardó; la sincronización con el reloj queda pendiente"
                            }
                    }
                    Log.d("ProfileVM", "Room save successful.")
                    
                    loadProfiles()
                    onComplete(true)
                } else {
                    Log.e("ProfileVM", "Repository returned null ID (Supabase insert failed)")
                    onComplete(false)
                }
            } catch (e: Exception) {
                Log.e("ProfileVM", "Unexpected exception during profile creation: ${e.message}", e)
                onComplete(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProfile(idPerfil: String, nombre: String, edad: Int, fechaNacimiento: String?, onComplete: (Boolean) -> Unit) {
        val userId = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id ?: return
        viewModelScope.launch {
            _isLoading.value = true
            Log.d("ProfileVM", "Starting update for $idPerfil")
            val success = repository.updateProfile(idPerfil, nombre, edad, fechaNacimiento)
            if (success) {
                val currentProfiles = _profiles.value
                val existingProfile = currentProfiles.find { it.idPerfil == idPerfil }
                
                val entity = PerfilMonitoreadoEntity(
                    idPerfil = idPerfil,
                    nombre = nombre,
                    edad = edad,
                    fechaNacimiento = fechaNacimiento,
                    tipoPerfil = existingProfile?.tipoPerfil ?: "menor",
                    idCuidador = userId
                )
                perfilDao.insertar(entity)
                smartwatchDao.obtenerPorPerfil(idPerfil)?.let { watch ->
                    val nodeId = watch.dataLayerNodeId
                    val watchId = watch.watchInstallationId
                    if (nodeId != null && watchId != null) {
                        wearRepository.linkProfile(
                            AvailableWearDevice(
                                nodeId = nodeId,
                                watchInstallationId = watchId,
                                displayName = watch.nombreDispositivo ?: watch.modelo ?: "Wear OS",
                                model = watch.modelo ?: "Wear OS",
                                batteryLevel = watch.bateria,
                                isNearby = watch.conexion == "bluetooth"
                            ),
                            entity
                        ).onFailure {
                            Log.w("ProfileVM", "Profile update sync deferred", it)
                        }
                    }
                }
                Log.d("ProfileVM", "Local Room updated. Refreshing list...")
                loadProfiles()
                onComplete(true)
            } else {
                Log.e("ProfileVM", "Update failed in Repository")
                onComplete(false)
            }
            _isLoading.value = false
        }
    }

    fun deleteProfile(idPerfil: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.deleteProfile(idPerfil)
            if (success) {
                // Borrar local
                val existingProfile = _profiles.value.find { it.idPerfil == idPerfil }
                val linkedWatch = smartwatchDao.obtenerPorPerfil(idPerfil)
                if (existingProfile != null) {
                    perfilDao.eliminar(existingProfile)
                }
                linkedWatch?.dataLayerNodeId?.let { nodeId ->
                    wearRepository.unlinkProfile(nodeId, idPerfil)
                        .onFailure { Log.w("ProfileVM", "Watch unlink deferred", it) }
                }
                smartwatchDao.eliminarPorPerfil(idPerfil)
                loadProfiles()
                onComplete(true)
            } else {
                onComplete(false)
            }
            _isLoading.value = false
        }
    }
}
