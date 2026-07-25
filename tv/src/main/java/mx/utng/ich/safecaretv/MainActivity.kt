package mx.utng.ich.safecaretv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.utng.ich.safecaretv.ui.SafeCareTvApp
import mx.utng.ich.safecaretv.ui.theme.SafeCareTheme
import mx.utng.ich.safecaretv.ui.viewmodel.TvAuthViewModel

class MainActivity : ComponentActivity() {
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
