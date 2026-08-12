package mx.utng.ich.safecaretv.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import mx.utng.ich.safecaretv.data.youtube.YouTubeVideo
import mx.utng.ich.safecaretv.data.profile.MonitoredProfile
import mx.utng.ich.safecaretv.data.profile.MonitoringStatus
import mx.utng.ich.safecaretv.ui.theme.SafeBackground
import mx.utng.ich.safecaretv.ui.theme.SafeNavy
import mx.utng.ich.safecaretv.ui.theme.SafePurple
import mx.utng.ich.safecaretv.ui.theme.SafePurpleLight
import mx.utng.ich.safecaretv.ui.theme.SafeTextMuted
import mx.utng.ich.safecaretv.ui.viewmodel.YouTubeUiState
import mx.utng.ich.safecaretv.ui.viewmodel.YouTubeViewModel
import mx.utng.ich.safecaretv.ui.viewmodel.MonitoredProfilesViewModel
import mx.utng.ich.safecaretv.ui.viewmodel.ProfilesUiState

@Composable
// Muestra el panel principal de perfiles y recomendaciones en TV.
fun TvHomeScreen(
    email: String,
    youTubeViewModel: YouTubeViewModel,
    profilesViewModel: MonitoredProfilesViewModel,
    onProfileClick: (MonitoredProfile) -> Unit,
    onAlertTonesClick: () -> Unit,
    onLogout: () -> Unit
) {
    val youTubeState by youTubeViewModel.state.collectAsStateWithLifecycle()
    val profilesState by profilesViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var now by remember { mutableStateOf(Date()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(30_000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SafeBackground)
    ) {
        DashboardHeader(
            now = now,
            onAlertTonesClick = onAlertTonesClick
        )

        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(0.66f)
                    .fillMaxHeight()
                    .padding(start = 32.dp, top = 20.dp, end = 22.dp, bottom = 24.dp)
            ) {
                Text(
                    text = "Personas monitoreadas",
                    color = SafeNavy,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                ProfilesContent(
                    state = profilesState,
                    onRetry = profilesViewModel::loadProfiles,
                    onProfileClick = onProfileClick,
                    modifier = Modifier.weight(1f)
                )
                ProfilesLegend()
            }

            RecommendationsPanel(
                state = youTubeState,
                onRetry = youTubeViewModel::loadRecommendations,
                onVideoClick = { video ->
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(video.watchUrl))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                onMoreClick = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                "https://www.youtube.com/results?search_query=" +
                                    "cuidados+adultos+mayores+cuidado+infantil"
                            )
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                modifier = Modifier
                    .weight(0.34f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
// Muestra la cuadrícula de perfiles monitoreados disponibles.
private fun ProfilesContent(
    state: ProfilesUiState,
    onRetry: () -> Unit,
    onProfileClick: (MonitoredProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        when (state) {
            ProfilesUiState.Loading -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = SafePurple)
                Spacer(Modifier.height(10.dp))
                Text("Cargando personas monitoreadas…", color = SafeTextMuted)
            }
            is ProfilesUiState.Error -> ErrorRecommendations(
                message = state.message,
                onRetry = onRetry
            )
            is ProfilesUiState.Content -> {
                if (state.profiles.isEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = SafePurple.copy(alpha = 0.55f),
                            modifier = Modifier.size(58.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Aún no hay personas monitoreadas",
                            color = SafeNavy,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Agrégalas desde la aplicación móvil",
                            color = SafeTextMuted,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        gridItems(state.profiles, key = { it.id }) { profile ->
                            MonitoredProfileCard(profile, onClick = { onProfileClick(profile) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
// Muestra el resumen seleccionable de un perfil monitoreado.
private fun MonitoredProfileCard(profile: MonitoredProfile, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val statusColor = profile.status.statusColor()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(202.dp)
            .scale(if (focused) 1.035f else 1f)
            .onFocusChanged { focused = it.isFocused }
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) SafePurple else Color(0xFFE1DEE8),
                RoundedCornerShape(15.dp)
            )
            .clickable(onClick = onClick),
        color = Color.White,
        shape = RoundedCornerShape(15.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(SafePurpleLight),
                    contentAlignment = Alignment.Center
                ) {
                    if (!profile.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = profile.photoUrl,
                            contentDescription = "Foto de ${profile.name}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = profile.name.trim().take(1).uppercase(),
                            color = SafePurple,
                            fontSize = 31.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Icon(
                    imageVector = profile.status.statusIcon(),
                    contentDescription = profile.status.statusLabel(),
                    tint = statusColor,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(25.dp)
                        .background(Color.White, androidx.compose.foundation.shape.CircleShape)
                        .padding(2.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                profile.name,
                color = SafeNavy,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(7.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusColor.copy(alpha = 0.14f))
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    profile.status.statusIcon(),
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    profile.status.statusLabel(),
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileMetric(
                    icon = Icons.Default.BatteryFull,
                    label = profile.batteryLevel?.let { "$it%" } ?: "--"
                )
                ProfileMetric(
                    icon = if (profile.isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                    label = if (profile.isOnline) "En línea" else "Sin conexión"
                )
            }
        }
    }
}

@Composable
// Muestra una métrica breve dentro de la tarjeta del perfil.
private fun ProfileMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = SafeNavy, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = SafeNavy, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
// Muestra la leyenda de colores para los estados de monitoreo.
private fun ProfilesLegend() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LegendItem("En zona segura", Color(0xFF24943A))
        LegendItem("Fuera de zona", Color(0xFFF2A900))
        LegendItem("SOS activo", Color(0xFFE31C24))
        LegendItem("Sin conexión", Color(0xFF9A94B7))
    }
}

@Composable
// Muestra un elemento de la leyenda de estados.
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .background(color, androidx.compose.foundation.shape.CircleShape)
        )
        Spacer(Modifier.width(5.dp))
        Text(label, color = SafeTextMuted, fontSize = 10.sp)
    }
}

// Traduce el estado de monitoreo a una etiqueta corta.
private fun MonitoringStatus.statusLabel(): String = when (this) {
    MonitoringStatus.SAFE -> "En zona segura"
    MonitoringStatus.OUTSIDE_SAFE_ZONE -> "Fuera de zona"
    MonitoringStatus.SOS -> "SOS activo"
    MonitoringStatus.OFFLINE -> "Sin conexión"
}

// Define el color asociado a cada estado de monitoreo.
private fun MonitoringStatus.statusColor(): Color = when (this) {
    MonitoringStatus.SAFE -> Color(0xFF24943A)
    MonitoringStatus.OUTSIDE_SAFE_ZONE -> Color(0xFFF2A900)
    MonitoringStatus.SOS -> Color(0xFFE31C24)
    MonitoringStatus.OFFLINE -> Color(0xFF77718F)
}

// Selecciona el icono asociado a cada estado de monitoreo.
private fun MonitoringStatus.statusIcon() = when (this) {
    MonitoringStatus.SAFE -> Icons.Default.CheckCircle
    MonitoringStatus.OUTSIDE_SAFE_ZONE -> Icons.Default.Error
    MonitoringStatus.SOS -> Icons.Default.Sos
    MonitoringStatus.OFFLINE -> Icons.Default.WifiOff
}

@Composable
// Muestra el encabezado con las acciones principales del panel.
private fun DashboardHeader(
    now: Date,
    onAlertTonesClick: () -> Unit
) {
    val mexicanSpanish = remember { Locale.forLanguageTag("es-MX") }
    val timeFormatter = remember { SimpleDateFormat("h:mm a", mexicanSpanish) }
    val dateFormatter = remember { SimpleDateFormat("d 'de' MMMM, yyyy", mexicanSpanish) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(94.dp),
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 34.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = SafePurple,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = "Familia Segura",
                    color = SafeNavy,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Monitoreo en tiempo real",
                    color = SafeTextMuted,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.weight(1f))
            HeaderAction(
                text = "Tonos de alerta",
                icon = Icons.Default.MusicNote,
                onClick = onAlertTonesClick
            )
            Spacer(Modifier.width(18.dp))
            HeaderIconAction(
                icon = Icons.Default.Settings,
                contentDescription = "Configuración",
                onClick = {}
            )
            Spacer(Modifier.width(32.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = timeFormatter.format(now).lowercase(mexicanSpanish),
                    color = SafeNavy,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateFormatter.format(now),
                    color = SafeNavy,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.width(18.dp))
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = "Conexión de red",
                tint = SafeNavy,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
// Muestra una acción textual en el encabezado de TV.
private fun HeaderAction(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .scale(if (focused) 1.05f else 1f)
            .onFocusChanged { focused = it.isFocused }
            .border(
                width = 2.dp,
                color = if (focused) SafePurple else SafePurple.copy(alpha = 0.7f),
                shape = RoundedCornerShape(11.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(11.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = SafePurple, modifier = Modifier.size(23.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, color = SafePurple, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
// Muestra una acción con icono en el encabezado de TV.
private fun HeaderIconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(52.dp)
            .scale(if (focused) 1.08f else 1f)
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(11.dp))
            .background(if (focused) SafePurpleLight else Color.White)
            .border(1.dp, Color(0xFFD9D6E2), RoundedCornerShape(11.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = SafeNavy)
    }
}

@Composable
// Muestra recomendaciones de video y su estado de carga.
private fun RecommendationsPanel(
    state: YouTubeUiState,
    onRetry: () -> Unit,
    onVideoClick: (YouTubeVideo) -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(top = 2.dp, end = 18.dp, bottom = 18.dp),
        color = Color(0xFFF6F4FA),
        shape = RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE3E0EA))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Recomendaciones para ti",
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                color = SafeNavy,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (state) {
                    YouTubeUiState.Loading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = SafePurple)
                            Spacer(Modifier.height(12.dp))
                            Text("Buscando recomendaciones reales…", color = SafeTextMuted)
                        }
                    }
                    is YouTubeUiState.Error -> ErrorRecommendations(
                        message = state.message,
                        onRetry = onRetry
                    )
                    is YouTubeUiState.Content -> LazyColumn(
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.videos, key = { it.id }) { video ->
                            VideoRecommendationItem(
                                video = video,
                                onClick = { onVideoClick(video) }
                            )
                        }
                    }
                }
            }

            MoreYouTubeButton(onClick = onMoreClick)
        }
    }
}

@Composable
// Muestra un video recomendado y permite abrirlo.
private fun VideoRecommendationItem(
    video: YouTubeVideo,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (focused) 1.025f else 1f)
            .onFocusChanged { focused = it.isFocused }
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) SafePurple else Color(0xFFE4E1E9),
                shape = RoundedCornerShape(13.dp)
            )
            .clickable(onClick = onClick),
        color = Color.White,
        shape = RoundedCornerShape(13.dp)
    ) {
        Row(
            modifier = Modifier.padding(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(142.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE8E6EC))
            ) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = "Miniatura de ${video.title}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (video.duration.isNotBlank()) {
                    Text(
                        text = video.duration,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(5.dp)
                            .background(
                                Color.Black.copy(alpha = 0.82f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    color = SafeNavy,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = video.channelTitle,
                    color = SafeTextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
// Muestra un mensaje cuando no se pueden cargar recomendaciones.
private fun ErrorRecommendations(
    message: String,
    onRetry: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = SafePurple)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Reintentar")
            }
        }
    }
}

@Composable
// Muestra el acceso para ver más contenido en YouTube.
private fun MoreYouTubeButton(onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (focused) 1.02f else 1f)
            .onFocusChanged { focused = it.isFocused }
            .background(if (focused) Color(0xFFFFEBEE) else Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(34.dp)
                .height(23.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFFF0000)),
            contentAlignment = Alignment.Center
        ) {
            Text("▶", color = Color.White, fontSize = 12.sp)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "Más videos en YouTube",
            color = SafeNavy,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
