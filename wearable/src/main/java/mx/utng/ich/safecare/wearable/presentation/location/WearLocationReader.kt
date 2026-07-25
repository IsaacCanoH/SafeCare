package mx.utng.ich.safecare.wearable.presentation.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.SystemClock
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class WearLocationReader(
    context: Context
) {
    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(LocationManager::class.java)

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(
        onLocationTextChange: (String) -> Unit
    ) {
        onLocationTextChange("Obteniendo ubicación GPS del reloj...")

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            onLocationTextChange("Activa el GPS del reloj")
            return
        }

        locationManager.getCurrentLocation(
            LocationManager.GPS_PROVIDER,
            CancellationSignal(),
            appContext.mainExecutor
        ) { location ->
            if (location != null && isUsableWatchGpsLocation(location)) {
                onLocationTextChange(
                    "Lat: ${location.latitude}\n" +
                            "Lng: ${location.longitude}\n" +
                            "Precisión: ${location.accuracy}m"
                )
            } else {
                onLocationTextChange("Esperando una lectura GPS reciente del reloj")
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocationData(): Location? {
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return null
        }

        val location = suspendCoroutine<Location?> { continuation ->
            locationManager.getCurrentLocation(
                LocationManager.GPS_PROVIDER,
                CancellationSignal(),
                appContext.mainExecutor
            ) { result ->
                continuation.resume(result)
            }
        }
        return location?.takeIf(::isUsableWatchGpsLocation)
    }

    private fun isUsableWatchGpsLocation(location: Location): Boolean {
        return location.provider == LocationManager.GPS_PROVIDER &&
                location.latitude in -90.0..90.0 &&
                location.longitude in -180.0..180.0 &&
                locationAgeMillis(location) <= MAX_LOCATION_AGE_MILLIS &&
                (!location.hasAccuracy() || location.accuracy <= MAX_ACCURACY_METERS)
    }

    private fun locationAgeMillis(location: Location): Long {
        if (location.elapsedRealtimeNanos <= 0L) return Long.MAX_VALUE
        return (
            SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos
        ).coerceAtLeast(0L) / 1_000_000L
    }

    companion object {
        private const val MAX_LOCATION_AGE_MILLIS = 30_000L
        private const val MAX_ACCURACY_METERS = 200f
    }
}
