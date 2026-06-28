package mx.utng.ich.safecare.wearable.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import mx.utng.ich.safecare.wearable.presentation.ui.WearAlertScreen

class AlertActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "Saliste de zona segura"
        val address = intent.getStringExtra("EXTRA_ADDRESS") ?: "Ubicación desconocida"

        setContent {
            WearAlertScreen(
                message = message,
                address = address,
                onDismiss = {
                    finish()
                }
            )
        }
    }
}
