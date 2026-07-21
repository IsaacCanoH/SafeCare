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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import mx.utng.ich.safecare.ui.viewmodel.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProfileScreen(
    viewModel: ProfileViewModel,
    onBackClick: () -> Unit = {},
    onSaveSuccess: () -> Unit = {}
) {
    var name by remember { mutableStateOf("") }
    var ageStr by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var serialNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Menor de edad") }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val profileTypes = listOf(
        Triple("Menor de edad", Icons.Default.ChildCare, "Menor"),
        Triple("Adulto mayor", Icons.Default.Elderly, "Adulto"),
        Triple("Cuidador", Icons.Default.SupervisorAccount, "Cuidador")
    )

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
                Text("Agregar nuevo perfil", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    TextButton(onClick = {
                        if (name.isBlank() || ageStr.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar("Por favor ingresa nombre y edad") }
                            return@TextButton
                        }
                        val edad = ageStr.toIntOrNull() ?: 0
                        viewModel.addProfile(name, edad, selectedType, if(selectedType != "Cuidador") serialNumber else null) { success ->
                            if (success) onSaveSuccess()
                            else {
                                scope.launch { snackbarHostState.showSnackbar("Error al guardar. Verifica el tipo en Supabase.") }
                            }
                        }
                    }) {
                        Text("Guardar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Tipo de perfil", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                profileTypes.forEach { (label, icon, value) ->
                    ProfileTypeChip(
                        selected = selectedType == label,
                        onClick = { selectedType = label },
                        label = label,
                        icon = icon,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Información básica", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre completo") },
                placeholder = { Text("Ej. Juan Pérez") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                enabled = !isLoading,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Next
                )
            )

            OutlinedTextField(
                value = ageStr,
                onValueChange = { if (it.all { char -> char.isDigit() }) ageStr = it },
                label = { Text("Edad") },
                placeholder = { Text("Ej. 70") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                enabled = !isLoading,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Next
                )
            )

            if (selectedType != "Cuidador") {
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
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Información de dispositivo", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

                OutlinedTextField(
                    value = serialNumber,
                    onValueChange = { serialNumber = it },
                    label = { Text("Número de serie SmartWatch") },
                    placeholder = { Text("Ej. SW-ABC123456") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    enabled = !isLoading
                )
            } else {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo electrónico") },
                    placeholder = { Text("ejemplo@correo.com") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    enabled = !isLoading
                )
            }

            Button(
                onClick = {
                    val edad = ageStr.toIntOrNull() ?: 0 
                    viewModel.addProfile(name, edad, selectedType, if(selectedType != "Cuidador") serialNumber else null) { success ->
                        if (success) onSaveSuccess()
                        else {
                            scope.launch { snackbarHostState.showSnackbar("Error al guardar.") }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium,
                enabled = !isLoading && name.isNotEmpty() && ageStr.isNotEmpty()
            ) {
                Text("Guardar perfil", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTypeChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { 
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                Text(label.split(" ").first(), fontSize = 10.sp)
            }
        },
        modifier = modifier
    )
}
