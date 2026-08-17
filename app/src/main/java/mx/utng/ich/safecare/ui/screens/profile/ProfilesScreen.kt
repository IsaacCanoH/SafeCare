package mx.utng.ich.safecare.ui.screens.profile

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
import mx.utng.ich.safecare.data.local.entity.PerfilMonitoreadoEntity
import mx.utng.ich.safecare.ui.viewmodel.ProfileViewModel

@Composable
/** Muestra la lista de perfiles disponibles para el cuidador. */
fun ProfilesScreen(
    viewModel: ProfileViewModel,
    onAddProfileClick: () -> Unit = {},
    onEditProfileClick: (PerfilMonitoreadoEntity) -> Unit = {}
) {
    val profiles by viewModel.profiles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var profileToDelete by remember { mutableStateOf<PerfilMonitoreadoEntity?>(null) }

    if (profileToDelete != null) {
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            title = { Text("Eliminar perfil") },
            text = { Text("¿Estás seguro de que deseas eliminar a ${profileToDelete?.nombre}? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        profileToDelete?.let {
                            viewModel.deleteProfile(it.idPerfil) { success ->
                                if (success) profileToDelete = null
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Perfiles", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            IconButton(onClick = onAddProfileClick) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading && profiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (profiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay perfiles registrados", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(profiles) { profile ->
                    ProfileItem(
                        profile = profile, 
                        onEditClick = { onEditProfileClick(profile) },
                        onDeleteClick = { profileToDelete = profile }
                    )
                }
            }
        }
    }
}

@Composable
/** Muestra las acciones y datos básicos de un perfil. */
fun ProfileItem(
    profile: PerfilMonitoreadoEntity, 
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = profile.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = "${profile.tipoPerfil.replace("_", " ").capitalize()} • ${profile.edad} años", 
                    fontSize = 12.sp, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
