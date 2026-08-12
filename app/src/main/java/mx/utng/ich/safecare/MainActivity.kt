package mx.utng.ich.safecare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import mx.utng.ich.safecare.ui.screens.SafeCareApp

class MainActivity : ComponentActivity() {
    // Inicializa la interfaz principal de la aplicación móvil.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SafeCareApp()
        }
    }
}
