package mx.utng.ich.safecaretv.data.profile

enum class MonitoringStatus {
    SAFE,
    OUTSIDE_SAFE_ZONE,
    SOS,
    OFFLINE
}

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

data class SafeZoneInfo(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double
)
