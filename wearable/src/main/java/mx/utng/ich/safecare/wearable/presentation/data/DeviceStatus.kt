package mx.utng.ich.safecare.wearable.presentation.data

/**
 * Modelo integral que refleja el estado operativo y las mÃ©tricas actuales del dispositivo fÃ­sico (como un smartwatch).
 *  * Incluye datos vitales operativos como el nivel de baterÃ­a, estado de la conexiÃ³n Bluetooth/Wi-Fi, y precisiÃ³n de los sensores.
 */
data class DeviceStatus(
    val batteryText: String,
    val connectionText: String
)