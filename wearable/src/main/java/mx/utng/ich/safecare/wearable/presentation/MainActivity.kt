package mx.utng.ich.safecare.wearable.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mx.utng.ich.safecare.wearable.data.local.SafeCareProfileResolver
import mx.utng.ich.safecare.wearable.data.local.database.DatabaseProvider
import mx.utng.ich.safecare.wearable.data.local.entity.ZonaSeguraEntity
import mx.utng.ich.safecare.wearable.data.worker.StatusWorker
import mx.utng.ich.safecare.wearable.presentation.controller.WearStatusController
import mx.utng.ich.safecare.wearable.presentation.geofence.GeofenceManager
import mx.utng.ich.safecare.wearable.presentation.geofence.SafeZoneGeofence
import mx.utng.ich.safecare.wearable.presentation.location.LocationPermissionManager
import mx.utng.ich.safecare.wearable.presentation.location.LocationTrackingService
import mx.utng.ich.safecare.wearable.presentation.ui.WearHomeScreen
import mx.utng.ich.safecare.wearable.presentation.ui.WearHomeUiState

class MainActivity : ComponentActivity() {

    private lateinit var wearStatusController: WearStatusController
    private lateinit var geofenceManager: GeofenceManager

    private val locationPermissionManager by lazy {
        LocationPermissionManager(this)
    }

    private var geofenceSetupJob: Job? = null
    private var uiState by mutableStateOf(WearHomeUiState())

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            wearStatusController.handleLocationPermissionResult(permissions)

            if (locationPermissionManager.isPreciseLocationPermissionGranted(permissions)) {
                requestBackgroundLocationPermissionOrSetupGeofences()
            } else {
                Log.w(TAG, "No se registran geocercas sin ubicacion precisa")
                requestNotificationPermissionIfNeeded()
            }
        }

    private val backgroundLocationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            wearStatusController.updateLocationPermissionStatus()

            if (!granted && !locationPermissionManager.hasBackgroundLocationPermission()) {
                Log.w(TAG, "La ubicacion en segundo plano no fue concedida")
            }

            val notificationRequestStarted = requestNotificationPermissionIfNeeded()
            if (!notificationRequestStarted) {
                startLocationTrackingIfPossible()
            }
            setupGeofences()
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                Log.i(TAG, "Permiso de notificaciones concedido")
            } else {
                Log.w(TAG, "Permiso de notificaciones denegado")
            }
            startLocationTrackingIfPossible()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wearStatusController =
            WearStatusController(this) { updatedUiState ->
                uiState = updatedUiState
            }

        geofenceManager = GeofenceManager(this)

        wearStatusController.updateLocationPermissionStatus()
        setupPeriodicMonitoring()

        setContent {
            WearHomeScreen(
                uiState = uiState,
                onPanicButtonLongPress = {
                    wearStatusController.onPanicButtonPressed { permissions ->
                        locationPermissionLauncher.launch(permissions)
                    }
                }
            )
        }

        requestMonitoringPermissionsOrSetupGeofences()
    }

    private fun setupPeriodicMonitoring() {
        val monitorWorkRequest = PeriodicWorkRequestBuilder<StatusWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SafeCareMonitor",
            ExistingPeriodicWorkPolicy.KEEP,
            monitorWorkRequest
        )
    }

    private fun requestMonitoringPermissionsOrSetupGeofences() {
        if (!locationPermissionManager.hasPreciseLocationPermission()) {
            locationPermissionLauncher.launch(
                locationPermissionManager.getForegroundLocationPermissions()
            )
            return
        }

        requestBackgroundLocationPermissionOrSetupGeofences()
    }

    private fun requestBackgroundLocationPermissionOrSetupGeofences() {
        val backgroundPermission = locationPermissionManager.getBackgroundLocationPermission()

        if (
            backgroundPermission != null &&
            !locationPermissionManager.hasBackgroundLocationPermission()
        ) {
            backgroundLocationPermissionLauncher.launch(backgroundPermission)
            return
        }

        val notificationRequestStarted = requestNotificationPermissionIfNeeded()
        if (!notificationRequestStarted) {
            startLocationTrackingIfPossible()
        }
        setupGeofences()
    }

    private fun requestNotificationPermissionIfNeeded(): Boolean {
        return if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            true
        } else {
            false
        }
    }

    private fun startLocationTrackingIfPossible() {
        if (!locationPermissionManager.hasPreciseLocationPermission()) {
            Log.w(TAG, "Tracking de ubicacion no iniciado: falta ubicacion precisa")
            return
        }

        LocationTrackingService.start(this)
    }

    private fun setupGeofences() {
        wearStatusController.updateLocationPermissionStatus()

        if (!locationPermissionManager.hasGeofencePermissions()) {
            Log.w(
                TAG,
                "Geocercas no registradas. precise=" +
                        "${locationPermissionManager.hasPreciseLocationPermission()}, " +
                        "background=${locationPermissionManager.hasBackgroundLocationPermission()}"
            )
            return
        }

        geofenceSetupJob?.cancel()
        geofenceSetupJob = lifecycleScope.launch {
            val database = DatabaseProvider.getDatabase(this@MainActivity)
            val idPerfil = SafeCareProfileResolver.resolveProfileId(database)
            val zonasLocales = database.zonaSeguraDao().obtenerZonasActivas(idPerfil)

            actualizarGeofencingEnAndroid(zonasLocales)

            if (zonasLocales.isNotEmpty()) {
                Log.i(
                    TAG,
                    "Geocercas cargadas desde Room: ${zonasLocales.size}, perfil=$idPerfil"
                )
            } else {
                Log.w(TAG, "No hay zonas activas en Room para el perfil=$idPerfil")
            }
        }
    }

    private suspend fun actualizarGeofencingEnAndroid(zonas: List<ZonaSeguraEntity>) {
        val safeZones = zonas.map { zona ->
            SafeZoneGeofence(
                id = zona.idZona,
                lat = zona.latitudCentro,
                lng = zona.longitudCentro,
                radiusInMeters = zona.radioMetros.toFloat()
            )
        }

        geofenceManager.replaceGeofences(safeZones)
            .onSuccess { count ->
                Log.i(TAG, "Geocercas activas confirmadas: $count")
            }
            .onFailure { exception ->
                Log.e(TAG, "Fallo al activar geocercas", exception)
            }
    }

    companion object {
        private const val TAG = "SafeCareGeofences"
    }
}
