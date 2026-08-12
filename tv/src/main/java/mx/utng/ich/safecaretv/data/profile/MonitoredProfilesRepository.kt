package mx.utng.ich.safecaretv.data.profile

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mx.utng.ich.safecaretv.data.remote.TvSupabaseClient
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MonitoredProfilesRepository {
    private val client = TvSupabaseClient.client

    // Obtiene perfiles, relojes, ubicaciones y zonas para la TV.
    suspend fun getProfiles(): List<MonitoredProfile> = coroutineScope {
        val caregiverId = client.auth.currentSessionOrNull()?.user?.id
            ?: error("La sesión ha expirado")

        val profiles = client.postgrest["PerfilMonitoreado"].select {
            filter { eq("idCuidador", caregiverId) }
        }.decodeList<ProfileRow>()

        if (profiles.isEmpty()) return@coroutineScope emptyList()

        val profileIds = profiles.map { it.id }
        val watchesDeferred = async {
            client.postgrest["SmartWatch"].select {
                filter { isIn("idPerfil", profileIds) }
            }.decodeList<WatchRow>()
        }
        val zonesDeferred = async {
            client.postgrest["ZonaSegura"].select {
                filter {
                    isIn("idPerfil", profileIds)
                    eq("activa", true)
                }
            }.decodeList<SafeZoneRow>()
        }

        val watches = watchesDeferred.await()
        val zones = zonesDeferred.await()
        val watchIds = watches
            .flatMap { listOfNotNull(it.id, it.serialNumber) }
            .distinct()
        val locations = watchIds.mapNotNull { watchId ->
            client.postgrest["Ubicacion"].select {
                filter { eq("idSmartwatch", watchId) }
                order("fechaHora", Order.DESCENDING)
                limit(1)
            }.decodeList<LocationRow>().firstOrNull()
        }

        val watchByProfile = watches
            .filter { !it.profileId.isNullOrBlank() }
            .associateBy { it.profileId!! }
        val latestLocationByWatch = locations
            .groupBy { it.watchId }
            .mapValues { (_, values) -> values.maxByOrNull(LocationRow::timestamp) }
        val zonesByProfile = zones.groupBy(SafeZoneRow::profileId)

        profiles.map { profile ->
            val watch = watchByProfile[profile.id]
            val online = watch?.connection.equals("online", ignoreCase = true) ||
                watch?.connection.equals("bluetooth", ignoreCase = true)
            val location = watch?.let {
                latestLocationByWatch[it.id] ?: it.serialNumber?.let(latestLocationByWatch::get)
            }
            val profileZones = zonesByProfile[profile.id].orEmpty()
            val currentZone = location?.let { currentLocation ->
                profileZones.firstOrNull { zone ->
                    distanceMeters(
                        currentLocation.latitude,
                        currentLocation.longitude,
                        zone.latitude,
                        zone.longitude
                    ) <= zone.radiusMeters
                }
            }
            val isOutside = location != null &&
                profileZones.isNotEmpty() &&
                profileZones.none { zone ->
                    distanceMeters(
                        location.latitude,
                        location.longitude,
                        zone.latitude,
                        zone.longitude
                    ) <= zone.radiusMeters
                }

            MonitoredProfile(
                id = profile.id,
                name = profile.name,
                age = profile.age,
                profileType = profile.profileType,
                birthDate = profile.birthDate,
                photoUrl = profile.photoUrl,
                batteryLevel = watch?.battery?.coerceIn(0, 100),
                isOnline = online,
                status = when {
                    !online -> MonitoringStatus.OFFLINE
                    isOutside -> MonitoringStatus.OUTSIDE_SAFE_ZONE
                    else -> MonitoringStatus.SAFE
                },
                watchName = watch?.deviceName ?: watch?.model ?: watch?.serialNumber,
                lastConnection = watch?.lastConnection,
                latitude = location?.latitude,
                longitude = location?.longitude,
                locationTimestamp = location?.timestamp,
                currentSafeZoneName = currentZone?.name,
                safeZones = profileZones.map {
                    SafeZoneInfo(it.name, it.latitude, it.longitude, it.radiusMeters)
                },
                watchIds = watch?.let { listOfNotNull(it.id, it.serialNumber).toSet() }.orEmpty()
            )
        }
    }

    // Calcula la distancia en metros entre dos coordenadas.
    private fun distanceMeters(
        latitudeA: Double,
        longitudeA: Double,
        latitudeB: Double,
        longitudeB: Double
    ): Double {
        val earthRadiusMeters = 6_371_000.0
        val latitudeDelta = Math.toRadians(latitudeB - latitudeA)
        val longitudeDelta = Math.toRadians(longitudeB - longitudeA)
        val a = sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
            cos(Math.toRadians(latitudeA)) * cos(Math.toRadians(latitudeB)) *
            sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
        return earthRadiusMeters * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

}

@Serializable
private data class ProfileRow(
    @SerialName("idPerfil") val id: String,
    @SerialName("nombre") val name: String,
    @SerialName("edad") val age: Int,
    @SerialName("tipoPerfil") val profileType: String = "menor",
    @SerialName("fechaNacimiento") val birthDate: String? = null,
    @SerialName("foto") val photoUrl: String? = null
)

@Serializable
private data class WatchRow(
    @SerialName("idSmartwatch") val id: String,
    @SerialName("bateria") val battery: Int = 0,
    @SerialName("conexion") val connection: String = "offline",
    @SerialName("ultimaConexion") val lastConnection: Long? = null,
    @SerialName("numeroSerie") val serialNumber: String? = null,
    @SerialName("nombreDispositivo") val deviceName: String? = null,
    @SerialName("modelo") val model: String? = null,
    @SerialName("idPerfil") val profileId: String? = null
)

@Serializable
private data class SafeZoneRow(
    @SerialName("nombre") val name: String,
    @SerialName("latitudCentro") val latitude: Double,
    @SerialName("longitudCentro") val longitude: Double,
    @SerialName("radioMetros") val radiusMeters: Double,
    @SerialName("idPerfil") val profileId: String
)

@Serializable
private data class LocationRow(
    @SerialName("latitud") val latitude: Double,
    @SerialName("longitud") val longitude: Double,
    @SerialName("fechaHora") val timestamp: Long,
    @SerialName("idSmartwatch") val watchId: String
)
