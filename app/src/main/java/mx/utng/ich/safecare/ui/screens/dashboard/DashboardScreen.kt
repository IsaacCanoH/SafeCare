package mx.utng.ich.safecare.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class MonitoredPerson(
    val id: String,
    val name: String,
    val type: String,
    val status: String,
    val battery: Int,
    val connection: String,
    val lastUpdate: String,
    val isInSafeZone: Boolean,
    val safeZonesCount: Int = 0, // Nuevo campo
    val isSosActive: Boolean = false
)

@Composable
fun DashboardContent(
    userName: String = "Usuario",
    monitoredPersons: List<MonitoredPerson> = emptyList(),
    onAddPersonClick: () -> Unit = {},
    onPersonClick: (MonitoredPerson) -> Unit = {}
) {
    val persons = monitoredPersons

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hola, $userName",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (persons.isEmpty()) "Aún no tienes personas registradas." 
                          else "Este es el estado de tus\npersonas monitoreadas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Button(
                onClick = onAddPersonClick,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Agregar", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (persons.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Presiona 'Agregar' para comenzar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(persons) { person ->
                    PersonCard(person = person, onClick = { onPersonClick(person) })
                }
            }
        }
    }
}

@Composable
fun PersonCard(person: MonitoredPerson, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Image Placeholder
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = person.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${person.type} • ${person.safeZonesCount} zonas seguras",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Surface(
                    color = when {
                        person.isSosActive -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        person.isInSafeZone -> Color(0xFFE8F5E9)
                        else -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    },
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        person.isSosActive -> MaterialTheme.colorScheme.error
                                        person.isInSafeZone -> Color(0xFF4CAF50)
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = person.status,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                person.isSosActive -> MaterialTheme.colorScheme.error
                                person.isInSafeZone -> Color(0xFF2E7D32)
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatusItem(Icons.Default.BatteryFull, "${person.battery}%", "Batería")
            StatusItem(Icons.Default.Wifi, person.connection, "Conexión")
            StatusItem(Icons.Default.History, person.lastUpdate, "Actualizado")
        }
    }
}

@Composable
fun StatusItem(icon: ImageVector, value: String, label: String) {
    Column {
        Text(text = label, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = value, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}
