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
import mx.utng.ich.safecare.ui.viewmodel.LocationViewModel
import mx.utng.ich.safecare.ui.viewmodel.AlertViewModel
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

@Composable
// Muestra el mapa con la última ubicación y zonas de los perfiles.
fun LiveMapScreen(
    profileViewModel: ProfileViewModel,
    zoneViewModel: SafeZoneViewModel,
    locationViewModel: LocationViewModel,
    alertViewModel: AlertViewModel,
    selectedProfileId: String? = null, // ID opcional para filtrado
    onBackClick: () -> Unit = {}
) {
    val profiles by profileViewModel.profiles.collectAsState()
    val zones by zoneViewModel.zones.collectAsState()
    val latestLocations by locationViewModel.latestLocationsByProfile.collectAsState()
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var showCustomAlertDialog by remember { mutableStateOf(false) }
    var customAlertMessage by remember { mutableStateOf("") }
    var isSendingAlert by remember { mutableStateOf(false) }
    var alertFeedback by remember { mutableStateOf<String?>(null) }
    
    // Filtrar perfiles según si venimos de un perfil específico o de la barra global
    val displayedProfiles = if (selectedProfileId != null) {
        profiles.filter { it.idPerfil == selectedProfileId }
    } else {
        profiles
    }

    // Perfil para mostrar en la tarjeta inferior (el primero de la lista mostrada)
    val cardProfile = displayedProfiles.firstOrNull()
    val cardLocation = cardProfile?.let { latestLocations[it.idPerfil] }
    val mapCenter = cardLocation?.let { GeoPoint(it.latitud, it.longitud) }
        ?: GeoPoint(21.1526, -100.9312)

    LaunchedEffect(mapView, displayedProfiles, zones, latestLocations) {
        mapView?.let { currentMap ->
            currentMap.overlays.clear()

            displayedProfiles.forEach { profile ->
                latestLocations[profile.idPerfil]?.let { location ->
                    currentMap.addSimpleMarker(
                        GeoPoint(location.latitud, location.longitud),
                        profile.nombre
                    )
                }
            }

            zones
                .filter { zone ->
                    zone.activa && displayedProfiles.any { it.idPerfil == zone.idPerfil }
                }
                .forEach { zone ->
                    currentMap.addSafeZoneCircle(
                        GeoPoint(zone.latitudCentro, zone.longitudCentro),
                        zone.radioMetros,
                        0x445A4699.toInt()
                    )
                }
            currentMap.invalidate()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Mapa
        OsmMapView(
            modifier = Modifier.fillMaxSize(),
            center = mapCenter,
            onMapReady = { readyMap -> mapView = readyMap }
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
                            Text(
                                text = cardLocation?.let {
                                    "Actualizado ${formatElapsedTime(it.fechaHora)}"
                                } ?: "Sin ubicación recibida",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showCustomAlertDialog = true },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Alerta", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (showCustomAlertDialog && cardProfile != null) {
            AlertDialog(
                onDismissRequest = {
                    if (!isSendingAlert) showCustomAlertDialog = false
                },
                title = { Text("Enviar alerta a ${cardProfile.nombre}") },
                text = {
                    Column {
                        Text("Escribe el mensaje que aparecerá en el reloj.")
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = customAlertMessage,
                            onValueChange = {
                                if (it.length <= MAX_CUSTOM_ALERT_LENGTH) {
                                    customAlertMessage = it
                                    alertFeedback = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Mensaje") },
                            supportingText = {
                                Text("${customAlertMessage.length}/$MAX_CUSTOM_ALERT_LENGTH")
                            },
                            minLines = 3,
                            maxLines = 5,
                            enabled = !isSendingAlert
                        )
                        alertFeedback?.let { feedback ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                feedback,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showCustomAlertDialog = false },
                        enabled = !isSendingAlert
                    ) { Text("Cancelar") }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isSendingAlert = true
                            alertViewModel.sendCustomAlert(
                                profileId = cardProfile.idPerfil,
                                message = customAlertMessage
                            ) { result ->
                                isSendingAlert = false
                                result.onSuccess {
                                    customAlertMessage = ""
                                    alertFeedback = null
                                    showCustomAlertDialog = false
                                }.onFailure { error ->
                                    alertFeedback = error.message ?: "No se pudo enviar la alerta"
                                }
                            }
                        },
                        enabled = customAlertMessage.isNotBlank() && !isSendingAlert
                    ) {
                        if (isSendingAlert) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Enviar")
                        }
                    }
                }
            )
        }
    }
}

private const val MAX_CUSTOM_ALERT_LENGTH = 160

// Convierte una marca de tiempo en un texto de tiempo transcurrido.
private fun formatElapsedTime(timestamp: Long): String {
    val elapsedSeconds =
        ((System.currentTimeMillis() - timestamp) / 1_000).coerceAtLeast(0)
    return when {
        elapsedSeconds < 10 -> "ahora"
        elapsedSeconds < 60 -> "hace ${elapsedSeconds}s"
        elapsedSeconds < 3_600 -> "hace ${elapsedSeconds / 60} min"
        else -> "hace ${elapsedSeconds / 3_600} h"
    }
}
