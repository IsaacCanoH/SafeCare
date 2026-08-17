package mx.utng.ich.safecare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import mx.utng.ich.safecare.ui.screens.SafeCareApp

/**
 * Actividad principal de la aplicación móvil SafeCare para el cuidador.
 *
 * Sirve como punto de entrada de Android: habilita el diseño de borde a borde
 * y coloca la interfaz Compose [SafeCareApp] en pantalla.
 */
class MainActivity : ComponentActivity() {
    /**
     * Inicializa la interfaz principal de la aplicación móvil.
     *
     * @param savedInstanceState Estado previamente guardado de la actividad, o `null` si es la primera creación.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SafeCareApp()
        }
    }
}
