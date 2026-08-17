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

/**
 * Actividad principal diseÃ±ada especÃ­ficamente para la visualizaciÃ³n y gestiÃ³n de alertas crÃ­ticas en la interfaz de usuario.
 *  * Esta actividad se lanza de forma prioritaria cuando se detecta una anomalÃ­a en los signos vitales, una salida de zona segura, o una alerta manual.
 *  * Proporciona opciones rÃ¡pidas de respuesta y muestra detalles crÃ­ticos del incidente.
 */
class AlertActivity : ComponentActivity() {
    private var vibrator: Vibrator? = null
    private var displayAddress by mutableStateOf("Ubicacion desconocida")

    /** Muestra y activa los recursos de una alerta urgente. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showAsPersistentFullScreenAlert()
        startEmergencyVibration()

        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Saliste de zona segura"
        val alertType = intent.getStringExtra(EXTRA_ALERT_TYPE) ?: "FUERA_ZONA_SEGURA"
        displayAddress = intent.getStringExtra(EXTRA_ADDRESS) ?: "Ubicacion desconocida"
        val latitude = intent.getDoubleExtra("EXTRA_LATITUDE", Double.NaN)
        val longitude = intent.getDoubleExtra("EXTRA_LONGITUDE", Double.NaN)

        setContent {
            WearAlertScreen(
                message = message,
                address = displayAddress,
                alertType = alertType,
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

    /** Cierra la alerta y elimina la notificación asociada. */
    private fun dismissAlert() {
        stopVibration()
        SafeCareAlertNotifier.dismissSafeZoneExitNotification(this)
        finish()
    }

    /** Detiene la vibración al cerrar la pantalla de alerta. */
    override fun onDestroy() {
        stopVibration()
        super.onDestroy()
    }

    /** Mantiene la alerta visible a pantalla completa sobre otras vistas. */
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

    /** Inicia el patrón de vibración de emergencia. */
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

    /** Cancela cualquier vibración activa del reloj. */
    private fun stopVibration() {
        vibrator?.cancel()
    }

    /** Verifica que las coordenadas recibidas sean utilizables. */
    private fun hasCoordinates(latitude: Double, longitude: Double): Boolean {
        return !latitude.isNaN() && !longitude.isNaN()
    }

    /** Convierte coordenadas de alerta en una dirección para mostrar. */
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

    /** Convierte una dirección geocodificada a texto visible. */
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

    /** Deshabilitar el boton de atras para evitar el cierre accidental. */
    @Deprecated("Deprecated in Java")
    /** Evita cerrar la alerta urgente con el botón de regresar. */
    override fun onBackPressed() {
        // No hacer nada para evitar el cierre accidental.
    }

    companion object {
        const val EXTRA_ALERT_TYPE = "EXTRA_ALERT_TYPE"
        const val EXTRA_MESSAGE = "EXTRA_MESSAGE"
        const val EXTRA_ADDRESS = "EXTRA_ADDRESS"
    }
}