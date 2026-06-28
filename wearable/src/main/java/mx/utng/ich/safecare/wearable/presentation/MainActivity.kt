package mx.utng.ich.safecare.wearable.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import mx.utng.ich.safecare.wearable.data.worker.StatusWorker
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import mx.utng.ich.safecare.wearable.data.repository.SupabaseRepository
import mx.utng.ich.safecare.wearable.presentation.controller.WearStatusController
import mx.utng.ich.safecare.wearable.presentation.geofence.GeofenceManager
import mx.utng.ich.safecare.wearable.presentation.ui.WearHomeScreen
import mx.utng.ich.safecare.wearable.presentation.ui.WearHomeUiState
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var wearStatusController: WearStatusController
    private lateinit var geofenceManager: GeofenceManager
    private val repository = SupabaseRepository()

    private var uiState by mutableStateOf(WearHomeUiState())

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            wearStatusController.handleLocationPermissionResult(permissions)
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
        setupTestGeofence()

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
    }

    private fun setupPeriodicMonitoring() {
        val monitorWorkRequest = PeriodicWorkRequestBuilder<StatusWorker>(
            15, TimeUnit.MINUTES // Mínimo permitido por Android
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SafeCareMonitor",
            ExistingPeriodicWorkPolicy.KEEP,
            monitorWorkRequest
        )
    }

    private fun setupTestGeofence() {
        // ID del perfil que estamos monitoreando (Abuelo)
        val idPerfil = "b84236e7-578d-4a1e-8761-0b5c1792f582"

        lifecycleScope.launch {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // 1. Limpiar geocercas antiguas
                geofenceManager.removeGeofences()
                
                // 2. Obtener zonas desde Supabase
                val zonas = repository.getZonasSeguras(idPerfil)
                
                // 3. Registrar cada zona en el sistema de Android
                zonas.forEach { zona ->
                    geofenceManager.addGeofence(
                        id = zona.id,
                        lat = zona.latitudCentro,
                        lng = zona.longitudCentro,
                        radiusInMeters = zona.radioMetros.toFloat()
                    )
                    android.util.Log.i("SafeCare", "Geocerca activa desde DB: ${zona.nombre}")
                }
            }
        }
    }
}
