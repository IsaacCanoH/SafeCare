package mx.utng.ich.safecare.wearable.presentation.controller

import android.content.Context
import android.util.Log
import mx.utng.ich.safecare.wearable.presentation.location.LocationPermissionManager
import mx.utng.ich.safecare.wearable.presentation.location.WearLocationReader
import mx.utng.ich.safecare.wearable.presentation.sensors.DeviceStatusReader
import mx.utng.ich.safecare.wearable.presentation.ui.WearHomeUiState

class WearStatusController(
    context: Context,
    private val onUiStateChange: (WearHomeUiState) -> Unit
) {

    private val locationPermissionManager = LocationPermissionManager(context)
    private val wearLocationReader = WearLocationReader(context)
    private val deviceStatusReader = DeviceStatusReader(context)

    private var currentUiState = WearHomeUiState()

    fun updateLocationPermissionStatus() {
        updateUiState(
            currentUiState.copy(
                locationPermissionStatus =
                    locationPermissionManager.getLocationPermissionStatusText()
            )
        )
    }

    fun onPanicButtonPressed(
        onRequestLocationPermission: (Array<String>) -> Unit
    ) {
        Log.e(TAG, "SOS ACTIVADO")
        Log.e(TAG, "Mensaje: Necesito ayuda")

        updateDeviceStatus()

        val hasLocationPermission =
            locationPermissionManager.hasLocationPermission()

        if (hasLocationPermission) {
            updateUiState(
                currentUiState.copy(
                    locationPermissionStatus = "Permiso de ubicación concedido"
                )
            )

            getCurrentLocation()
        } else {
            Log.w(TAG, "No hay permiso de ubicación. Solicitando permiso...")

            onRequestLocationPermission(
                locationPermissionManager.getLocationPermissions()
            )
        }
    }

    fun requestPermissionOrGetLocation(
        onRequestLocationPermission: (Array<String>) -> Unit
    ) {
        updateDeviceStatus()

        val hasLocationPermission =
            locationPermissionManager.hasLocationPermission()

        if (hasLocationPermission) {
            updateUiState(
                currentUiState.copy(
                    locationPermissionStatus = "Permiso de ubicación concedido"
                )
            )

            getCurrentLocation()
        } else {
            onRequestLocationPermission(
                locationPermissionManager.getLocationPermissions()
            )
        }
    }

    fun handleLocationPermissionResult(
        permissions: Map<String, Boolean>
    ) {
        val locationPermissionGranted =
            locationPermissionManager.isLocationPermissionGranted(permissions)

        if (locationPermissionGranted) {
            Log.i(TAG, "Permiso de ubicación concedido")

            updateUiState(
                currentUiState.copy(
                    locationPermissionStatus = "Permiso de ubicación concedido"
                )
            )

            updateDeviceStatus()
            getCurrentLocation()
        } else {
            Log.w(TAG, "Permiso de ubicación denegado")

            updateUiState(
                currentUiState.copy(
                    locationPermissionStatus = "Permiso de ubicación denegado",
                    locationText = "No se puede obtener ubicación sin permiso"
                )
            )

            updateDeviceStatus()
        }
    }

    private fun updateDeviceStatus() {
        val deviceStatus = deviceStatusReader.getDeviceStatus()

        Log.i(TAG, deviceStatus.batteryText.replace("\n", " | "))
        Log.i(TAG, deviceStatus.connectionText.replace("\n", " | "))

        updateUiState(
            currentUiState.copy(
                batteryText = deviceStatus.batteryText,
                connectionText = deviceStatus.connectionText
            )
        )
    }

    private fun getCurrentLocation() {
        wearLocationReader.getCurrentLocation { updatedLocationText ->

            Log.i(TAG, updatedLocationText.replace("\n", " | "))

            updateUiState(
                currentUiState.copy(
                    locationText = updatedLocationText
                )
            )
        }
    }

    private fun updateUiState(
        newUiState: WearHomeUiState
    ) {
        currentUiState = newUiState
        onUiStateChange(currentUiState)
    }

    companion object {
        private const val TAG = "SafeCareSOS"
    }
}