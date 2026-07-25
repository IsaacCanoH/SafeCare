package mx.utng.ich.safecare.data.datalayer

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import mx.utng.ich.safecare.data.local.database.SafeCareAppDatabase
import mx.utng.ich.safecare.data.local.entity.AlertaEntity
import mx.utng.ich.safecare.data.local.entity.UbicacionEntity
import mx.utng.ich.safecare.data.repository.SupabaseRepository

class MobileDataLayerService : WearableListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val supabaseRepository = SupabaseRepository()

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents
            .filter { it.type == DataEvent.TYPE_CHANGED }
            .forEach { event ->
                val dataItem = event.dataItem.freeze()
                serviceScope.launch {
                    runCatching {
                        processDataItem(dataItem)
                        syncRecentRoomLocations()
                    }
                        .onFailure { Log.e(TAG, "No se pudo procesar Data Layer", it) }
                }
            }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun processDataItem(dataItem: com.google.android.gms.wearable.DataItem) {
        val database = SafeCareAppDatabase.getDatabase(this)
        val data = DataMapItem.fromDataItem(dataItem).dataMap
        when {
            dataItem.uri.path?.startsWith(PATH_STATUS) == true -> {
                val watchId = data.getString(KEY_WATCH_ID) ?: return
                val existing = database.smartwatchDao().obtenerPorWatchId(watchId) ?: return
                database.smartwatchDao().insertar(
                    existing.copy(
                        bateria = data.getInt(KEY_BATTERY),
                        conexion = data.getString(KEY_CONNECTION) ?: "online",
                        ultimaConexion = data.getLong(KEY_TIMESTAMP),
                        dataLayerNodeId = dataItem.uri.host ?: existing.dataLayerNodeId
                    )
                )
            }

            dataItem.uri.path?.startsWith(PATH_ALERT) == true -> {
                val alertId = data.getString(KEY_ALERT_ID) ?: return
                val profileId = data.getString(KEY_PROFILE_ID) ?: return
                val locationId = data.getString(KEY_LOCATION_ID)
                if (locationId != null && data.containsKey(KEY_LATITUDE)) {
                    val watchId = data.getString(KEY_WATCH_ID) ?: return
                    val location = UbicacionEntity(
                            idUbicacion = locationId,
                            latitud = data.getDouble(KEY_LATITUDE),
                            longitud = data.getDouble(KEY_LONGITUDE),
                            fechaHora = data.getLong(KEY_TIMESTAMP),
                            idSmartwatch = watchId
                        )
                    database.ubicacionDao().insertar(location)
                    supabaseRepository.saveLocation(location)
                }
                val alert = AlertaEntity(
                        idAlerta = alertId,
                        tipoAlerta = data.getString(KEY_ALERT_TYPE) ?: "ALERTA",
                        descripcion = data.getString(KEY_DESCRIPTION) ?: "",
                        fechaHora = data.getLong(KEY_TIMESTAMP),
                        estado = data.getString(KEY_STATE) ?: "ACTIVA",
                        idPerfil = profileId,
                        idUbicacion = locationId
                    )
                database.alertaDao().insertar(alert)
                supabaseRepository.saveAlert(alert)
            }

            dataItem.uri.path?.startsWith(PATH_LOCATION) == true -> {
                val locationId = data.getString(KEY_LOCATION_ID) ?: return
                val watchId = data.getString(KEY_WATCH_ID) ?: return
                val location = UbicacionEntity(
                        idUbicacion = locationId,
                        latitud = data.getDouble(KEY_LATITUDE),
                        longitud = data.getDouble(KEY_LONGITUDE),
                        fechaHora = data.getLong(KEY_TIMESTAMP),
                        idSmartwatch = watchId
                    )
                database.ubicacionDao().insertar(location)
                supabaseRepository.saveLocation(location)
            }
        }
    }

    private suspend fun syncRecentRoomLocations() {
        val locations = SafeCareAppDatabase.getDatabase(this)
            .ubicacionDao()
            .obtenerRecientes()
        locations.forEach { location ->
            supabaseRepository.saveLocation(location)
        }
    }

    companion object {
        private const val TAG = "MobileDataLayer"
        private const val PATH_STATUS = "/safecare/status/"
        private const val PATH_ALERT = "/safecare/alert/"
        private const val PATH_LOCATION = "/safecare/location/"
        private const val KEY_WATCH_ID = "watchId"
        private const val KEY_BATTERY = "battery"
        private const val KEY_CONNECTION = "connection"
        private const val KEY_TIMESTAMP = "timestamp"
        private const val KEY_ALERT_ID = "alertId"
        private const val KEY_PROFILE_ID = "profileId"
        private const val KEY_LOCATION_ID = "locationId"
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
        private const val KEY_ALERT_TYPE = "alertType"
        private const val KEY_DESCRIPTION = "description"
        private const val KEY_STATE = "state"
    }
}
