package mx.utng.ich.safecare.ui.screens.zone

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import mx.utng.ich.safecare.ui.components.OsmMapView
import mx.utng.ich.safecare.ui.components.addSafeZoneCircle
import mx.utng.ich.safecare.ui.viewmodel.SafeZoneViewModel
import mx.utng.ich.safecare.data.local.entity.PerfilMonitoreadoEntity
import org.osmdroid.util.GeoPoint
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.MapView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSafeZoneScreen(
    viewModel: SafeZoneViewModel,
    profiles: List<PerfilMonitoreadoEntity> = emptyList(),
    onBackClick: () -> Unit = {},
    onSaveSuccess: () -> Unit = {}
) {
    var zoneName by remember { mutableStateOf("") }
    var radius by remember { mutableFloatStateOf(200f) }
    var centerPoint by remember { mutableStateOf(GeoPoint(21.1526, -100.9312)) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedProfile by remember(profiles) { mutableStateOf(profiles.firstOrNull()) }
    var profileMenuExpanded by remember { mutableStateOf(false) }
    
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Referencia persistente al overlay del círculo para poder actualizarlo
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }

    // Efecto para actualizar el círculo cuando cambie el radio o el punto central
    LaunchedEffect(radius, centerPoint, mapViewInstance) {
        mapViewInstance?.let { mapView ->
            mapView.overlays.removeAll { it !is MapEventsOverlay }
            mapView.addSafeZoneCircle(centerPoint, radius.toDouble(), 0x445A4699.toInt())
            mapView.invalidate()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Crear zona segura", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        TextButton(onClick = {
                            if (zoneName.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar("Ingresa un nombre para la zona") }
                            } else {
                                val profileId = selectedProfile?.idPerfil
                                if (profileId == null) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Primero registra un perfil monitoreado"
                                        )
                                    }
                                    return@TextButton
                                }
                                viewModel.addZone(zoneName, centerPoint.latitude, centerPoint.longitude, radius.toDouble(), profileId) { success ->
                                    if (success) onSaveSuccess()
                                    else scope.launch { snackbarHostState.showSnackbar("Error al guardar") }
                                }
                            }
                        }) {
                            Text("Guardar", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Buscador y Resultados (Z-Index alto para evitar que el mapa lo tape)
            Box(modifier = Modifier.fillMaxWidth().zIndex(2f)) {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { 
                            searchQuery = it
                            if (it.length >= 3) viewModel.searchLocation(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Buscar estado, ciudad o calle...") },
                        trailingIcon = { 
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = ""; viewModel.clearSearch() }) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                }
                            } else {
                                Icon(Icons.Default.Search, contentDescription = null)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    if (searchResults.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .heightIn(max = 250.dp),
                            elevation = CardDefaults.cardElevation(8.dp)
                        ) {
                            LazyColumn {
                                items(searchResults) { (name, point) ->
                                    ListItem(
                                        headlineContent = { Text(name, fontSize = 12.sp) },
                                        modifier = Modifier.clickable {
                                            centerPoint = point
                                            searchQuery = ""
                                            viewModel.clearSearch()
                                        },
                                        leadingContent = { Icon(Icons.Default.LocationOn, contentDescription = null) }
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }

            // Mapa
            Box(modifier = Modifier.weight(1f).zIndex(1f)) {
                OsmMapView(
                    modifier = Modifier.fillMaxSize(),
                    center = centerPoint,
                    onMapReady = { mapView ->
                        mapViewInstance = mapView
                        
                        // Añadir gestor de eventos de toque
                        val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                centerPoint = p // Esto disparará el LaunchedEffect
                                return true
                            }
                            override fun longPressHelper(p: GeoPoint): Boolean = false
                        })
                        mapView.overlays.add(eventsOverlay)
                    }
                )

                // Instrucción flotante
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = CircleShape
                ) {
                    Text(
                        "Toca el mapa para cambiar la ubicación",
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // Panel de Control
            Card(
                modifier = Modifier.fillMaxWidth().zIndex(2f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                elevation = CardDefaults.cardElevation(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = "Ajustes de zona", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                    ExposedDropdownMenuBox(
                        expanded = profileMenuExpanded,
                        onExpandedChange = {
                            if (profiles.isNotEmpty()) {
                                profileMenuExpanded = !profileMenuExpanded
                            }
                        }
                    ) {
                        OutlinedTextField(
                            value = selectedProfile?.nombre ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Perfil monitoreado") },
                            placeholder = { Text("No hay perfiles registrados") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = profileMenuExpanded
                                )
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = profileMenuExpanded,
                            onDismissRequest = { profileMenuExpanded = false }
                        ) {
                            profiles.forEach { profile ->
                                DropdownMenuItem(
                                    text = { Text(profile.nombre) },
                                    onClick = {
                                        selectedProfile = profile
                                        profileMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    OutlinedTextField(
                        value = zoneName,
                        onValueChange = { zoneName = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        label = { Text("Nombre de la zona") },
                        placeholder = { Text("Ej. Casa, Escuela") },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Radio de protección", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "${radius.toInt()} m", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    
                    Slider(
                        value = radius,
                        onValueChange = { radius = it },
                        valueRange = 50f..1000f
                    )
                }
            }
        }
    }
}
