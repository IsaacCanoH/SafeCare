package mx.utng.ich.safecare.wearable.presentation.geofence

import android.content.Context
import android.location.Location
import android.util.Log
import mx.utng.ich.safecare.wearable.data.local.SafeCareProfileResolver
import mx.utng.ich.safecare.wearable.data.local.database.DatabaseProvider

/**
 * Verificación independiente de Google Geofencing.
 *
 * Se ejecuta con cada coordenada GPS producida por el propio reloj. De esta forma
 * SafeCare no depende de que Fused Location/Geofencing entregue una transición.
 */
class SafeZoneMonitor(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    // Evalúa si la ubicación actual salió de una zona segura.
    suspend fun evaluate(location: Location) {
        val database = DatabaseProvider.getDatabase(appContext)
        val profileId = SafeCareProfileResolver.resolveProfileId(database)
        if (profileId.isBlank()) return

        val zones = database.zonaSeguraDao().obtenerZonasActivas(profileId)
        if (zones.isEmpty()) {
            Log.w(TAG, "No se evaluó la ubicación: no hay zonas activas para $profileId")
            return
        }

        val containingZone = zones.firstOrNull { zone ->
            distanceMeters(
                location.latitude,
                location.longitude,
                zone.latitudCentro,
                zone.longitudCentro
            ) <= zone.radioMetros
        }
        val isInsideAnySafeZone = containingZone != null
        val stateKey = "$STATE_KEY_PREFIX$profileId"
        val hadPreviousState = preferences.contains(stateKey)
        val wasInside = preferences.getBoolean(stateKey, false)

        preferences.edit().putBoolean(stateKey, isInsideAnySafeZone).apply()

        if (isInsideAnySafeZone) {
            if (!wasInside) {
                Log.i(TAG, "El wearable está dentro de ${containingZone?.nombre}")
                SafeCareAlertNotifier.dismissSafeZoneExitNotification(appContext)
            }
            return
        }

        if (!hadPreviousState || wasInside) {
            Log.w(TAG, "Salida de zona segura detectada con GPS nativo del wearable")
            GeofenceBroadcastReceiver.handleSafeZoneExit(
                context = appContext,
                zoneLabel = nearestZoneLabel(location, zones),
                triggeringLocation = location
            )
        }
    }

    // Reinicia el estado de salida registrado para un perfil.
    fun reset(profileId: String) {
        preferences.edit().remove("$STATE_KEY_PREFIX$profileId").apply()
    }

    // Obtiene el nombre de la zona segura más cercana.
    private fun nearestZoneLabel(
        location: Location,
        zones: List<mx.utng.ich.safecare.wearable.data.local.entity.ZonaSeguraEntity>
    ): String? = zones.minByOrNull { zone ->
        distanceMeters(
            location.latitude,
            location.longitude,
            zone.latitudCentro,
            zone.longitudCentro
        )
    }?.nombre

    // Calcula la distancia en metros entre dos coordenadas.
    private fun distanceMeters(
        latitude: Double,
        longitude: Double,
        centerLatitude: Double,
        centerLongitude: Double
    ): Float {
        val result = FloatArray(1)
        Location.distanceBetween(
            latitude,
            longitude,
            centerLatitude,
            centerLongitude,
            result
        )
        return result[0]
    }

    companion object {
        private const val TAG = "SafeZoneMonitor"
        private const val PREFERENCES_NAME = "safe_zone_monitor"
        private const val STATE_KEY_PREFIX = "inside_"
    }
}
