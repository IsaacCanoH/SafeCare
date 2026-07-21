package mx.utng.ich.safecare.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.utng.ich.safecare.data.local.dao.PerfilMonitoreadoDao
import mx.utng.ich.safecare.data.local.entity.PerfilMonitoreadoEntity
import mx.utng.ich.safecare.data.remote.SupabaseClient
import io.github.jan.supabase.auth.auth
import mx.utng.ich.safecare.data.repository.SupabaseRepository
import android.util.Log

class ProfileViewModel(
    private val perfilDao: PerfilMonitoreadoDao,
    private val repository: SupabaseRepository = SupabaseRepository()
) : ViewModel() {
    private val _profiles = MutableStateFlow<List<PerfilMonitoreadoEntity>>(emptyList())
    val profiles: StateFlow<List<PerfilMonitoreadoEntity>> = _profiles

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

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

    fun addProfile(nombre: String, edad: Int, tipo: String, numeroSerie: String?, onComplete: (Boolean) -> Unit) {
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
                val idPerfil = repository.createProfile(nombre, edad, tipo, userId, numeroSerie)
                if (idPerfil != null) {
                    Log.d("ProfileVM", "Supabase profile created: $idPerfil. Now saving to Room...")
                    
                    val entity = PerfilMonitoreadoEntity(
                        idPerfil = idPerfil,
                        nombre = nombre,
                        edad = edad,
                        tipoPerfil = tipo,
                        idCuidador = userId
                    )
                    perfilDao.insertar(entity)
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
                if (existingProfile != null) {
                    perfilDao.eliminar(existingProfile)
                }
                loadProfiles()
                onComplete(true)
            } else {
                onComplete(false)
            }
            _isLoading.value = false
        }
    }
}
