package mx.utng.ich.safecare.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.utng.ich.safecare.data.local.entity.ZonaSeguraEntity
import mx.utng.ich.safecare.data.remote.SupabaseClient
import mx.utng.ich.safecare.data.repository.SupabaseRepository
import org.json.JSONArray
import org.osmdroid.util.GeoPoint
import java.net.URLEncoder
import java.net.URL

/**
 * ViewModel que gestiona las zonas seguras del cuidador.
 *
 * Mantiene la lista de zonas seguras, permite crearlas, editarlas, activar o desactivar
 * su monitoreo y eliminarlas. También incluye la búsqueda de direcciones mediante
 * la API de Nominatim para facilitar la ubicación de las zonas en el mapa.
 *
 * @param context Contexto de Android para las operaciones del repositorio.
 * @param repository Repositorio de Supabase para operaciones de persistencia.
 */
class SafeZoneViewModel(context: Context, private val repository: SupabaseRepository = SupabaseRepository()) : ViewModel() {
    private val _zones = MutableStateFlow<List<ZonaSeguraEntity>>(emptyList())
    /** Flujo observable con la lista de zonas seguras del cuidador. */
    val zones: StateFlow<List<ZonaSeguraEntity>> = _zones
    private val _isLoading = MutableStateFlow(false)
    /** Flujo observable que indica si hay una operación de carga en curso. */
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _searchResults = MutableStateFlow<List<Pair<String, GeoPoint>>>(emptyList())
    /** Flujo observable con los resultados de búsqueda de direcciones (nombre, coordenadas). */
    val searchResults: StateFlow<List<Pair<String, GeoPoint>>> = _searchResults
    private var searchJob: Job? = null

    /**
     * Carga las zonas seguras de los perfiles del cuidador autenticado.
     *
     * @return [Job] de la corrutina lanzada, o `null` si no hay sesión activa.
     */
    fun loadZones(): Job? {
        val userId = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id ?: return null
        return viewModelScope.launch { runCatching { repository.fetchSafeZonesForCaregiver(userId) }.onSuccess { _zones.value = it } }
    }

    /**
     * Busca direcciones mediante la API de Nominatim y devuelve sus coordenadas.
     *
     * Aplica un debounce de 600ms para evitar consultas excesivas mientras el usuario escribe.
     * Requiere al menos 3 caracteres para iniciar la búsqueda.
     *
     * @param query Texto de búsqueda de dirección o lugar.
     */
    fun searchLocation(query: String) {
        if (query.length < 3) { _searchResults.value = emptyList(); return }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(600)
            try {
                val encodedQuery = URLEncoder.encode(query, Charsets.UTF_8.name())
                _searchResults.value = withContext(Dispatchers.IO) {
                    val connection = URL(
                        "https://nominatim.openstreetmap.org/search?format=json&q=$encodedQuery"
                    ).openConnection().apply {
                        setRequestProperty("User-Agent", "SafeCare/1.0")
                        connectTimeout = SEARCH_TIMEOUT_MILLIS
                        readTimeout = SEARCH_TIMEOUT_MILLIS
                    }
                    val response = connection.getInputStream()
                        .bufferedReader()
                        .use { it.readText() }
                    val json = JSONArray(response)
                    List(json.length()) { index ->
                        json.getJSONObject(index).let {
                            it.getString("display_name") to
                                GeoPoint(it.getDouble("lat"), it.getDouble("lon"))
                        }
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.w(TAG, "No se pudo buscar la ubicación", exception)
                _searchResults.value = emptyList()
            }
        }
    }

    /**
     * Borra los resultados de la búsqueda de ubicación.
     */
    fun clearSearch() { _searchResults.value = emptyList() }

    /**
     * Crea una zona segura para uno o varios perfiles seleccionados.
     *
     * @param nombre Nombre descriptivo de la zona.
     * @param lat Latitud del centro de la zona.
     * @param lng Longitud del centro de la zona.
     * @param radio Radio de la zona en metros.
     * @param profileIds Lista de identificadores de perfiles a asignar.
     * @param onComplete Callback con `true` si la creación fue exitosa.
     */
    fun addZone(
        nombre: String,
        lat: Double,
        lng: Double,
        radio: Double,
        profileIds: List<String>,
        onComplete: (Boolean) -> Unit
    ) = viewModelScope.launch {
        _isLoading.value = true
        val selectedProfiles = profileIds.distinct()
        val primaryProfileId = selectedProfiles.firstOrNull()
        if (primaryProfileId == null) {
            _isLoading.value = false
            onComplete(false)
            return@launch
        }
        val zone = ZonaSeguraEntity(
            nombre = nombre,
            latitudCentro = lat,
            longitudCentro = lng,
            radioMetros = radio,
            idPerfil = primaryProfileId,
            idPerfiles = selectedProfiles.toSet()
        )
        val success = runCatching {
            repository.createSafeZone(zone.idZona, nombre, lat, lng, radio, selectedProfiles)
        }.getOrDefault(false)
        if (success) loadZones()?.join()
        _isLoading.value = false
        onComplete(success)
    }

    /**
     * Guarda los cambios de una zona segura existente.
     *
     * @param idZona Identificador de la zona a actualizar.
     * @param nombre Nuevo nombre de la zona.
     * @param lat Nueva latitud del centro.
     * @param lng Nueva longitud del centro.
     * @param radio Nuevo radio en metros.
     * @param profileIds Lista actualizada de perfiles asignados.
     * @param onComplete Callback con `true` si la actualización fue exitosa.
     */
    fun updateZone(
        idZona: String,
        nombre: String,
        lat: Double,
        lng: Double,
        radio: Double,
        profileIds: List<String>,
        onComplete: (Boolean) -> Unit
    ) = viewModelScope.launch {
        _isLoading.value = true
        val success = runCatching {
            repository.updateSafeZone(idZona, nombre, lat, lng, radio, profileIds.distinct())
        }.getOrDefault(false)
        if (success) loadZones()?.join()
        _isLoading.value = false
        onComplete(success)
    }

    /**
     * Cambia el estado activo de una zona segura.
     *
     * @param zone Entidad de la zona a modificar.
     * @param newStatus `true` para activar, `false` para desactivar el monitoreo.
     */
    fun toggleZoneStatus(zone: ZonaSeguraEntity, newStatus: Boolean) = viewModelScope.launch {
        if (repository.toggleSafeZoneStatus(zone.idZona, newStatus)) {
            loadZones()
        }
    }

    private companion object {
        const val TAG = "SafeZoneViewModel"
        const val SEARCH_TIMEOUT_MILLIS = 10_000
    }
}
