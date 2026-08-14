package mx.utng.ich.safecare.wearable.presentation.controller

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mx.utng.ich.safecare.wearable.data.local.SafeCareProfileResolver
import mx.utng.ich.safecare.wearable.data.datalayer.WearIdentityStore
import mx.utng.ich.safecare.wearable.data.local.database.DatabaseProvider
import mx.utng.ich.safecare.wearable.data.local.entity.AlertaEntity
import mx.utng.ich.safecare.wearable.data.local.entity.SmartwatchEntity
import mx.utng.ich.safecare.wearable.data.local.entity.UbicacionEntity
import mx.utng.ich.safecare.wearable.presentation.location.LocationPermissionManager
import mx.utng.ich.safecare.wearable.presentation.location.WearLocationReader
import mx.utng.ich.safecare.wearable.presentation.sensors.DeviceStatusReader
import mx.utng.ich.safecare.wearable.presentation.ui.WearHomeUiState
import mx.utng.ich.safecare.wearable.data.repository.SupabaseRepository

class WearStatusController(
    private val context: Context,
    private val onUiStateChange: (WearHomeUiState) -> Unit
) {

    private val locationPermissionManager = LocationPermissionManager(context)
    private val wearLocationReader = WearLocationReader(context)
    private val deviceStatusReader = DeviceStatusReader(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    private var currentUiState = WearHomeUiState()

    // Actualiza en la interfaz el estado de los permisos de ubicación.
    fun updateLocationPermissionStatus() {
        updateUiState(
            currentUiState.copy(
                locationPermissionStatus =
                    locationPermissionManager.getLocationPermissionStatusText()
            )
        )
    }

    // Genera y publica una alerta SOS con la ubicación disponible.
    fun onPanicButtonPressed(
        onRequestLocationPermission: (Array<String>) -> Unit
    ) {
        Log.e(TAG, "--- INICIANDO FLUJO SOS ---")
        
        updateDeviceStatus()

        val hasLocationPermission = locationPermissionManager.hasLocationPermission()

        if (hasLocationPermission) {
            scope.launch {
                val serialIdentificador = WearIdentityStore(context).getOrCreateWatchId()
                val database = DatabaseProvider.getDatabase(context)
                val idPerfil = SafeCareProfileResolver.resolveProfileId(
                    database = database,
                    watchId = serialIdentificador
                ) ?: run {
                    Log.e(TAG, "SOS descartado: el reloj no tiene un perfil vinculado")
                    return@launch
                }
                val profileName = database.perfilMonitoreadoDao()
                    .obtenerPorId(idPerfil)
                    ?.nombre
                    ?.takeIf { it.isNotBlank() }
                    ?: "El perfil monitoreado"
                val locationData = wearLocationReader.getCurrentLocationData()
                val alertaDao = database.alertaDao()
                val ubicacionDao = database.ubicacionDao()
                val smartwatchDao = database.smartwatchDao()

                // 1. Guardar localmente en Room
                val batteryLevel = deviceStatusReader.getBatteryLevel()
                val isOnline = deviceStatusReader.isOnline()

                val smartwatchLocal = SmartwatchEntity(
                    idSmartwatch = serialIdentificador,
                    numeroSerie = serialIdentificador,
                    bateria = batteryLevel,
                    conexion = if (isOnline) "online" else "offline",
                    estado = if (isOnline) "ACTIVO" else "INACTIVO",
                    idPerfil = idPerfil
                )
                smartwatchDao.insertarOActualizar(smartwatchLocal)

                var localUbicacionId: String? = null
                if (locationData != null) {
                    val nuevaUbicacion = UbicacionEntity(
                        latitud = locationData.latitude,
                        longitud = locationData.longitude,
                        idSmartwatch = serialIdentificador
                    )
                    ubicacionDao.insertar(nuevaUbicacion)
                    if (isOnline) {
                        SupabaseRepository().saveLocation(nuevaUbicacion)
                    }
                    localUbicacionId = nuevaUbicacion.idUbicacion
                }

                val alertaLocal = AlertaEntity(
                    tipoAlerta = "SOS",
                    descripcion = "$profileName activó una alerta SOS desde su reloj",
                    idPerfil = idPerfil,
                    idUbicacion = localUbicacionId
                )
                alertaDao.insertar(alertaLocal)
                if (isOnline) {
                    val savedRemotely = SupabaseRepository().saveAlert(alertaLocal)
                    if (!savedRemotely) {
                        Log.w(TAG, "SOS pendiente de sincronización por el móvil")
                    }
                }
                
                Log.i(TAG, "SOS guardado localmente en Room")
                Log.i(TAG, "--- FLUJO SOS FINALIZADO ---")
            }
            getCurrentLocation()
        } else {
            onRequestLocationPermission(locationPermissionManager.getLocationPermissions())
        }
    }

    // Solicita permisos o inicia la lectura de ubicación actual.
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

    // Continúa el flujo de ubicación tras responder a los permisos.
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

    // Lee y publica el estado actual del reloj.
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

    // Solicita la ubicación actual para actualizar la interfaz.
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

    // Actualiza el estado observable que consume la interfaz Wear.
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
