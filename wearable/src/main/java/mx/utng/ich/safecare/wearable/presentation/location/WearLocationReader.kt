package mx.utng.ich.safecare.wearable.presentation.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

class WearLocationReader(
    private val context: Context
) {

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(
        onLocationTextChange: (String) -> Unit
    ) {
        onLocationTextChange("Obteniendo ubicación...")

        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->

            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                val accuracy = location.accuracy

                onLocationTextChange(
                    "Lat: $latitude\nLng: $longitude\nPrecisión: ${accuracy}m"
                )
            } else {
                onLocationTextChange("No se pudo obtener ubicación")
            }

        }.addOnFailureListener { exception ->
            onLocationTextChange("Error: ${exception.message}")
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocationData(): android.location.Location? {
        val cancellationTokenSource = CancellationTokenSource()
        return try {
            val task = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            )
            com.google.android.gms.tasks.Tasks.await(task)
        } catch (e: Exception) {
            null
        }
    }
}
