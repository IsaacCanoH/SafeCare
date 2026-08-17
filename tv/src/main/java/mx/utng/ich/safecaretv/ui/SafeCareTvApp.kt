package mx.utng.ich.safecaretv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.utng.ich.safecaretv.ui.login.TvLoginScreen
import mx.utng.ich.safecaretv.ui.home.TvHomeScreen
import mx.utng.ich.safecaretv.ui.viewmodel.TvAuthState
import mx.utng.ich.safecaretv.ui.viewmodel.TvAuthViewModel
import mx.utng.ich.safecaretv.ui.viewmodel.YouTubeViewModel
import mx.utng.ich.safecaretv.ui.viewmodel.MonitoredProfilesViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.utng.ich.safecaretv.ui.profile.TvProfileDetailScreen
import mx.utng.ich.safecaretv.ui.viewmodel.ProfilesUiState
import mx.utng.ich.safecaretv.ui.alert.TvFullScreenAlert
import mx.utng.ich.safecaretv.ui.viewmodel.TvAlertsViewModel
import mx.utng.ich.safecaretv.ui.settings.TvAlertTonesScreen

@Composable
/** Coordina la navegación y pantallas de la aplicación para TV. */
fun SafeCareTvApp(authViewModel: TvAuthViewModel) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()

    when (val state = authState) {
        TvAuthState.CheckingSession -> TvLoginScreen(
            isLoading = true,
            errorMessage = null,
            onLogin = { _, _ -> }
        )
        TvAuthState.SignedOut -> TvLoginScreen(
            isLoading = false,
            errorMessage = null,
            onLogin = authViewModel::login
        )
        TvAuthState.Loading -> TvLoginScreen(
            isLoading = true,
            errorMessage = null,
            onLogin = { _, _ -> }
        )
        is TvAuthState.Error -> TvLoginScreen(
            isLoading = false,
            errorMessage = state.message,
            onLogin = authViewModel::login,
            onInputChanged = authViewModel::dismissError
        )
        is TvAuthState.SignedIn -> {
            val youTubeViewModel: YouTubeViewModel = viewModel()
            val profilesViewModel: MonitoredProfilesViewModel = viewModel()
            val alertsViewModel: TvAlertsViewModel = viewModel()
            val profilesState by profilesViewModel.state.collectAsStateWithLifecycle()
            val activeAlert by alertsViewModel.activeAlert.collectAsStateWithLifecycle()
            var selectedProfileId by remember { mutableStateOf<String?>(null) }
            var showingAlertTones by remember { mutableStateOf(false) }
            val profiles = (profilesState as? ProfilesUiState.Content)?.profiles.orEmpty()
            val selectedProfile = profiles
                ?.firstOrNull { it.id == selectedProfileId }
            val alertProfile = activeAlert?.let { alert ->
                profiles.firstOrNull { it.id == alert.profileId }
            }

            if (activeAlert != null && alertProfile != null) {
                TvFullScreenAlert(
                    alert = activeAlert!!,
                    profile = alertProfile,
                    onAcknowledge = alertsViewModel::acknowledge
                )
            } else if (showingAlertTones) {
                TvAlertTonesScreen(onBack = { showingAlertTones = false })
            } else selectedProfile?.let { profile ->
                TvProfileDetailScreen(
                    profile = profile,
                    onBack = { selectedProfileId = null }
                )
            } ?: TvHomeScreen(
                    email = state.email,
                    youTubeViewModel = youTubeViewModel,
                    profilesViewModel = profilesViewModel,
                    onProfileClick = { selectedProfileId = it.id },
                    onAlertTonesClick = { showingAlertTones = true },
                    onLogout = authViewModel::logout
                )
        }
    }
}
