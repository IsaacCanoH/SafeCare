package mx.utng.ich.safecare.ui.screens.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import mx.utng.ich.safecare.data.local.entity.PerfilMonitoreadoEntity
import mx.utng.ich.safecare.ui.components.OsmMapView
import mx.utng.ich.safecare.ui.components.addSafeZoneCircle
import mx.utng.ich.safecare.ui.components.addSimpleMarker
import mx.utng.ich.safecare.ui.viewmodel.ProfileViewModel
import mx.utng.ich.safecare.ui.viewmodel.SafeZoneViewModel
import org.osmdroid.util.GeoPoint

@Composable
fun LiveMapScreen(
    profileViewModel: ProfileViewModel,
    zoneViewModel: SafeZoneViewModel,
    selectedProfileId: String? = null, // ID opcional para filtrado
    onBackClick: () -> Unit = {}
) {
    val profiles by profileViewModel.profiles.collectAsState()
    val zones by zoneViewModel.zones.collectAsState()
    
    // Filtrar perfiles según si venimos de un perfil específico o de la barra global
    val displayedProfiles = if (selectedProfileId != null) {
        profiles.filter { it.idPerfil == selectedProfileId }
    } else {
        profiles
    }

    // Perfil para mostrar en la tarjeta inferior (el primero de la lista mostrada)
    val cardProfile = displayedProfiles.firstOrNull()

    Box(modifier = Modifier.fillMaxSize()) {
        // Mapa
        OsmMapView(
            modifier = Modifier.fillMaxSize(),
            center = GeoPoint(21.1526, -100.9312),
            onMapReady = { mapView ->
                mapView.overlays.clear()
                
                // 1. Mostrar marcadores de los perfiles filtrados
                displayedProfiles.forEach { profile ->
                    // En una app real, estas coordenadas vendrían de la tabla Ubicacion
                    val point = GeoPoint(21.1526, -100.9312) 
                    mapView.addSimpleMarker(point, profile.nombre)
                }
                
                // 2. Mostrar todas las zonas seguras (Opción A: Familiares)
                zones.filter { it.activa }.forEach { zone ->
                    val point = GeoPoint(zone.latitudCentro, zone.longitudCentro)
                    mapView.addSafeZoneCircle(point, zone.radioMetros, 0x445A4699.toInt())
                }
            }
        )

        // Barra Superior Local
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            color = Color.White.copy(alpha = 0.9f),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                }
                Text(
                    text = if (selectedProfileId != null) "Ubicación de ${cardProfile?.nombre ?: ""}" 
                          else "Mapa Familiar", 
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Tarjeta inferior (Solo si hay perfiles para mostrar)
        if (cardProfile != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .padding(bottom = 8.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = cardProfile.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "En línea", fontSize = 11.sp, color = Color(0xFF2E7D32))
                            }
                        }

                        // Info de batería/conexión (Simulada o del Smartwatch)
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "85%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.BatteryFull, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                            Text(text = "WiFi", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { /* Alerta manual */ },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Alerta", fontSize = 12.sp)
                        }
                        
                        Button(
                            onClick = { /* SOS */ },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Report, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SOS", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
