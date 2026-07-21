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
    onBackClick: () -> Unit = {}
) {
    val profiles by profileViewModel.profiles.collectAsState()
    val zones by zoneViewModel.zones.collectAsState()
    
    // Por simplicidad, seleccionamos el primer perfil si hay alguno
    val selectedProfile = profiles.firstOrNull()

    Box(modifier = Modifier.fillMaxSize()) {
        // Map
        OsmMapView(
            modifier = Modifier.fillMaxSize(),
            center = GeoPoint(21.1526, -100.9312),
            onMapReady = { mapView ->
                // Añadir marcadores para todos los perfiles (si tienen ubicación, aquí simulamos)
                profiles.forEach { profile ->
                    val point = GeoPoint(21.1526, -100.9312) // En una app real vendría de UbicacionEntity
                    mapView.addSimpleMarker(point, profile.nombre)
                }
                
                // Añadir círculos para las zonas seguras
                zones.forEach { zone ->
                    val point = GeoPoint(zone.latitudCentro, zone.longitudCentro)
                    mapView.addSafeZoneCircle(point, zone.radioMetros, 0x445A4699.toInt())
                }
            }
        )

        // Local Top Bar
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
                    "Mapa en tiempo real", 
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Overlay Details (Bottom Card)
        if (selectedProfile != null) {
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
                            Text(text = selectedProfile.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "En línea", fontSize = 11.sp, color = Color(0xFF2E7D32))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { /* Alert */ },
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
