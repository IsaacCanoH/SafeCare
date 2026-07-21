package mx.utng.ich.safecare.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import mx.utng.ich.safecare.data.local.entity.PerfilMonitoreadoEntity
import mx.utng.ich.safecare.ui.viewmodel.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    profile: PerfilMonitoreadoEntity,
    viewModel: ProfileViewModel,
    onBackClick: () -> Unit = {},
    onSaveSuccess: () -> Unit = {}
) {
    var name by remember { mutableStateOf(profile.nombre) }
    var ageStr by remember { mutableStateOf(profile.edad.toString()) }
    var birthDate by remember { mutableStateOf(profile.fechaNacimiento ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    val isLoading by viewModel.isLoading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val date = Date(it)
                        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        birthDate = formatter.format(date)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                }
                Text("Editar perfil", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    TextButton(onClick = {
                        if (name.isBlank() || ageStr.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar("Completa todos los campos") }
                            return@TextButton
                        }
                        viewModel.updateProfile(profile.idPerfil, name, ageStr.toIntOrNull() ?: 0, birthDate) { success ->
                            if (success) onSaveSuccess()
                            else scope.launch { snackbarHostState.showSnackbar("Error al actualizar") }
                        }
                    }) {
                        Text("Actualizar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Información básica", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre completo") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                enabled = !isLoading
            )

            OutlinedTextField(
                value = ageStr,
                onValueChange = { if (it.all { char -> char.isDigit() }) ageStr = it },
                label = { Text("Edad") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                enabled = !isLoading,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                )
            )

            // Campo de fecha de nacimiento agregado
            OutlinedTextField(
                value = birthDate,
                onValueChange = { },
                readOnly = true,
                label = { Text("Fecha de nacimiento") },
                placeholder = { Text("dd/mm/aaaa") },
                trailingIcon = { 
                    IconButton(onClick = { showDatePicker = true }, enabled = !isLoading) {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                enabled = !isLoading
            )

            Button(
                onClick = {
                    if (name.isBlank() || ageStr.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("Completa todos los campos") }
                        return@Button
                    }
                    viewModel.updateProfile(profile.idPerfil, name, ageStr.toIntOrNull() ?: 0, birthDate) { success ->
                        if (success) onSaveSuccess()
                        else scope.launch { snackbarHostState.showSnackbar("Error al actualizar") }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium,
                enabled = !isLoading && name.isNotEmpty()
            ) {
                Text("Guardar cambios", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
