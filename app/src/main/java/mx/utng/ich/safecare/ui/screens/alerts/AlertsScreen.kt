package mx.utng.ich.safecare.ui.screens.alerts

import androidx.compose.foundation.background
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
import mx.utng.ich.safecare.data.local.entity.AlertaConPerfil
import mx.utng.ich.safecare.ui.viewmodel.AlertViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
// Muestra las alertas recibidas y mantiene su contenido actualizado.
fun AlertsScreen(viewModel: AlertViewModel) {
    val alerts by viewModel.alerts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Centro de Alertas", 
            style = MaterialTheme.typography.headlineSmall, 
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (alerts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay alertas recientes", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(alerts, key = { it.alerta.idAlerta }) { alert ->
                    AlertItem(
                        item = alert,
                        onAcknowledge = {
                            viewModel.acknowledgeAlert(alert.alerta.idAlerta)
                        }
                    )
                }
            }
        }
    }
}

@Composable
// Presenta la información principal de una alerta individual.
@OptIn(ExperimentalMaterial3Api::class)
fun AlertItem(item: AlertaConPerfil, onAcknowledge: () -> Unit) {
    if (item.alerta.estado != "ACTIVA") {
        AlertCard(item)
        return
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onAcknowledge()
            }
            false
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { AcknowledgeAlertBackground() },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        AlertCard(item)
    }
}

@Composable
private fun AcknowledgeAlertBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(end = 24.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Reconocer",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AlertCard(item: AlertaConPerfil) {
    val alert = item.alerta
    val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    val dateStr = sdf.format(Date(alert.fechaHora))
    val isActive = alert.estado == "ACTIVA"
    val isSos = alert.tipoAlerta == "SOS"
    val isCustomAlert = alert.tipoAlerta == "ALERTA"
    val accentColor = when {
        !isActive -> MaterialTheme.colorScheme.outline
        isSos -> Color(0xFFC62828)
        isCustomAlert -> MaterialTheme.colorScheme.primary
        else -> Color(0xFFF9A825)
    }
    val containerColor = when {
        !isActive -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        isSos -> Color(0xFFFFEBEE)
        isCustomAlert -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else -> Color(0xFFFFF8E1)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        accentColor
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isSos -> Icons.Default.Warning
                        isCustomAlert -> Icons.Default.Campaign
                        else -> Icons.Default.LocationOff
                    },
                    contentDescription = null, 
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alertTitle(item),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = alertMessage(item),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!isActive) {
                    Text(
                        text = "Atendida",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(text = dateStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// Genera el mensaje visible según el tipo de alerta.
fun alertMessage(item: AlertaConPerfil): String {
    val name = item.nombrePerfil?.trim().takeUnless { it.isNullOrEmpty() }
        ?: "Perfil sin nombre"
    return when (item.alerta.tipoAlerta) {
        "SOS" -> "$name activó una alerta SOS desde su reloj."
        "ALERTA" -> item.alerta.descripcion
        else -> "$name salió del perímetro de la zona segura."
    }
}

// Genera el título visible según el tipo de alerta.
fun alertTitle(item: AlertaConPerfil): String =
    when (item.alerta.tipoAlerta) {
        "SOS" -> "SOS"
        "ALERTA" -> "Alerta personalizada"
        else -> "Fuera de zona segura"
    }
