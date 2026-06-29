package mx.utng.ich.safecare.wearable.presentation.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SafeZoneGeofence(
    val id: String,
    val lat: Double,
    val lng: Double,
    val radiusInMeters: Float
)

class GeofenceManager(context: Context) {

    private val appContext = context.applicationContext
    private val geofencingClient = LocationServices.getGeofencingClient(appContext)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(appContext, GeofenceBroadcastReceiver::class.java).apply {
            action = ACTION_GEOFENCE_EVENT
        }
        PendingIntent.getBroadcast(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    suspend fun replaceGeofences(
        zones: List<SafeZoneGeofence>
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Tasks.await(geofencingClient.removeGeofences(geofencePendingIntent))
            Log.i(TAG, "Geocercas anteriores eliminadas")

            if (zones.isEmpty()) {
                Log.w(TAG, "No hay zonas activas para registrar")
                return@withContext Result.success(0)
            }

            val geofences = zones.map { zone ->
                Geofence.Builder()
                    .setRequestId(zone.id)
                    .setCircularRegion(zone.lat, zone.lng, zone.radiusInMeters)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(
                        Geofence.GEOFENCE_TRANSITION_EXIT or
                                Geofence.GEOFENCE_TRANSITION_ENTER
                    )
                    .setNotificationResponsiveness(NOTIFICATION_RESPONSIVENESS_MS)
                    .build()
            }

            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(
                    GeofencingRequest.INITIAL_TRIGGER_ENTER or
                            GeofencingRequest.INITIAL_TRIGGER_EXIT
                )
                .addGeofences(geofences)
                .build()

            Tasks.await(geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent))
            Log.i(TAG, "Geocercas registradas correctamente: ${zones.size}")
            Result.success(zones.size)
        } catch (exception: Exception) {
            Log.e(TAG, "No se pudieron registrar geocercas: ${exception.geofenceMessage()}", exception)
            Result.failure(exception)
        }
    }

    private fun Throwable.geofenceMessage(): String {
        val statusCode = (this as? ApiException)?.statusCode
        val statusText = statusCode?.let { GeofenceStatusCodes.getStatusCodeString(it) }
        return statusText ?: message ?: javaClass.simpleName
    }

    companion object {
        private const val TAG = "GeofenceManager"
        private const val ACTION_GEOFENCE_EVENT =
            "mx.utng.ich.safecare.wearable.action.GEOFENCE_EVENT"
        private const val NOTIFICATION_RESPONSIVENESS_MS = 10_000
    }
}
