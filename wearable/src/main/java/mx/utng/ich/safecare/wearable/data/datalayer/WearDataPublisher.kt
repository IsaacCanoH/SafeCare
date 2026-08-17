package mx.utng.ich.safecare.wearable.data.datalayer

import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import mx.utng.ich.safecare.wearable.data.local.entity.AlertaEntity
import mx.utng.ich.safecare.wearable.data.local.entity.SmartwatchEntity
import mx.utng.ich.safecare.wearable.data.local.entity.UbicacionEntity

class WearDataPublisher(context: Context) {
    private val dataClient = Wearable.getDataClient(context.applicationContext)

    /** Envía el estado del smartwatch a la aplicación móvil. */
    fun publishStatus(status: SmartwatchEntity) {
        val request = PutDataMapRequest.create("$PATH_STATUS${status.idSmartwatch}").apply {
            dataMap.putString(KEY_WATCH_ID, status.idSmartwatch)
            dataMap.putInt(KEY_BATTERY, status.bateria)
            dataMap.putString(KEY_CONNECTION, status.conexion)
            dataMap.putLong(KEY_TIMESTAMP, status.ultimaConexion)
        }.asPutDataRequest().setUrgent()

        dataClient.putDataItem(request)
            .addOnFailureListener { Log.w(TAG, "Estado pendiente de sincronización", it) }
    }

    /** Envía una alerta y sus coordenadas a la aplicación móvil. */
    fun publishAlert(
        watchId: String,
        alert: AlertaEntity,
        location: Location?
    ) {
        val request = PutDataMapRequest.create("$PATH_ALERT${alert.idAlerta}").apply {
            dataMap.putString(KEY_WATCH_ID, watchId)
            dataMap.putString(KEY_ALERT_ID, alert.idAlerta)
            dataMap.putString(KEY_PROFILE_ID, alert.idPerfil)
            dataMap.putString(KEY_ALERT_TYPE, alert.tipoAlerta)
            dataMap.putString(KEY_DESCRIPTION, alert.descripcion)
            dataMap.putString(KEY_STATE, alert.estado)
            dataMap.putLong(KEY_TIMESTAMP, alert.fechaHora)
            alert.idUbicacion?.let { dataMap.putString(KEY_LOCATION_ID, it) }
            location?.let {
                dataMap.putDouble(KEY_LATITUDE, it.latitude)
                dataMap.putDouble(KEY_LONGITUDE, it.longitude)
            }
        }.asPutDataRequest().setUrgent()

        dataClient.putDataItem(request)
            .addOnFailureListener { Log.w(TAG, "Alerta pendiente de sincronización", it) }
    }

    /** Envía una ubicación nueva a la aplicación móvil. */
    fun publishLocation(location: UbicacionEntity) {
        val request = PutDataMapRequest.create("$PATH_LOCATION${location.idSmartwatch}").apply {
            dataMap.putString(KEY_WATCH_ID, location.idSmartwatch)
            dataMap.putString(KEY_LOCATION_ID, location.idUbicacion)
            dataMap.putDouble(KEY_LATITUDE, location.latitud)
            dataMap.putDouble(KEY_LONGITUDE, location.longitud)
            dataMap.putLong(KEY_TIMESTAMP, location.fechaHora)
        }.asPutDataRequest().setUrgent()

        dataClient.putDataItem(request)
            .addOnFailureListener { Log.w(TAG, "Ubicación pendiente de sincronización", it) }
    }

    companion object {
        private const val TAG = "WearDataPublisher"
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
