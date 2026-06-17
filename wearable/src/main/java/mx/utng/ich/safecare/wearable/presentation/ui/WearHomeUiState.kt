package mx.utng.ich.safecare.wearable.presentation.ui

data class WearHomeUiState(
    val greetingName: String = "SafeCare",
    val locationPermissionStatus: String = "Permiso de ubicación pendiente",
    val locationText: String = "Ubicación todavía no consultada",
    val batteryText: String = "Batería todavía no consultada",
    val connectionText: String = "Conexión todavía no consultada",
    val panicButtonText: String = "SOS"
)