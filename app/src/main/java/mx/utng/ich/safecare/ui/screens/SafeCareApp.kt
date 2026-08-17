package mx.utng.ich.safecare.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import mx.utng.ich.safecare.data.remote.SupabaseClient
import mx.utng.ich.safecare.designsystem.theme.AppTheme
import io.github.jan.supabase.auth.auth
import mx.utng.ich.safecare.ui.screens.alerts.AlertsScreen
import mx.utng.ich.safecare.ui.screens.dashboard.DashboardContent
import mx.utng.ich.safecare.ui.screens.login.LoginScreen
import mx.utng.ich.safecare.ui.screens.map.LiveMapScreen
import mx.utng.ich.safecare.ui.screens.profile.AddProfileScreen
import mx.utng.ich.safecare.ui.screens.profile.EditProfileScreen
import mx.utng.ich.safecare.ui.screens.profile.ProfilesScreen
import mx.utng.ich.safecare.ui.screens.register.RegisterScreen
import mx.utng.ich.safecare.ui.screens.zone.CreateSafeZoneScreen
import mx.utng.ich.safecare.ui.screens.zone.EditSafeZoneScreen
import mx.utng.ich.safecare.ui.screens.zone.SafeZonesScreen
import mx.utng.ich.safecare.data.local.entity.PerfilMonitoreadoEntity
import mx.utng.ich.safecare.data.local.entity.ZonaSeguraEntity

import mx.utng.ich.safecare.ui.viewmodel.AuthState
import mx.utng.ich.safecare.ui.viewmodel.AuthViewModel
import mx.utng.ich.safecare.ui.viewmodel.SafeZoneViewModel
import mx.utng.ich.safecare.ui.viewmodel.ProfileViewModel
import mx.utng.ich.safecare.ui.viewmodel.AlertViewModel
import mx.utng.ich.safecare.ui.viewmodel.LocationViewModel

/**
 * Pantallas raíz de la aplicación móvil.
 *
 * Define los estados de navegación principales: acceso, registro y pantalla principal.
 */
enum class Screen {
    LOGIN, REGISTER, MAIN
}

/**
 * Composable raíz de la aplicación móvil SafeCare del cuidador.
 *
 * Coordina la navegación entre pantallas, crea los ViewModels necesarios,
 * conserva la pestaña seleccionada del menú inferior, carga los datos al
 * autenticar, inicia las actualizaciones en tiempo real y conecta cada
 * pantalla con su respectivo ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun SafeCareApp() {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel {
        AuthViewModel()
    }
    
    val profileViewModel: ProfileViewModel = viewModel {
        ProfileViewModel(context.applicationContext)
    }
    
    val zoneViewModel: SafeZoneViewModel = viewModel {
        SafeZoneViewModel(context.applicationContext)
    }
    
    val alertViewModel: AlertViewModel = viewModel {
        AlertViewModel(context.applicationContext)
    }

    val locationViewModel: LocationViewModel = viewModel {
        LocationViewModel()
    }
    
    var currentRootScreen by remember { mutableStateOf(Screen.LOGIN) }
    var bottomNavTab by remember { mutableStateOf("Inicio") }
    var selectedProfileIdForMap by remember { mutableStateOf<String?>(null) }
    var selectedProfileForEdit by remember { mutableStateOf<PerfilMonitoreadoEntity?>(null) }
    var selectedZoneForEdit by remember { mutableStateOf<ZonaSeguraEntity?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val refreshScope = rememberCoroutineScope()
    
    val authState by authViewModel.authState
    val profiles by profileViewModel.profiles.collectAsState()
    val allZones by zoneViewModel.zones.collectAsState() // Obtenemos todas las zonas
    val alerts by alertViewModel.alerts.collectAsState()
    val activeAlertsCount = alerts.count { it.alerta.estado == "ACTIVA" }

    /** Recarga perfiles y zonas para actualizar la configuración visible. */
    suspend fun refreshConfiguration() {
        if (isRefreshing) return
        isRefreshing = true
        try {
            listOfNotNull(
                profileViewModel.loadProfiles(),
                zoneViewModel.loadZones()
            ).joinAll()
        } finally {
            isRefreshing = false
        }
    }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { refreshScope.launch { refreshConfiguration() } }
    )

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            currentRootScreen = Screen.MAIN
            refreshConfiguration()
            alertViewModel.refreshAlerts()
            locationViewModel.refreshLocations()
            alertViewModel.startRealtimeUpdates()
            locationViewModel.startRealtimeUpdates()
        }
    }

    AppTheme {
        when (currentRootScreen) {
            Screen.LOGIN -> LoginScreen(
                authState = authState,
                onLoginClick = { email, pass -> 
                    authViewModel.login(email, pass)
                },
                onRegisterClick = { currentRootScreen = Screen.REGISTER }
            )
            Screen.REGISTER -> RegisterScreen(
                authState = authState,
                onBackClick = { currentRootScreen = Screen.LOGIN },
                onRegisterSuccess = { name, email, pass ->
                    authViewModel.register(name, email, pass)
                }
            )
            Screen.MAIN -> {
                Scaffold(
                    topBar = {
                        val showMainBar = bottomNavTab in listOf("Inicio", "Alertas", "Zonas", "Perfiles")
                        if (showMainBar) {
                            CenterAlignedTopAppBar(
                                title = { 
                                    if (bottomNavTab == "Inicio") {
                                        Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                    } else {
                                        Text(bottomNavTab, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                },
                                actions = {
                                    TextButton(
                                        onClick = {
                                            authViewModel.logout()
                                            bottomNavTab = "Inicio"
                                            selectedProfileIdForMap = null
                                            currentRootScreen = Screen.LOGIN
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Logout,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Cerrar sesión")
                                    }
                                }
                            )
                        }
                    },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = bottomNavTab == "Inicio",
                                onClick = { bottomNavTab = "Inicio" },
                                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                label = { Text("Inicio") }
                            )
                            NavigationBarItem(
                                selected = bottomNavTab == "Mapa",
                                onClick = { 
                                    selectedProfileIdForMap = null
                                    bottomNavTab = "Mapa" 
                                },
                                icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                label = { Text("Mapa") }
                            )
                            NavigationBarItem(
                                selected = bottomNavTab == "Zonas",
                                onClick = { bottomNavTab = "Zonas" },
                                icon = { Icon(Icons.Default.Security, contentDescription = null) },
                                label = { Text("Zonas") }
                            )
                            NavigationBarItem(
                                selected = bottomNavTab == "Alertas",
                                onClick = { bottomNavTab = "Alertas" },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (activeAlertsCount > 0) {
                                                Badge(containerColor = Color(0xFFD32F2F)) {
                                                    Text(
                                                        text = if (activeAlertsCount > 99) "99+" else activeAlertsCount.toString(),
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Notifications,
                                            contentDescription = "Alertas"
                                        )
                                    }
                                },
                                label = { Text("Alertas") }
                            )
                            NavigationBarItem(
                                selected = bottomNavTab == "Perfiles",
                                onClick = { bottomNavTab = "Perfiles" },
                                icon = { Icon(Icons.Default.Person, contentDescription = null) },
                                label = { Text("Perfiles") }
                            )
                        }
                    }
                ) { padding ->
                    val pullToRefreshEnabled = bottomNavTab in setOf(
                        "Inicio", "Alertas", "Zonas", "Perfiles"
                    )
                    Box(
                        modifier = (Modifier
                            .padding(padding)
                            .fillMaxSize()).let { baseModifier ->
                            if (pullToRefreshEnabled) {
                                baseModifier.pullRefresh(pullRefreshState)
                            } else {
                                baseModifier
                            }
                        }
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                        when(bottomNavTab) {
                            "Inicio" -> DashboardContent(
                                monitoredPersons = profiles.map { profile -> 
                                    mx.utng.ich.safecare.ui.screens.dashboard.MonitoredPerson(
                                        id = profile.idPerfil,
                                        name = profile.nombre,
                                        type = profile.tipoPerfil,
                                        status = "En línea",
                                        battery = 100,
                                        connection = "WiFi",
                                        lastUpdate = "Ahora",
                                        isInSafeZone = true,
                                        safeZonesCount = allZones.count { profile.idPerfil in it.idPerfiles }
                                    )
                                },
                                onAddPersonClick = { bottomNavTab = "AgregarPerfil" },
                                onPersonClick = { person ->
                                    selectedProfileIdForMap = person.id
                                    bottomNavTab = "Mapa"
                                }
                            )
                            "Mapa" -> LiveMapScreen(
                                profileViewModel = profileViewModel,
                                zoneViewModel = zoneViewModel,
                                locationViewModel = locationViewModel,
                                alertViewModel = alertViewModel,
                                selectedProfileId = selectedProfileIdForMap,
                                onBackClick = { 
                                    selectedProfileIdForMap = null
                                    bottomNavTab = "Inicio" 
                                }
                            )
                            "Zonas" -> SafeZonesScreen(
                                viewModel = zoneViewModel,
                                onBackClick = { bottomNavTab = "Inicio" },
                                onAddZoneClick = { bottomNavTab = "CrearZona" },
                                onEditZoneClick = { zone ->
                                    selectedZoneForEdit = zone
                                    bottomNavTab = "EditarZona"
                                }
                            )
                            "EditarZona" -> {
                                selectedZoneForEdit?.let { zone ->
                                    EditSafeZoneScreen(
                                        zone = zone,
                                        profiles = profiles,
                                        viewModel = zoneViewModel,
                                        onBackClick = { bottomNavTab = "Zonas" },
                                        onSaveSuccess = { bottomNavTab = "Zonas" }
                                    )
                                }
                            }
                            "Alertas" -> AlertsScreen(viewModel = alertViewModel)
                            "Perfiles" -> ProfilesScreen(
                                viewModel = profileViewModel,
                                onAddProfileClick = { bottomNavTab = "AgregarPerfil" },
                                onEditProfileClick = { profile ->
                                    selectedProfileForEdit = profile
                                    bottomNavTab = "EditarPerfil"
                                }
                            )
                            "EditarPerfil" -> {
                                selectedProfileForEdit?.let { profile ->
                                    EditProfileScreen(
                                        profile = profile,
                                        viewModel = profileViewModel,
                                        onBackClick = { bottomNavTab = "Perfiles" },
                                        onSaveSuccess = { bottomNavTab = "Perfiles" }
                                    )
                                }
                            }
                            "AgregarPerfil" -> AddProfileScreen(
                                viewModel = profileViewModel,
                                onBackClick = { bottomNavTab = "Inicio" },
                                onSaveSuccess = { 
                                    bottomNavTab = "Inicio" 
                                }
                            )
                            "CrearZona" -> CreateSafeZoneScreen(
                                viewModel = zoneViewModel,
                                profiles = profiles,
                                onBackClick = { bottomNavTab = "Zonas" },
                                onSaveSuccess = { 
                                    bottomNavTab = "Zonas" 
                                }
                            )
                        }
                        }
                        if (pullToRefreshEnabled) {
                            PullRefreshIndicator(
                                refreshing = isRefreshing,
                                state = pullRefreshState,
                                modifier = Modifier.align(Alignment.TopCenter)
                            )
                        }
                    }
                }
            }
        }
    }
}
