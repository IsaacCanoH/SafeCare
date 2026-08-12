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
import mx.utng.ich.safecare.data.local.entity.AlertaEntity
import mx.utng.ich.safecare.data.local.entity.UbicacionEntity
import mx.utng.ich.safecare.data.repository.SupabaseRepository

class MobileDataLayerService : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository = SupabaseRepository()
    // Recibe y procesa los datos nuevos enviados desde el smartwatch.
    override fun onDataChanged(events: DataEventBuffer) {
        // DataEventBuffer solo es vÃ¡lido durante esta llamada. Congelamos los datos
        // antes de lanzar la corrutina para evitar "Buffer is closed".
        val changedItems = events
            .filter { it.type == DataEvent.TYPE_CHANGED }
            .map { it.dataItem.freeze() }

        changedItems.forEach { item ->
            scope.launch {
                runCatching { process(item) }
                    .onFailure { Log.e(TAG, "Data Layer", it) }
            }
        }
    }
    // Cancela las tareas pendientes al detener el servicio.
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
    // Guarda el estado, ubicación o alerta recibida según su ruta.
    private suspend fun process(item: com.google.android.gms.wearable.DataItem) {
        val data = DataMapItem.fromDataItem(item).dataMap
        when {
            item.uri.path?.startsWith(PATH_STATUS) == true -> repository.updateSmartWatchStatus(
                data.getString(KEY_WATCH_ID) ?: return, data.getInt(KEY_BATTERY), data.getString(KEY_CONNECTION) ?: "online", data.getLong(KEY_TIMESTAMP))
            item.uri.path?.startsWith(PATH_LOCATION) == true -> repository.saveLocation(UbicacionEntity(
                data.getString(KEY_LOCATION_ID) ?: return, data.getDouble(KEY_LATITUDE), data.getDouble(KEY_LONGITUDE), data.getLong(KEY_TIMESTAMP), data.getString(KEY_WATCH_ID) ?: return))
            item.uri.path?.startsWith(PATH_ALERT) == true -> {
                val locationId = data.getString(KEY_LOCATION_ID)
                if (locationId != null && data.containsKey(KEY_LATITUDE)) repository.saveLocation(UbicacionEntity(locationId, data.getDouble(KEY_LATITUDE), data.getDouble(KEY_LONGITUDE), data.getLong(KEY_TIMESTAMP), data.getString(KEY_WATCH_ID) ?: return))
                repository.saveAlert(AlertaEntity(data.getString(KEY_ALERT_ID) ?: return, data.getString(KEY_ALERT_TYPE) ?: "ALERTA", data.getString(KEY_DESCRIPTION) ?: "", data.getLong(KEY_TIMESTAMP), data.getString(KEY_STATE) ?: "ACTIVA", data.getString(KEY_PROFILE_ID) ?: return, locationId))
            }
        }
    }
    companion object { private const val TAG="MobileDataLayer"; private const val PATH_STATUS="/safecare/status/"; private const val PATH_ALERT="/safecare/alert/"; private const val PATH_LOCATION="/safecare/location/"; private const val KEY_WATCH_ID="watchId"; private const val KEY_BATTERY="battery"; private const val KEY_CONNECTION="connection"; private const val KEY_TIMESTAMP="timestamp"; private const val KEY_ALERT_ID="alertId"; private const val KEY_PROFILE_ID="profileId"; private const val KEY_LOCATION_ID="locationId"; private const val KEY_LATITUDE="latitude"; private const val KEY_LONGITUDE="longitude"; private const val KEY_ALERT_TYPE="alertType"; private const val KEY_DESCRIPTION="description"; private const val KEY_STATE="state" }
}
