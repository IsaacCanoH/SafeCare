package mx.utng.ich.safecaretv.data.profile

/**
 * EnumeraciÃ³n MonitoringStatus del sistema SafeCare.
 */
enum class MonitoringStatus {
    SAFE,
    OUTSIDE_SAFE_ZONE,
    SOS,
    OFFLINE
}

/**
 * Perfil exhaustivo que encapsula a una persona monitoreada junto con todas sus configuraciones y mÃ©tricas asociadas.
 *  * Proporciona una vista unificada del estado actual del paciente para ser consumida por los cuadros de mando (dashboards).
 */
data class MonitoredProfile(
    val id: String,
    val name: String,
    val age: Int,
    val profileType: String,
    val birthDate: String?,
    val photoUrl: String?,
    val batteryLevel: Int?,
    val isOnline: Boolean,
    val status: MonitoringStatus,
    val watchName: String?,
    val lastConnection: Long?,
    val latitude: Double?,
    val longitude: Double?,
    val locationTimestamp: Long?,
    val currentSafeZoneName: String?,
    val safeZones: List<SafeZoneInfo>,
    /** Identificadores aceptados por Ubicacion para actualizar este perfil desde Realtime. */
    val watchIds: Set<String> = emptySet()
)

/**
 * Envoltorio de informaciÃ³n consolidada que agrupa todas las zonas seguras activas y asignadas a un perfil especÃ­fico.
 *  * Facilita el transporte de datos espaciales entre las capas de dominio y presentaciÃ³n.
 */
data class SafeZoneInfo(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double
)