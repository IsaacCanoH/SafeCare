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
import mx.utng.ich.safecare.data.local.entity.ZonaSeguraEntity
import mx.utng.ich.safecare.ui.components.OsmMapView
import mx.utng.ich.safecare.ui.components.addSafeZoneCircle
import mx.utng.ich.safecare.ui.viewmodel.SafeZoneViewModel
import org.osmdroid.util.GeoPoint
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.MapView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// Permite modificar la ubicación y radio de una zona segura.
fun EditSafeZoneScreen(
    zone: ZonaSeguraEntity,
    viewModel: SafeZoneViewModel,
    onBackClick: () -> Unit = {},
    onSaveSuccess: () -> Unit = {}
) {
    var zoneName by remember { mutableStateOf(zone.nombre) }
    var radius by remember { mutableFloatStateOf(zone.radioMetros.toFloat()) }
    var centerPoint by remember { mutableStateOf(GeoPoint(zone.latitudCentro, zone.longitudCentro)) }
    var searchQuery by remember { mutableStateOf("") }
    
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }

    LaunchedEffect(zoneName, radius, centerPoint, mapViewInstance) {
        mapViewInstance?.let { mapView ->
            mapView.overlays.removeAll { it !is MapEventsOverlay }
            mapView.addSafeZoneCircle(
                centerPoint,
                radius.toDouble(),
                0x445A4699.toInt(),
                zoneName.ifBlank { "Zona segura" }
            )
            mapView.invalidate()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Editar zona segura", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
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
                                viewModel.updateZone(zone.idZona, zoneName, centerPoint.latitude, centerPoint.longitude, radius.toDouble(), zone.idPerfil) { success ->
                                    if (success) onSaveSuccess()
                                    else scope.launch { snackbarHostState.showSnackbar("Error al actualizar") }
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
                        placeholder = { Text("Buscar nueva ubicación...") },
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

            Box(modifier = Modifier.weight(1f).zIndex(1f)) {
                OsmMapView(
                    modifier = Modifier.fillMaxSize(),
                    center = centerPoint,
                    onMapReady = { mapView ->
                        mapViewInstance = mapView
                        val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                            // Usa el toque para cambiar el centro de la zona.
                            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                centerPoint = p
                                return true
                            }
                            // Ignora las pulsaciones prolongadas del mapa.
                            override fun longPressHelper(p: GeoPoint): Boolean = false
                        })
                        mapView.overlays.add(eventsOverlay)
                    }
                )

                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = CircleShape
                ) {
                    Text(
                        "Toca el mapa para cambiar el centro",
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().zIndex(2f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                elevation = CardDefaults.cardElevation(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = "Editar detalles", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    
                    OutlinedTextField(
                        value = zoneName,
                        onValueChange = { zoneName = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        label = { Text("Nombre de la zona") },
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
