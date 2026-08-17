package mx.utng.ich.safecaretv.data.alert

/**
 * Variante del modelo de alerta adaptada especÃ­ficamente a las caracterÃ­sticas visuales y requerimientos de Android TV.
 *  * Contiene metadatos adicionales para optimizar su despliegue en pantallas de gran formato (Leanback).
 */
data class TvAlert(
    val id: String,
    val type: String,
    val description: String,
    val timestamp: Long,
    val profileId: String
) {
    val isSos: Boolean
        get() = type.equals("SOS", true)

    val isSafeZoneExit: Boolean
        get() = type.equals("FUERA_ZONA_SEGURA", true) ||
            type.equals("ZONA_SEGURA", true) ||
            type.equals("ZONA_SEGURA_SALIDA", true)
}