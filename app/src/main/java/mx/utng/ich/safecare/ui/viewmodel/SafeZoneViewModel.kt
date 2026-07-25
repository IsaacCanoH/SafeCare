package mx.utng.ich.safecare.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.utng.ich.safecare.data.local.dao.ZonaSeguraDao
import mx.utng.ich.safecare.data.local.dao.SmartwatchDao
import mx.utng.ich.safecare.data.local.entity.ZonaSeguraEntity
import mx.utng.ich.safecare.data.remote.SupabaseClient
import io.github.jan.supabase.auth.auth
import mx.utng.ich.safecare.data.repository.SupabaseRepository
import mx.utng.ich.safecare.data.datalayer.WearDataLayerRepository
import org.osmdroid.util.GeoPoint
import java.net.URL
import org.json.JSONArray
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class SafeZoneViewModel(
    private val zonaSeguraDao: ZonaSeguraDao,
    private val smartwatchDao: SmartwatchDao,
    context: Context,
    private val repository: SupabaseRepository = SupabaseRepository()
) : ViewModel() {
    private val wearRepository = WearDataLayerRepository(context)
    private val _zones = MutableStateFlow<List<ZonaSeguraEntity>>(emptyList())
    val zones: StateFlow<List<ZonaSeguraEntity>> = _zones

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _searchResults = MutableStateFlow<List<Pair<String, GeoPoint>>>(emptyList())
    val searchResults: StateFlow<List<Pair<String, GeoPoint>>> = _searchResults

    private var searchJob: Job? = null

    init {
        loadZones()
    }

    fun loadZones() {
        val userId = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // EXPLICACIÓN: Como Room no es relacional de la misma forma que SQL puro con Joins complejos 
                // en una sola sentencia sencilla de DAO, vamos a cargar TODAS las zonas locales.
                // Luego en el SafeCareApp filtramos o mostramos según necesitemos.
                val result = zonaSeguraDao.obtenerTodas() 
                _zones.value = result
                Log.d("SafeZoneVM", "Zonas cargadas: ${result.size}")
            } catch (e: Exception) {
                Log.e("SafeZoneVM", "Error loading zones: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchLocation(query: String) {
        if (query.isBlank() || query.length < 3) {
            _searchResults.value = emptyList()
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(600) // Debounce para no saturar la API
            
            _isLoading.value = true
            try {
                val results = withContext(Dispatchers.IO) {
                    val connection = URL("https://nominatim.openstreetmap.org/search?format=json&q=${query.replace(" ", "+")}").openConnection()
                    // Nominatim bloquea peticiones sin un User-Agent descriptivo
                    connection.setRequestProperty("User-Agent", "SafeCare-App-V2")
                    
                    val response = connection.getInputStream().bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(response)
                    val list = mutableListOf<Pair<String, GeoPoint>>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val name = obj.getString("display_name")
                        val lat = obj.getDouble("lat")
                        val lon = obj.getDouble("lon")
                        list.add(name to GeoPoint(lat, lon))
                    }
                    list
                }
                _searchResults.value = results
                Log.d("SafeZoneVM", "Search success: ${results.size} items found")
            } catch (e: Exception) {
                Log.e("SafeZoneVM", "Search error: ${e.message}", e)
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
    }

    fun addZone(nombre: String, lat: Double, lng: Double, radio: Double, idPerfil: String, onComplete: (Boolean) -> Unit) {
        if (idPerfil.isEmpty()) {
            Log.e("SafeZoneVM", "Cannot add zone: idPerfil is empty")
            onComplete(false)
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
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
                    syncZonesWithLinkedWatch(idPerfil)
                    loadZones()
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                Log.e("SafeZoneVM", "Error adding zone: ${e.message}")
                onComplete(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateZone(idZona: String, nombre: String, lat: Double, lng: Double, radio: Double, idPerfil: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = repository.updateSafeZone(idZona, nombre, lat, lng, radio)
                if (success) {
                    val entity = ZonaSeguraEntity(
                        idZona = idZona,
                        nombre = nombre,
                        latitudCentro = lat,
                        longitudCentro = lng,
                        radioMetros = radio,
                        idPerfil = idPerfil
                    )
                    zonaSeguraDao.insertar(entity)
                    syncZonesWithLinkedWatch(idPerfil)
                    loadZones()
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                Log.e("SafeZoneVM", "Error updating zone: ${e.message}")
                onComplete(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleZoneStatus(zone: ZonaSeguraEntity, newStatus: Boolean) {
        viewModelScope.launch {
            // Optimistic update local
            val updatedEntity = zone.copy(activa = newStatus)
            zonaSeguraDao.insertar(updatedEntity)
            loadZones()

            // Update remote
            val success = repository.toggleSafeZoneStatus(zone.idZona, newStatus)
            if (!success) {
                // Revert local if remote fails
                zonaSeguraDao.insertar(zone)
                loadZones()
                Log.e("SafeZoneVM", "Failed to toggle status in Supabase")
            } else {
                syncZonesWithLinkedWatch(zone.idPerfil)
            }
        }
    }

    private suspend fun syncZonesWithLinkedWatch(profileId: String) {
        val watch = smartwatchDao.obtenerPorPerfil(profileId) ?: return
        val nodeId = watch.dataLayerNodeId ?: return
        val profileZones = zonaSeguraDao.obtenerPorPerfil(profileId)
        wearRepository.syncZones(nodeId, profileId, profileZones)
            .onFailure { exception ->
                Log.w("SafeZoneVM", "Zone sync deferred for profile=$profileId", exception)
            }
    }
}
