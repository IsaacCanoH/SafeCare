package mx.utng.ich.safecare.wearable.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import mx.utng.ich.safecare.wearable.R
import mx.utng.ich.safecare.wearable.presentation.theme.SafeCareTheme

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var locationPermissionStatus by mutableStateOf("Permiso de ubicación pendiente")
    private var locationText by mutableStateOf("Ubicación todavía no consultada")

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val fineLocationGranted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

            val coarseLocationGranted =
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (fineLocationGranted || coarseLocationGranted) {
                locationPermissionStatus = "Permiso de ubicación concedido"
                getCurrentLocation()
            } else {
                locationPermissionStatus = "Permiso de ubicación denegado"
                locationText = "No se puede obtener ubicación sin permiso"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        updateLocationPermissionStatus()

        setContent {
            WearApp(
                greetingName = "SafeCare",
                locationPermissionStatus = locationPermissionStatus,
                locationText = locationText,
                onGetLocation = {
                    requestPermissionOrGetLocation()
                }
            )
        }
    }

    private fun updateLocationPermissionStatus() {
        val fineLocationPermission =
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)

        val coarseLocationPermission =
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)

        locationPermissionStatus =
            if (
                fineLocationPermission == PackageManager.PERMISSION_GRANTED ||
                coarseLocationPermission == PackageManager.PERMISSION_GRANTED
            ) {
                "Permiso de ubicación concedido"
            } else {
                "Permiso de ubicación pendiente"
            }
    }

    private fun requestPermissionOrGetLocation() {
        val fineLocationPermission =
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)

        val coarseLocationPermission =
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)

        val hasLocationPermission =
            fineLocationPermission == PackageManager.PERMISSION_GRANTED ||
                    coarseLocationPermission == PackageManager.PERMISSION_GRANTED

        if (hasLocationPermission) {
            locationPermissionStatus = "Permiso de ubicación concedido"
            getCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        locationText = "Obteniendo ubicación..."

        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->

            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                val accuracy = location.accuracy

                locationText =
                    "Lat: $latitude\nLng: $longitude\nPrecisión: ${accuracy}m"
            } else {
                locationText = "No se pudo obtener ubicación"
            }

        }.addOnFailureListener { exception ->
            locationText = "Error: ${exception.message}"
        }
    }
}

@Composable
fun WearApp(
    greetingName: String,
    locationPermissionStatus: String = "Permiso de ubicación pendiente",
    locationText: String = "Ubicación todavía no consultada",
    onGetLocation: () -> Unit = {}
) {
    SafeCareTheme {
        AppScaffold {
            val listState = rememberTransformingLazyColumnState()
            val transformationSpec = rememberTransformationSpec()

            ScreenScaffold(
                scrollState = listState,
                edgeButton = {
                    EdgeButton(
                        onClick = { },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                    ) {
                        Text("SOS")
                    }
                },
            ) { contentPadding ->

                TransformingLazyColumn(
                    contentPadding = contentPadding,
                    state = listState
                ) {
                    item {
                        ListHeader(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                        ) {
                            Text(text = stringResource(R.string.hello_world, greetingName))
                        }
                    }

                    item {
                        Text(
                            text = locationPermissionStatus,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .transformedHeight(this, transformationSpec)
                        )
                    }

                    item {
                        Text(
                            text = locationText,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .transformedHeight(this, transformationSpec)
                        )
                    }

                    item {
                        Button(
                            onClick = onGetLocation,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                        ) {
                            Text("Obtener ubicación")
                        }
                    }
                }
            }
        }
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
fun DefaultPreview() {
    WearApp("Preview Android")
}