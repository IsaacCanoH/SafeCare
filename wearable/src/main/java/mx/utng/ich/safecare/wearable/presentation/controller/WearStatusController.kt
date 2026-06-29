package mx.utng.ich.safecare.wearable.presentation.controller

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mx.utng.ich.safecare.wearable.data.local.SafeCareProfileResolver
import mx.utng.ich.safecare.wearable.data.local.database.DatabaseProvider
import mx.utng.ich.safecare.wearable.data.local.entity.AlertaEntity
import mx.utng.ich.safecare.wearable.data.local.entity.SmartwatchEntity
import mx.utng.ich.safecare.wearable.data.local.entity.UbicacionEntity
import mx.utng.ich.safecare.wearable.presentation.location.LocationPermissionManager
import mx.utng.ich.safecare.wearable.presentation.location.WearLocationReader
import mx.utng.ich.safecare.wearable.presentation.sensors.DeviceStatusReader
import mx.utng.ich.safecare.wearable.presentation.ui.WearHomeUiState

class WearStatusController(
    private val context: Context,
    private val onUiStateChange: (WearHomeUiState) -> Unit
) {

    private val locationPermissionManager = LocationPermissionManager(context)
    private val wearLocationReader = WearLocationReader(context)
    private val deviceStatusReader = DeviceStatusReader(context)
    private val scope = CoroutineScope(Dispatchers.IO)

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
        Log.e(TAG, "--- INICIANDO FLUJO SOS ---")
        
        updateDeviceStatus()

        val hasLocationPermission = locationPermissionManager.hasLocationPermission()

        if (hasLocationPermission) {
            scope.launch {
                val serialIdentificador = Build.MODEL 
                val database = DatabaseProvider.getDatabase(context)
                val idPerfil = SafeCareProfileResolver.resolveProfileId(database)
                val locationData = wearLocationReader.getCurrentLocationData()
                val alertaDao = database.alertaDao()
                val ubicacionDao = database.ubicacionDao()
                val smartwatchDao = database.smartwatchDao()

                // 1. Guardar localmente en Room
                val batteryLevel = deviceStatusReader.getBatteryLevel()
                val isOnline = deviceStatusReader.isOnline()

                val smartwatchLocal = SmartwatchEntity(
                    numeroSerie = serialIdentificador,
                    bateria = batteryLevel,
                    conexion = if (isOnline) "online" else "offline",
                    idPerfil = idPerfil,
                    sincronizado = false
                )
                smartwatchDao.insertarOActualizar(smartwatchLocal)

                var localUbicacionId: Long? = null
                if (locationData != null) {
                    val nuevaUbicacion = UbicacionEntity(
                        latitud = locationData.latitude,
                        longitud = locationData.longitude,
                        idSmartwatch = serialIdentificador,
                        sincronizada = false
                    )
                    localUbicacionId = ubicacionDao.insertar(nuevaUbicacion)
                }

                val alertaLocal = AlertaEntity(
                    tipoAlerta = "SOS",
                    descripcion = "SOS activado desde el SmartWatch",
                    idPerfil = idPerfil,
                    idUbicacion = localUbicacionId?.toString(),
                    sincronizada = false
                )
                alertaDao.insertar(alertaLocal)
                
                Log.i(TAG, "SOS guardado localmente en Room")
                Log.i(TAG, "--- FLUJO SOS FINALIZADO ---")
            }
            getCurrentLocation()
        } else {
            onRequestLocationPermission(locationPermissionManager.getLocationPermissions())
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
