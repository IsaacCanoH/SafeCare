package mx.utng.ich.safecare.wearable.presentation.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

class LocationPermissionManager(
    private val context: Context
) {

    fun getLocationPermissions(): Array<String> {
        return getForegroundLocationPermissions()
    }

    fun getForegroundLocationPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    fun getBackgroundLocationPermission(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        } else {
            null
        }
    }

    fun hasLocationPermission(): Boolean {
        return hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
                hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    fun hasPreciseLocationPermission(): Boolean {
        return hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun hasBackgroundLocationPermission(): Boolean {
        val permission = getBackgroundLocationPermission() ?: return true
        return hasPermission(permission)
    }

    fun hasGeofencePermissions(): Boolean {
        return hasPreciseLocationPermission() && hasBackgroundLocationPermission()
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

    fun isPreciseLocationPermissionGranted(
        permissions: Map<String, Boolean>
    ): Boolean {
        return permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                hasPreciseLocationPermission()
    }

    fun getLocationPermissionStatusText(): String {
        return when {
            !hasLocationPermission() -> "Permiso de ubicacion pendiente"
            !hasPreciseLocationPermission() -> "Permiso de ubicacion precisa pendiente"
            !hasBackgroundLocationPermission() -> "Permiso de ubicacion en segundo plano pendiente"
            else -> "Permisos de ubicacion concedidos"
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
}
