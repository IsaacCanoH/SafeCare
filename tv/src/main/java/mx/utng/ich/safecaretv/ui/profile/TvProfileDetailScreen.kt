package mx.utng.ich.safecaretv.ui.profile

import android.graphics.Color as AndroidColor
import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import mx.utng.ich.safecaretv.data.profile.MonitoredProfile
import mx.utng.ich.safecaretv.data.profile.MonitoringStatus
import mx.utng.ich.safecaretv.ui.theme.SafeBackground
import mx.utng.ich.safecaretv.ui.theme.SafeNavy
import mx.utng.ich.safecaretv.ui.theme.SafePurple
import mx.utng.ich.safecaretv.ui.theme.SafePurpleLight
import mx.utng.ich.safecaretv.ui.theme.SafeTextMuted
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

@Composable
/** Muestra el detalle de un perfil monitoreado en TV. */
fun TvProfileDetailScreen(profile: MonitoredProfile, onBack: () -> Unit) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    val address = rememberAddress(profile.latitude, profile.longitude)

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }

    Column(Modifier.fillMaxSize().background(SafeBackground)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(70.dp).padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp).clickable(onClick = onBack),
                color = Color.Transparent,
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ArrowBack, "Regresar", tint = SafeNavy)
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "Perfil de ${profile.name}",
                color = SafeNavy,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Row(
            modifier = Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ProfileMap(
                profile = profile,
                updatedText = elapsedText(profile.locationTimestamp, now),
                modifier = Modifier.weight(0.42f).fillMaxHeight()
            )
            Column(
                modifier = Modifier.weight(0.58f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ProfileSummary(profile, now)
                PersonalInformation(profile, address, Modifier.weight(1f))
            }
        }
    }
}

@Composable
/** Presenta el mapa o una alternativa cuando no hay ubicación. */
private fun ProfileMap(
    profile: MonitoredProfile,
    updatedText: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFEDEBF2),
        tonalElevation = 1.dp
    ) {
        Box {
            if (profile.latitude != null && profile.longitude != null) {
                OsmProfileMap(profile, Modifier.fillMaxSize())
            } else {
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        null,
                        tint = SafePurple.copy(alpha = .55f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text("Sin ubicación recibida", color = SafeTextMuted)
                }
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp),
                color = Color.White.copy(alpha = .95f),
                shape = RoundedCornerShape(11.dp),
                shadowElevation = 5.dp
            ) {
                Text(
                    updatedText,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
                    color = SafeNavy,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
/** Renderiza la última ubicación y zonas del perfil en OpenStreetMap. */
private fun OsmProfileMap(profile: MonitoredProfile, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val center = remember(profile.latitude, profile.longitude) {
        GeoPoint(profile.latitude!!, profile.longitude!!)
    }
    val mapView = remember { MapView(context) }

    DisposableEffect(mapView) {
        Configuration.getInstance().userAgentValue = context.packageName
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            mapView.apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                minZoomLevel = 4.0
                maxZoomLevel = 20.0
            }
        },
        update = { map ->
            map.overlays.clear()
            profile.safeZones.forEach { zone ->
                Polygon(map).apply {
                    points = Polygon.pointsAsCircle(
                        GeoPoint(zone.latitude, zone.longitude),
                        zone.radiusMeters
                    )
                    fillPaint.color = AndroidColor.argb(48, 90, 70, 153)
                    outlinePaint.color = AndroidColor.rgb(116, 88, 211)
                    outlinePaint.strokeWidth = 3f
                    map.overlays.add(this)
                }
            }
            map.overlays.add(
                Marker(map).apply {
                    position = center
                    title = profile.name
                    snippet = profile.currentSafeZoneName ?: "Ubicación actual"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
            )
            map.controller.setZoom(16.0)
            map.controller.setCenter(center)
            map.invalidate()
        }
    )
}

@Composable
/** Muestra métricas resumidas del estado del perfil. */
private fun ProfileSummary(profile: MonitoredProfile, now: Long) {
    val statusColor = profile.status.detailColor()
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE4E1EA))
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfilePhoto(profile, 78)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(profile.name, color = SafeNavy, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                    Text(profileTypeLabel(profile.profileType), color = SafeNavy, fontSize = 14.sp)
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor.copy(alpha = .14f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        profile.status.detailIcon(),
                        null,
                        tint = statusColor,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(profile.status.detailLabel(), color = statusColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            Surface(
                color = Color(0xFFFBFAFD),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9E6EF))
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 15.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryMetric(Icons.Default.BatteryFull, "Batería", profile.batteryLevel?.let { "$it%" } ?: "Sin dato")
                    VerticalDivider()
                    SummaryMetric(
                        if (profile.isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                        "Conexión",
                        if (profile.isOnline) "En línea" else "Sin conexión"
                    )
                    VerticalDivider()
                    SummaryMetric(
                        Icons.Default.Schedule,
                        "Última actualización",
                        elapsedText(profile.locationTimestamp ?: profile.lastConnection, now)
                    )
                }
            }
        }
    }
}

@Composable
/** Muestra una métrica individual dentro del resumen del perfil. */
private fun SummaryMetric(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = SafeTextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = SafeNavy, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(6.dp))
            Text(value, color = SafeNavy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
/** Dibuja el separador vertical entre métricas del resumen. */
private fun VerticalDivider() {
    Box(Modifier.width(1.dp).height(50.dp).background(Color(0xFFE7E3EC)))
}

@Composable
/** Muestra los datos personales y ubicación del perfil. */
private fun PersonalInformation(profile: MonitoredProfile, address: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE4E1EA))
    ) {
        Column(
            Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Text("Información personal", color = SafeNavy, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            InformationRow("Fecha de nacimiento", formatBirthDate(profile.birthDate))
            InformationRow("Dirección", address)
            InformationRow(
                "Smartwatch",
                if (profile.watchName != null || profile.batteryLevel != null) {
                    "Conectado"
                } else {
                    "Sin vincular"
                }
            )
            InformationRow(
                "Zona segura actual",
                when {
                    profile.currentSafeZoneName != null -> profile.currentSafeZoneName
                    profile.status == MonitoringStatus.OUTSIDE_SAFE_ZONE ->
                        "Fuera de zona segura"
                    profile.safeZones.isNotEmpty() -> profile.safeZones.first().name
                    else -> "Sin zona asignada"
                }
            )
        }
    }
}

@Composable
/** Muestra una fila de información con etiqueta y valor. */
private fun InformationRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, color = SafeNavy, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(.36f))
        Text(
            value,
            color = SafeNavy,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(.64f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
/** Muestra la fotografía o inicial del perfil monitoreado. */
private fun ProfilePhoto(profile: MonitoredProfile, size: Int) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(SafePurpleLight),
        contentAlignment = Alignment.Center
    ) {
        if (!profile.photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = profile.photoUrl,
                contentDescription = "Foto de ${profile.name}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                profile.name.trim().take(1).uppercase(),
                color = SafePurple,
                fontSize = 31.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
/** Conserva una dirección legible para las coordenadas del perfil. */
private fun rememberAddress(latitude: Double?, longitude: Double?): String {
    val context = LocalContext.current
    val fallback = if (latitude != null && longitude != null) {
        String.format(Locale.US, "%.6f, %.6f", latitude, longitude)
    } else {
        "Sin ubicación recibida"
    }
    var address by remember(latitude, longitude) { mutableStateOf(fallback) }
    LaunchedEffect(latitude, longitude) {
        if (latitude == null || longitude == null) return@LaunchedEffect
        address = withContext(Dispatchers.IO) {
            runCatching {
                @Suppress("DEPRECATION")
                Geocoder(context, Locale.getDefault())
                    .getFromLocation(latitude, longitude, 1)
                    ?.firstOrNull()
                    ?.getAddressLine(0)
            }.getOrNull().orEmpty().ifBlank { fallback }
        }
    }
    return address
}

/** Convierte una marca de tiempo opcional en tiempo transcurrido. */
private fun elapsedText(timestamp: Long?, now: Long): String {
    if (timestamp == null) return "Sin actualización"
    val seconds = ((now - timestamp) / 1_000).coerceAtLeast(0)
    return when {
        seconds < 10 -> "Actualizado ahora"
        seconds < 60 -> "Hace $seconds seg"
        seconds < 3_600 -> "Hace ${seconds / 60} min"
        seconds < 86_400 -> "Hace ${seconds / 3_600} h"
        else -> "Hace ${seconds / 86_400} d"
    }
}

/** Formatea la fecha de nacimiento para mostrarla al usuario. */
private fun formatBirthDate(value: String?): String {
    if (value.isNullOrBlank()) return "Sin registrar"
    return runCatching {
        val source = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val target = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        target.format(source.parse(value) ?: Date())
    }.getOrDefault(value)
}

/** Traduce el tipo técnico de perfil a una etiqueta visible. */
private fun profileTypeLabel(value: String): String = when (value.lowercase()) {
    "menor" -> "Menor de edad"
    "adulto_mayor" -> "Adulto mayor"
    "cuidador" -> "Cuidador"
    else -> value.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

/** Traduce el estado de monitoreo para la vista de detalle. */
private fun MonitoringStatus.detailLabel(): String = when (this) {
    MonitoringStatus.SAFE -> "En zona segura"
    MonitoringStatus.OUTSIDE_SAFE_ZONE -> "Fuera de zona"
    MonitoringStatus.SOS -> "SOS activo"
    MonitoringStatus.OFFLINE -> "Sin conexión"
}

/** Define el color visual del estado de monitoreo. */
private fun MonitoringStatus.detailColor(): Color = when (this) {
    MonitoringStatus.SAFE -> Color(0xFF24943A)
    MonitoringStatus.OUTSIDE_SAFE_ZONE -> Color(0xFFE18700)
    MonitoringStatus.SOS -> Color(0xFFD9232E)
    MonitoringStatus.OFFLINE -> Color(0xFF77718F)
}

/** Selecciona el icono que representa el estado de monitoreo. */
private fun MonitoringStatus.detailIcon() = when (this) {
    MonitoringStatus.SAFE -> Icons.Default.CheckCircle
    MonitoringStatus.OUTSIDE_SAFE_ZONE -> Icons.Default.Error
    MonitoringStatus.SOS -> Icons.Default.Sos
    MonitoringStatus.OFFLINE -> Icons.Default.WifiOff
}
