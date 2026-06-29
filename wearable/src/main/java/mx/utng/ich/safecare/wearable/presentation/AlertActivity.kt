package mx.utng.ich.safecare.wearable.presentation

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import mx.utng.ich.safecare.wearable.presentation.geofence.SafeCareAlertNotifier
import mx.utng.ich.safecare.wearable.presentation.ui.WearAlertScreen

class AlertActivity : ComponentActivity() {
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showAsPersistentFullScreenAlert()
        startEmergencyVibration()

        val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "Saliste de zona segura"
        val address = intent.getStringExtra("EXTRA_ADDRESS") ?: "Ubicacion desconocida"

        setContent {
            WearAlertScreen(
                message = message,
                address = address,
                onDismiss = {
                    dismissAlert()
                }
            )
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
        // Mantener la pantalla encendida y mostrar sobre el bloqueo
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
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        vibrator?.let {
            if (it.hasVibrator()) {
                val pattern = longArrayOf(0, 500, 200, 500)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createWaveform(pattern, 0)) // 0 means repeat from index 0
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

    // Deshabilitar el botón de atrás para obligar a tocar la pantalla
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // No hacer nada para evitar el cierre accidental
    }
}
