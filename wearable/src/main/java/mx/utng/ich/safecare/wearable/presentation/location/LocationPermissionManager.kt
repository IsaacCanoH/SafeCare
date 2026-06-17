package mx.utng.ich.safecare.wearable.presentation.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager

class LocationPermissionManager(
    private val context: Context
) {

    fun getLocationPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    fun hasLocationPermission(): Boolean {
        val fineLocationPermission =
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)

        val coarseLocationPermission =
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)

        return fineLocationPermission == PackageManager.PERMISSION_GRANTED ||
                coarseLocationPermission == PackageManager.PERMISSION_GRANTED
    }

    fun isLocationPermissionGranted(
        permissions: Map<String, Boolean>
    ): Boolean {
        val fineLocationGranted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

        val coarseLocationGranted =
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        return fineLocationGranted || coarseLocationGranted
    }

    fun getLocationPermissionStatusText(): String {
        return if (hasLocationPermission()) {
            "Permiso de ubicación concedido"
        } else {
            "Permiso de ubicación pendiente"
        }
    }
}