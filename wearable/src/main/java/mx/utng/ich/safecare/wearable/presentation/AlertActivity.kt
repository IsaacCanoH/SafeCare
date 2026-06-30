package mx.utng.ich.safecare.wearable.presentation

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.utng.ich.safecare.wearable.presentation.geofence.SafeCareAlertNotifier
import mx.utng.ich.safecare.wearable.presentation.ui.WearAlertScreen

class AlertActivity : ComponentActivity() {
    private var vibrator: Vibrator? = null
    private var displayAddress by mutableStateOf("Ubicacion desconocida")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showAsPersistentFullScreenAlert()
        startEmergencyVibration()

        val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "Saliste de zona segura"
        displayAddress = intent.getStringExtra("EXTRA_ADDRESS") ?: "Ubicacion desconocida"
        val latitude = intent.getDoubleExtra("EXTRA_LATITUDE", Double.NaN)
        val longitude = intent.getDoubleExtra("EXTRA_LONGITUDE", Double.NaN)

        setContent {
            WearAlertScreen(
                message = message,
                address = displayAddress,
                onDismiss = {
                    dismissAlert()
                }
            )
        }

        if (hasCoordinates(latitude, longitude)) {
            lifecycleScope.launch {
                resolveAddressFromCoordinates(latitude, longitude)?.let { resolvedAddress ->
                    displayAddress = resolvedAddress
                }
            }
        }
    }

    private fun dismissAlert() {
        stopVibration()
        SafeCareAlertNotifier.dismissSafeZoneExitNotification(this)
        finish()
    }

    override fun onDestroy() {
        stopVibration()
        super.onDestroy()
    }

    private fun showAsPersistentFullScreenAlert() {
        // Mantener la pantalla encendida y mostrar sobre el bloqueo.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
    }

    private fun startEmergencyVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        vibrator?.let {
            if (it.hasVibrator()) {
                val pattern = longArrayOf(0, 500, 200, 500)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(pattern, 0)
                }
            }
        }
    }

    private fun stopVibration() {
        vibrator?.cancel()
    }

    private fun hasCoordinates(latitude: Double, longitude: Double): Boolean {
        return !latitude.isNaN() && !longitude.isNaN()
    }

    private suspend fun resolveAddressFromCoordinates(
        latitude: Double,
        longitude: Double
    ): String? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) {
            return@withContext null
        }

        runCatching {
            val geocoder = Geocoder(this@AlertActivity, Locale.getDefault())
            @Suppress("DEPRECATION")
            val address = geocoder.getFromLocation(latitude, longitude, 1)
                ?.firstOrNull()

            address?.toDisplayAddress()
        }.getOrNull()
    }

    private fun Address.toDisplayAddress(): String? {
        val street = listOfNotNull(thoroughfare, subThoroughfare)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .takeIf { it.isNotBlank() }
            ?: featureName?.takeIf { it.isNotBlank() }

        val neighborhood = subLocality
            ?.takeIf { it.isNotBlank() }
            ?.let { "Col. $it" }

        val city = listOfNotNull(locality, subAdminArea, adminArea)
            .firstOrNull { it.isNotBlank() }

        val compactAddress = listOfNotNull(street, neighborhood, city)
            .distinct()
            .joinToString(", ")

        return compactAddress.takeIf { it.isNotBlank() }
            ?: getAddressLine(0)?.takeIf { it.isNotBlank() }
    }

    // Deshabilitar el boton de atras para evitar el cierre accidental.
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // No hacer nada para evitar el cierre accidental.
    }
}
