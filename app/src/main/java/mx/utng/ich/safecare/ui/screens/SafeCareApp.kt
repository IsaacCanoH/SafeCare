package mx.utng.ich.safecare.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import mx.utng.ich.safecare.data.local.database.SafeCareAppDatabase
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

enum class Screen {
    LOGIN, REGISTER, MAIN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeCareApp() {
    val context = LocalContext.current
    val database = remember { SafeCareAppDatabase.getDatabase(context) }
    
    val authViewModel: AuthViewModel = viewModel {
        AuthViewModel(usuarioDao = database.usuarioDao())
    }
    
    val profileViewModel: ProfileViewModel = viewModel {
        ProfileViewModel(
            perfilDao = database.perfilMonitoreadoDao(),
            smartwatchDao = database.smartwatchDao(),
            context = context.applicationContext
        )
    }
    
    val zoneViewModel: SafeZoneViewModel = viewModel {
        SafeZoneViewModel(
            zonaSeguraDao = database.zonaSeguraDao(),
            smartwatchDao = database.smartwatchDao(),
            context = context.applicationContext
        )
    }
    
    val alertViewModel: AlertViewModel = viewModel {
        AlertViewModel(
            alertaDao = database.alertaDao(),
            smartwatchDao = database.smartwatchDao(),
            context = context.applicationContext
        )
    }

    val locationViewModel: LocationViewModel = viewModel {
        LocationViewModel(ubicacionDao = database.ubicacionDao())
    }
    
    var currentRootScreen by remember { mutableStateOf(Screen.LOGIN) }
    var bottomNavTab by remember { mutableStateOf("Inicio") }
    var selectedProfileIdForMap by remember { mutableStateOf<String?>(null) }
    var selectedProfileForEdit by remember { mutableStateOf<PerfilMonitoreadoEntity?>(null) }
    var selectedZoneForEdit by remember { mutableStateOf<ZonaSeguraEntity?>(null) }
    
    val authState by authViewModel.authState
    val profiles by profileViewModel.profiles.collectAsState()
    val allZones by zoneViewModel.zones.collectAsState() // Obtenemos todas las zonas
    val alerts by alertViewModel.alerts.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            currentRootScreen = Screen.MAIN
            profileViewModel.loadProfiles()
            zoneViewModel.loadZones()
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
                                navigationIcon = {
                                    IconButton(onClick = { /* Drawer */ }) {
                                        Icon(Icons.Default.Menu, contentDescription = null)
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
                                            if (alerts.isNotEmpty()) {
                                                Badge(containerColor = Color(0xFFD32F2F)) {
                                                    Text(
                                                        text = if (alerts.size > 99) "99+" else alerts.size.toString(),
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
                    Box(modifier = Modifier.padding(padding)) {
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
                                        // Busca todas las zonas seguras globales que pertenezcan a este cuidador
                                        // (Simplificado: mostramos todas las zonas guardadas)
                                        safeZonesCount = allZones.size 
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
                }
            }
        }
    }
}
