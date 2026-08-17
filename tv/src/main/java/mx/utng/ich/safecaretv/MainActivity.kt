package mx.utng.ich.safecaretv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.utng.ich.safecaretv.ui.SafeCareTvApp
import mx.utng.ich.safecaretv.ui.theme.SafeCareTheme
import mx.utng.ich.safecaretv.ui.viewmodel.TvAuthViewModel

/**
 * Punto de entrada principal (Activity) que orquesta la interfaz grÃ¡fica y la navegaciÃ³n en este mÃ³dulo.
 *  * Aloja los contenedores de Compose y gestiona el ciclo de vida primario de la experiencia de usuario.
 */
class MainActivity : ComponentActivity() {
    /** Inicializa la interfaz principal de SafeCare para TV. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SafeCareTheme {
                val authViewModel: TvAuthViewModel = viewModel()
                SafeCareTvApp(authViewModel)
            }
        }
    }
}