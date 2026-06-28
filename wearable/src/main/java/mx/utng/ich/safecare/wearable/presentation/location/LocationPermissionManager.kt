package mx.utng.ich.safecare.wearable.presentation.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager

class LocationPermissionManager(
    private val context: Context
) {

    fun getLocationPermissions(): Array<String> {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        return permissions.toTypedArray()
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