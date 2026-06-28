package mx.utng.ich.safecare.wearable.presentation.controller

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mx.utng.ich.safecare.wearable.data.model.Alerta
import mx.utng.ich.safecare.wearable.data.model.SmartWatch
import mx.utng.ich.safecare.wearable.data.model.TipoAlerta
import mx.utng.ich.safecare.wearable.data.model.TipoConexion
import mx.utng.ich.safecare.wearable.data.model.Ubicacion
import mx.utng.ich.safecare.wearable.data.repository.SupabaseRepository
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
    private val repository = SupabaseRepository()
    private val scope = CoroutineScope(Dispatchers.IO) // CAMBIADO A IO para evitar bloqueos de ubicación

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
                val locationData = wearLocationReader.getCurrentLocationData()
                val serialIdentificador = Build.MODEL 
                
                Log.d(TAG, "ID de hardware detectado (Modelo): $serialIdentificador")
                
                // ID de prueba
                val idPerfil = "b84236e7-578d-4a1e-8761-0b5c1792f582" 

                // 1. Actualizar estado del Smartwatch y obtener su UUID real
                val batteryLevel = deviceStatusReader.getBatteryLevel()
                val isOnline = deviceStatusReader.isOnline()
                
                Log.d(TAG, "1. Actualizando estado (Batería: $batteryLevel%)...")
                val swUpdated = repository.updateSmartWatchStatus(
                    numeroSerie = serialIdentificador,
                    bateria = batteryLevel,
                    conexion = if (isOnline) "online" else "offline"
                )

                val uuidSmartwatch = swUpdated?.id ?: ""
                Log.d(TAG, "UUID del Reloj desde DB: '$uuidSmartwatch'")

                // 2. Insertar Ubicación vinculada al UUID
                var idUbicacion: String? = null
                if (locationData != null && uuidSmartwatch.isNotEmpty()) {
                    Log.d(TAG, "2. Insertando ubicación en DB...")
                    val nuevaUbicacion = repository.insertUbicacion(
                        Ubicacion(
                            latitud = locationData.latitude,
                            longitud = locationData.longitude,
                            idSmartwatch = uuidSmartwatch
                        )
                    )
                    idUbicacion = nuevaUbicacion?.id
                    Log.i(TAG, "Ubicación guardada con ID: '$idUbicacion'")
                } else {
                    Log.e(TAG, "ERROR: No se pudo guardar ubicación. LocationData: $locationData, UUID: '$uuidSmartwatch'")
                }

                // 3. Insertar Alerta vinculada a la ubicación
                Log.d(TAG, "3. Insertando alerta...")
                val alerta = Alerta(
                    tipoAlerta = TipoAlerta.SOS,
                    descripcion = "SOS activado desde el SmartWatch",
                    idPerfil = idPerfil,
                    idUbicacion = idUbicacion
                )
                
                repository.insertAlerta(alerta)
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