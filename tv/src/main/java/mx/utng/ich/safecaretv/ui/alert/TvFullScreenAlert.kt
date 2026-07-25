package mx.utng.ich.safecaretv.ui.alert

import android.location.Geocoder
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import mx.utng.ich.safecaretv.data.alert.TvAlert
import mx.utng.ich.safecaretv.data.profile.MonitoredProfile
import mx.utng.ich.safecaretv.data.sound.AlertTonePlayer
import mx.utng.ich.safecaretv.data.sound.AlertTonePreferences
import java.util.Locale

@Composable
fun TvFullScreenAlert(
    alert: TvAlert,
    profile: MonitoredProfile,
    onAcknowledge: () -> Unit
) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val address = alertAddress(profile.latitude, profile.longitude)
    val context = LocalContext.current

    DisposableEffect(alert.id) {
        val player = AlertTonePlayer(context.applicationContext)
        player.playAlert(AlertTonePreferences.selected(context))
        onDispose(player::stop)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFCE1616)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = center
            listOf(150f, 270f, 400f, 540f, 690f).forEach { radius ->
                drawCircle(
                    color = Color.White.copy(alpha = .09f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2.5f)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 24.dp)
        ) {
            if (!alert.isSos) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(
                if (alert.isSos) "¡SOS!" else "¡Alerta!",
                color = Color.White,
                fontSize = 46.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                if (alert.isSos) {
                    "${profile.name} activó una alerta SOS"
                } else {
                    "${profile.name} salió de la zona segura"
                },
                color = Color.White,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))
            Surface(
                color = Color(0xFF981212).copy(alpha = .78f),
                shape = RoundedCornerShape(15.dp)
            ) {
                Row(
                    modifier = Modifier.width(420.dp).padding(horizontal = 28.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(Modifier.width(20.dp))
                    Column {
                        Text(
                            "Ubicación actual",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            address,
                            color = Color.White,
                            fontSize = 16.sp,
                            maxLines = 2
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                elapsedAlertTime(alert.timestamp, now),
                color = Color.White.copy(alpha = .9f),
                fontSize = 14.sp
            )
            Spacer(Modifier.height(28.dp))
            Surface(
                onClick = onAcknowledge,
                modifier = Modifier
                    .width(300.dp)
                    .height(72.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused }
                    .focusable()
                    .border(
                        if (focused) 4.dp else 1.dp,
                        if (focused) Color.White else Color(0xFFE4DCDC),
                        RoundedCornerShape(16.dp)
                    ),
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 9.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "Entendido",
                        color = Color(0xFFC51616),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun alertAddress(latitude: Double?, longitude: Double?): String {
    val context = LocalContext.current
    val fallback = if (latitude != null && longitude != null) {
        String.format(Locale.US, "%.6f, %.6f", latitude, longitude)
    } else {
        "Ubicación no disponible"
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

private fun elapsedAlertTime(timestamp: Long, now: Long): String {
    val seconds = ((now - timestamp) / 1_000).coerceAtLeast(0)
    return when {
        seconds < 10 -> "Ahora"
        seconds < 60 -> "Hace $seconds seg"
        seconds < 3_600 -> "Hace ${seconds / 60} min"
        seconds < 86_400 -> "Hace ${seconds / 3_600} h"
        else -> "Hace ${seconds / 86_400} d"
    }
}
