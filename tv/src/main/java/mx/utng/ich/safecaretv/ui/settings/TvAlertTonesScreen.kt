package mx.utng.ich.safecaretv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.ich.safecaretv.data.sound.AlertTone
import mx.utng.ich.safecaretv.data.sound.AlertTonePlayer
import mx.utng.ich.safecaretv.data.sound.AlertTonePreferences
import mx.utng.ich.safecaretv.data.sound.AlertTones
import mx.utng.ich.safecaretv.ui.theme.SafeBackground
import mx.utng.ich.safecaretv.ui.theme.SafeNavy
import mx.utng.ich.safecaretv.ui.theme.SafePurple
import mx.utng.ich.safecaretv.ui.theme.SafePurpleLight
import mx.utng.ich.safecaretv.ui.theme.SafeTextMuted

@Composable
// Permite elegir y previsualizar el tono de alertas de TV.
fun TvAlertTonesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val player = remember { AlertTonePlayer(context.applicationContext) }
    var selectedToneId by remember {
        mutableIntStateOf(AlertTonePreferences.selected(context).id)
    }

    DisposableEffect(player) {
        onDispose(player::stop)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SafeBackground)
            .padding(horizontal = 42.dp, vertical = 28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onClick = onBack)
            Column(modifier = Modifier.padding(start = 22.dp)) {
                Text(
                    text = "Tonos de alerta",
                    color = SafeNavy,
                    fontSize = 29.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Selecciona el tono que sonará cuando ocurra una alerta.",
                    color = SafeTextMuted,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(Modifier.height(26.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(AlertTones.all, key = AlertTone::id) { tone ->
                ToneCard(
                    tone = tone,
                    selected = tone.id == selectedToneId,
                    onSelect = {
                        selectedToneId = tone.id
                        AlertTonePreferences.select(context, tone)
                    },
                    onPreview = { player.playPreview(tone) }
                )
            }
        }
    }
}

@Composable
// Muestra una opción de tono y permite seleccionarla.
private fun ToneCard(
    tone: AlertTone,
    selected: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val outline = when {
        focused -> SafeNavy
        selected -> SafePurple
        else -> Color(0xFFE2DFEA)
    }

    Surface(
        onClick = onSelect,
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = if (focused) 8.dp else 1.dp,
        modifier = Modifier
            .height(184.dp)
            .scale(if (focused) 1.035f else 1f)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .border(if (selected || focused) 3.dp else 1.dp, outline, RoundedCornerShape(16.dp))
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(29.dp)
                        .background(SafePurple, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(19.dp))
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(64.dp).background(SafePurpleLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = SafePurple,
                        modifier = Modifier.size(35.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(tone.name, color = SafeNavy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(tone.description, color = SafeTextMuted, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                Surface(
                    onClick = onPreview,
                    color = SafePurpleLight,
                    shape = CircleShape,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Escuchar ${tone.name}",
                            tint = SafePurple,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
// Muestra el botón para regresar a la pantalla anterior.
private fun BackButton(onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        color = if (focused) SafePurpleLight else Color.Transparent,
        shape = CircleShape,
        modifier = Modifier
            .size(52.dp)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = SafeNavy,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}
