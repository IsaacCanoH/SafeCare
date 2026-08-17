package mx.utng.ich.safecare.wearable.presentation.ui

/**
 * Objeto contenedor del estado completo de la interfaz para la pantalla de inicio del reloj (Watch Face / App Home).
 *  * Maneja estados complejos en un solo objeto para facilitar la renderizaciÃ³n reactiva en Jetpack Compose for Wear OS.
 */
data class WearHomeUiState(
    val greetingName: String = "SafeCare",
    val locationPermissionStatus: String = "Permiso de ubicación pendiente",
    val locationText: String = "Ubicación todavía no consultada",
    val batteryText: String = "Batería todavía no consultada",
    val connectionText: String = "Conexión todavía no consultada",
    val panicButtonText: String = "SOS"
)