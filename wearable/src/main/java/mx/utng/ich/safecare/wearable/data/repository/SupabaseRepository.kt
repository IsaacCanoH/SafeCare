package mx.utng.ich.safecare.wearable.data.repository

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import mx.utng.ich.safecare.wearable.data.local.entity.UbicacionEntity
import mx.utng.ich.safecare.wearable.data.local.entity.AlertaEntity
import mx.utng.ich.safecare.wearable.data.remote.SupabaseClient
import android.util.Log
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mx.utng.ich.safecare.wearable.data.local.entity.PerfilMonitoreadoEntity
import mx.utng.ich.safecare.wearable.data.local.entity.ZonaSeguraEntity

class SupabaseRepository {

    private val client = SupabaseClient.client

    /** Sincroniza el estado actual del smartwatch con Supabase. */
    suspend fun updateSmartWatchStatus(
        numeroSerie: String,
        bateria: Int,
        conexion: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val updateData = buildJsonObject {
                put("bateria", bateria)
                put("conexion", conexion.lowercase())
                put("ultimaConexion", System.currentTimeMillis())
            }
            
            client.postgrest["SmartWatch"].update(updateData) {
                filter {
                    eq("numeroSerie", numeroSerie)
                }
            }
            "success"
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "Error: ${e.message}")
            null
        }
    }

    /** Guarda la ubicación generada por el smartwatch en Supabase. */
    suspend fun saveLocation(location: UbicacionEntity): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val locationData = buildJsonObject {
                    put("idUbicacion", location.idUbicacion)
                    put("latitud", location.latitud)
                    put("longitud", location.longitud)
                    put("fechaHora", location.fechaHora)
                    put("idSmartwatch", location.idSmartwatch)
                }
                client.postgrest["Ubicacion"].upsert(locationData) {
                    onConflict = "idUbicacion"
                }
                true
            } catch (exception: Exception) {
                Log.e(
                    "SupabaseRepo",
                    "No se pudo guardar ubicación ${location.idUbicacion}",
                    exception
                )
                false
            }
        }

    /** Guarda una alerta del smartwatch en Supabase. */
    suspend fun saveAlert(alert: AlertaEntity): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val alertData = buildJsonObject {
                    put("idAlerta", alert.idAlerta)
                    put("tipoAlerta", alert.tipoAlerta)
                    put("descripcion", alert.descripcion)
                    put("fechaHora", alert.fechaHora)
                    put("estado", alert.estado)
                    put("idPerfil", alert.idPerfil)
                    alert.idUbicacion?.let { put("idUbicacion", it) }
                }
                client.postgrest["Alerta"].upsert(alertData) {
                    onConflict = "idAlerta"
                }
                true
            } catch (exception: Exception) {
                Log.e(
                    "SupabaseRepo",
                    "No se pudo guardar alerta ${alert.idAlerta}",
                    exception
                )
                false
            }
        }

    /** Obtiene la configuración remota vinculada a este reloj. */
    suspend fun fetchLinkedConfiguration(numeroSerie: String): LinkedConfiguration? =
        withContext(Dispatchers.IO) {
            try {
                val profileId = client.postgrest["SmartWatch"].select(Columns.list("idPerfil")) {
                    filter { eq("numeroSerie", numeroSerie) }
                }.decodeList<WatchLinkRow>().firstOrNull()?.idPerfil ?: return@withContext null

                val profile = client.postgrest["PerfilMonitoreado"].select {
                    filter { eq("idPerfil", profileId) }
                }.decodeList<ProfileRow>().firstOrNull() ?: return@withContext null

                val zoneIds = client.postgrest["ZonaSeguraPerfil"].select {
                    filter { eq("idPerfil", profileId) }
                }.decodeList<SafeZoneProfileRow>().map(SafeZoneProfileRow::zoneId)

                val zones = if (zoneIds.isEmpty()) {
                    emptyList()
                } else {
                    client.postgrest["ZonaSegura"].select {
                        filter { isIn("idZona", zoneIds) }
                    }.decodeList<SafeZoneRow>()
                }.map { row ->
                    ZonaSeguraEntity(
                        idZona = row.id,
                        nombre = row.nombre,
                        latitudCentro = row.latitudCentro,
                        longitudCentro = row.longitudCentro,
                        radioMetros = row.radioMetros,
                        activa = row.activa,
                        idPerfil = profileId
                    )
                }

                LinkedConfiguration(
                    profile = PerfilMonitoreadoEntity(
                        idPerfil = profile.id,
                        nombre = profile.nombre,
                        edad = profile.edad,
                        fechaNacimiento = profile.fechaNacimiento,
                        tipoPerfil = profile.tipoPerfil,
                        foto = profile.foto,
                        estadoActual = profile.estadoActual,
                        idCuidador = profile.idCuidador
                    ),
                    zones = zones
                )
            } catch (exception: Exception) {
                Log.w("SupabaseRepo", "No se pudo consultar la configuración remota", exception)
                null
            }
        }

    @Serializable
    private data class WatchLinkRow(@SerialName("idPerfil") val idPerfil: String? = null)

    @Serializable
    private data class ProfileRow(
        @SerialName("idPerfil") val id: String,
        val nombre: String,
        val edad: Int,
        @SerialName("fechaNacimiento") val fechaNacimiento: String? = null,
        @SerialName("tipoPerfil") val tipoPerfil: String = "menor",
        val foto: String? = null,
        @SerialName("estadoActual") val estadoActual: Boolean = true,
        @SerialName("idCuidador") val idCuidador: String
    )

    @Serializable
    private data class SafeZoneRow(
        @SerialName("idZona") val id: String,
        val nombre: String,
        @SerialName("latitudCentro") val latitudCentro: Double,
        @SerialName("longitudCentro") val longitudCentro: Double,
        @SerialName("radioMetros") val radioMetros: Double,
        val activa: Boolean = true,
        @SerialName("idPerfil") val idPerfil: String
    )

    @Serializable
    private data class SafeZoneProfileRow(
        @SerialName("idZona") val zoneId: String
    )
}

data class LinkedConfiguration(
    val profile: PerfilMonitoreadoEntity,
    val zones: List<ZonaSeguraEntity>
)
