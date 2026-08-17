package mx.utng.ich.safecare.wearable.presentation.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Gestor de seguridad responsable de la evaluaciÃ³n, solicitud y verificaciÃ³n de los permisos de geolocalizaciÃ³n en tiempo de ejecuciÃ³n.
 *  * Maneja la lÃ³gica de permisos en primero y segundo plano, garantizando el cumplimiento de las polÃ­ticas de privacidad.
 */
class LocationPermissionManager(
    private val context: Context
) {

    /** Devuelve los permisos necesarios para obtener ubicación. */
    fun getLocationPermissions(): Array<String> {
        return getForegroundLocationPermissions()
    }

    /** Devuelve los permisos de ubicación requeridos en primer plano. */
    fun getForegroundLocationPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    /** Devuelve el permiso de ubicación en segundo plano si aplica. */
    fun getBackgroundLocationPermission(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        } else {
            null
        }
    }

    /** Verifica si se otorgó algún permiso de ubicación. */
    fun hasLocationPermission(): Boolean {
        return hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
                hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    /** Verifica si se otorgó el permiso de ubicación precisa. */
    fun hasPreciseLocationPermission(): Boolean {
        return hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    /** Verifica si se otorgó ubicación en segundo plano. */
    fun hasBackgroundLocationPermission(): Boolean {
        val permission = getBackgroundLocationPermission() ?: return true
        return hasPermission(permission)
    }

    /** Verifica si existen permisos suficientes para geocercas. */
    fun hasGeofencePermissions(): Boolean {
        return hasPreciseLocationPermission() && hasBackgroundLocationPermission()
    }

    /** Evalúa el resultado recibido al solicitar ubicación. */
    fun isLocationPermissionGranted(
        permissions: Map<String, Boolean>
    ): Boolean {
        val fineLocationGranted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

        val coarseLocationGranted =
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        return fineLocationGranted || coarseLocationGranted
    }

    /** Evalúa el resultado recibido al solicitar ubicación precisa. */
    fun isPreciseLocationPermissionGranted(
        permissions: Map<String, Boolean>
    ): Boolean {
        return permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                hasPreciseLocationPermission()
    }

    /** Genera un texto legible sobre el permiso de ubicación. */
    fun getLocationPermissionStatusText(): String {
        return when {
            !hasLocationPermission() -> "Permiso de ubicacion pendiente"
            !hasPreciseLocationPermission() -> "Permiso de ubicacion precisa pendiente"
            !hasBackgroundLocationPermission() -> "Permiso de ubicacion en segundo plano pendiente"
            else -> "Permisos de ubicacion concedidos"
        }
    }

    /** Comprueba si un permiso concreto fue otorgado. */
    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
}