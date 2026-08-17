package mx.utng.ich.safecare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import mx.utng.ich.safecare.ui.screens.SafeCareApp

/**
 * Punto de entrada principal (Activity) que orquesta la interfaz grÃ¡fica y la navegaciÃ³n en este mÃ³dulo.
 *  * Aloja los contenedores de Compose y gestiona el ciclo de vida primario de la experiencia de usuario.
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