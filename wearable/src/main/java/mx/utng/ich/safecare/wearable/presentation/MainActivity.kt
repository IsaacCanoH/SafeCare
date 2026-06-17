package mx.utng.ich.safecare.wearable.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import mx.utng.ich.safecare.wearable.presentation.controller.WearStatusController
import mx.utng.ich.safecare.wearable.presentation.ui.WearHomeScreen
import mx.utng.ich.safecare.wearable.presentation.ui.WearHomeUiState

class MainActivity : ComponentActivity() {

    private lateinit var wearStatusController: WearStatusController

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

        wearStatusController.updateLocationPermissionStatus()

        setContent {
            WearHomeScreen(
                uiState = uiState,
                onGetStatus = {
                    wearStatusController.onPanicButtonPressed { permissions ->
                        locationPermissionLauncher.launch(permissions)
                    }
                }
            )
        }
    }
}