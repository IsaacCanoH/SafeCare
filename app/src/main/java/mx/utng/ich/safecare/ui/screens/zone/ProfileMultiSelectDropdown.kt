package mx.utng.ich.safecare.ui.screens.zone

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mx.utng.ich.safecare.data.local.entity.PerfilMonitoreadoEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileMultiSelectDropdown(
    profiles: List<PerfilMonitoreadoEntity>,
    selectedProfileIds: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedProfiles = profiles.filter { it.idPerfil in selectedProfileIds }
    val fieldValue = when (selectedProfiles.size) {
        0 -> ""
        1 -> selectedProfiles.single().nombre
        else -> "${selectedProfiles.size} perfiles seleccionados"
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            if (profiles.isNotEmpty()) expanded = !expanded
        },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = fieldValue,
            onValueChange = {},
            readOnly = true,
            enabled = profiles.isNotEmpty(),
            label = { Text("Perfiles monitoreados") },
            placeholder = { Text("No hay perfiles registrados") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 280.dp)
        ) {
            profiles.forEach { profile ->
                val isSelected = profile.idPerfil in selectedProfileIds
                DropdownMenuItem(
                    text = { Text(profile.nombre) },
                    onClick = {
                        onSelectionChange(
                            if (isSelected) selectedProfileIds - profile.idPerfil
                            else selectedProfileIds + profile.idPerfil
                        )
                    },
                    leadingIcon = {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null
                        )
                    }
                )
            }
        }
    }
}
