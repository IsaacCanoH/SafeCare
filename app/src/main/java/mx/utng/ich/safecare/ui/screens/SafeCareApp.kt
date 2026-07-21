package mx.utng.ich.safecare.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.utng.ich.safecare.data.local.database.SafeCareAppDatabase
import mx.utng.ich.safecare.designsystem.theme.AppTheme
import mx.utng.ich.safecare.ui.screens.alerts.AlertsScreen
import mx.utng.ich.safecare.ui.screens.dashboard.DashboardContent
import mx.utng.ich.safecare.ui.screens.login.LoginScreen
import mx.utng.ich.safecare.ui.screens.map.LiveMapScreen
import mx.utng.ich.safecare.ui.screens.profile.AddProfileScreen
import mx.utng.ich.safecare.ui.screens.profile.EditProfileScreen
import mx.utng.ich.safecare.ui.screens.profile.ProfilesScreen
import mx.utng.ich.safecare.ui.screens.register.RegisterScreen
import mx.utng.ich.safecare.ui.screens.zone.CreateSafeZoneScreen
import mx.utng.ich.safecare.ui.screens.zone.SafeZonesScreen
import mx.utng.ich.safecare.data.local.entity.PerfilMonitoreadoEntity

import mx.utng.ich.safecare.ui.viewmodel.AuthState
import mx.utng.ich.safecare.ui.viewmodel.AuthViewModel
import mx.utng.ich.safecare.ui.viewmodel.SafeZoneViewModel
import mx.utng.ich.safecare.ui.viewmodel.ProfileViewModel
import mx.utng.ich.safecare.ui.viewmodel.AlertViewModel
import java.text.SimpleDateFormat
import java.util.*

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
        ProfileViewModel(perfilDao = database.perfilMonitoreadoDao())
    }
    
    val zoneViewModel: SafeZoneViewModel = viewModel {
        SafeZoneViewModel(zonaSeguraDao = database.zonaSeguraDao())
    }
    
    val alertViewModel: AlertViewModel = viewModel()
    
    var currentRootScreen by remember { mutableStateOf(Screen.LOGIN) }
    var bottomNavTab by remember { mutableStateOf("Inicio") }
    var selectedProfileForEdit by remember { mutableStateOf<PerfilMonitoreadoEntity?>(null) }
    
    var showNotificationModal by remember { mutableStateOf(false) }

    val authState by authViewModel.authState
    val profiles by profileViewModel.profiles.collectAsState()

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
                                },
                                actions = {
                                    IconButton(onClick = { showNotificationModal = true }) {
                                        val alerts by alertViewModel.alerts.collectAsState()
                                        if (alerts.isNotEmpty()) {
                                            BadgedBox(badge = { Badge { Text(alerts.size.toString()) } }) {
                                                Icon(Icons.Default.Notifications, contentDescription = null)
                                            }
                                        } else {
                                            Icon(Icons.Default.Notifications, contentDescription = null)
                                        }
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
                                onClick = { bottomNavTab = "Mapa" },
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
                                icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
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
                                monitoredPersons = profiles.map { 
                                    mx.utng.ich.safecare.ui.screens.dashboard.MonitoredPerson(
                                        id = it.idPerfil,
                                        name = it.nombre,
                                        type = it.tipoPerfil,
                                        status = "En línea",
                                        battery = 100,
                                        connection = "WiFi",
                                        lastUpdate = "Ahora",
                                        isInSafeZone = true
                                    )
                                },
                                onAddPersonClick = { bottomNavTab = "AgregarPerfil" },
                                onPersonClick = { bottomNavTab = "Mapa" }
                            )
                            "Mapa" -> LiveMapScreen(
                                profileViewModel = profileViewModel,
                                zoneViewModel = zoneViewModel,
                                onBackClick = { bottomNavTab = "Inicio" }
                            )
                            "Zonas" -> SafeZonesScreen(
                                viewModel = zoneViewModel,
                                onBackClick = { bottomNavTab = "Inicio" },
                                onAddZoneClick = { bottomNavTab = "CrearZona" }
                            )
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
                                idPerfil = profiles.firstOrNull()?.idPerfil ?: "",
                                onBackClick = { bottomNavTab = "Zonas" },
                                onSaveSuccess = { 
                                    bottomNavTab = "Zonas" 
                                }
                            )
                        }
                    }
                }
                
                if (showNotificationModal) {
                    NotificationModal(
                        viewModel = alertViewModel,
                        onDismiss = { showNotificationModal = false }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationModal(viewModel: AlertViewModel, onDismiss: () -> Unit) {
    val alerts by viewModel.alerts.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Notificaciones recientes")
            }
        },
        text = {
            if (alerts.isEmpty()) {
                Text("No tienes notificaciones pendientes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    alerts.take(5).forEach { alert ->
                        val timeStr = sdf.format(Date(alert.fechaHora))
                        NotificationItem(
                            text = "${alert.tipoAlerta}: ${alert.descripcion}", 
                            time = "Hoy, $timeStr", 
                            color = if (alert.tipoAlerta == "SOS") Color(0xFFD32F2F) else Color(0xFFFF9800)
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun NotificationItem(text: String, time: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
