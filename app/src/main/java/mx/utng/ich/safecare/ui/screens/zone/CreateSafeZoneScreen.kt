package mx.utng.ich.safecare.ui.screens.zone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.ich.safecare.ui.components.OsmMapView
import mx.utng.ich.safecare.ui.components.addSafeZoneCircle
import mx.utng.ich.safecare.ui.viewmodel.SafeZoneViewModel
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSafeZoneScreen(
    viewModel: SafeZoneViewModel,
    idPerfil: String = "",
    onBackClick: () -> Unit = {},
    onSaveSuccess: () -> Unit = {}
) {
    var zoneName by remember { mutableStateOf("") }
    var radius by remember { mutableFloatStateOf(200f) }
    var isActive by remember { mutableStateOf(true) }
    var centerPoint by remember { mutableStateOf(GeoPoint(21.1526, -100.9312)) }
    var addressSearch by remember { mutableStateOf("") }

    val isLoading by viewModel.isLoading.collectAsState()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
            }
            Text("Crear zona segura", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                TextButton(onClick = {
                    viewModel.addZone(zoneName, centerPoint.latitude, centerPoint.longitude, radius.toDouble(), idPerfil) {
                        if (it) onSaveSuccess()
                    }
                }) {
                    Text("Guardar", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Search Bar (Simulada para UI, requiere Geocoder real para funcionar)
        OutlinedTextField(
            value = addressSearch,
            onValueChange = { addressSearch = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            placeholder = { Text("Buscar estado, ciudad o calle...") },
            trailingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp)
        )

        // Map Preview
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .pointerInput(Unit) {
                // Esto ayuda a que el mapa capture gestos y no el scroll del Column
            }
        ) {
            OsmMapView(
                modifier = Modifier.fillMaxSize(),
                center = centerPoint,
                onMapReady = { mapView ->
                    mapView.overlays.clear()
                    mapView.addSafeZoneCircle(centerPoint, radius.toDouble(), 0x445A4699.toInt())
                    
                    val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                            centerPoint = p
                            mapView.overlays.clear()
                            mapView.addSafeZoneCircle(p, radius.toDouble(), 0x445A4699.toInt())
                            mapView.invalidate()
                            return true
                        }
                        override fun longPressHelper(p: GeoPoint): Boolean = false
                    })
                    mapView.overlays.add(eventsOverlay)
                }
            )
            
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(8.dp),
                color = Color.Black.copy(alpha = 0.6f),
                shape = CircleShape
            ) {
                Text(
                    "Toca el mapa para fijar el centro",
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        Column(modifier = Modifier.padding(24.dp)) {
            Text(text = "Nombre de la zona", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = zoneName,
                onValueChange = { zoneName = it },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
                placeholder = { Text("Ej. Casa, Escuela") },
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Radio", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(text = "${radius.toInt()} m", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            
            Slider(
                value = radius,
                onValueChange = { 
                    radius = it 
                    // El mapa se actualizará en la siguiente recomposición
                },
                valueRange = 50f..1000f,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    viewModel.addZone(zoneName, centerPoint.latitude, centerPoint.longitude, radius.toDouble(), idPerfil) {
                        if (it) onSaveSuccess()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading && zoneName.isNotEmpty()
            ) {
                Text("Crear zona segura", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
