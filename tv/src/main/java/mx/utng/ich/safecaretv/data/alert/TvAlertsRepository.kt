package mx.utng.ich.safecaretv.data.alert

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import java.time.Instant
import java.time.OffsetDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import mx.utng.ich.safecaretv.data.remote.TvSupabaseClient

class TvAlertsRepository {
    private val client = TvSupabaseClient.client

    // Consulta solamente las alertas activas de los perfiles del cuidador en sesión.
    suspend fun getActiveAlerts(): List<TvAlert> {
        val caregiverId = client.auth.currentSessionOrNull()?.user?.id
            ?: error("La sesión ha expirado")
        val profileIds = client.postgrest["PerfilMonitoreado"].select {
            filter { eq("idCuidador", caregiverId) }
        }.decodeList<ProfileIdRow>().map(ProfileIdRow::id)
        if (profileIds.isEmpty()) return emptyList()

        return client.postgrest["Alerta"].select {
            filter {
                eq("estado", "ACTIVA")
                isIn("idPerfil", profileIds)
            }
        }.decodeList<JsonObject>()
            .mapNotNull(::toAlert)
            .filter { it.isSos || it.isSafeZoneExit }
            .sortedByDescending(TvAlert::timestamp)
    }

    // Reconoce una alerta en Supabase para retirarla de todos los dispositivos.
    suspend fun acknowledgeAlert(alertId: String) {
        client.postgrest["Alerta"].update(
            buildJsonObject { put("estado", "ATENDIDA") }
        ) {
            filter { eq("idAlerta", alertId) }
        }
    }

    // Convierte una fila remota al modelo de alerta de TV.
    private fun toAlert(row: JsonObject): TvAlert? {
        val id = row.text("idAlerta") ?: return null
        val type = row.text("tipoAlerta") ?: return null
        val profileId = row.text("idPerfil") ?: return null
        return TvAlert(
            id = id,
            type = type,
            description = row.text("descripcion").orEmpty(),
            timestamp = parseTimestamp(row.text("fechaHora")) ?: return null,
            profileId = profileId
        )
    }

    // Convierte la fecha remota a milisegundos desde época.
    private fun parseTimestamp(value: String?): Long? {
        if (value == null) return null
        return value.toLongOrNull()
            ?: runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }.getOrNull()
    }

    // Busca el primer texto disponible entre varias claves JSON.
    private fun JsonObject.text(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key ->
            get(key)?.jsonPrimitive?.contentOrNull
        }

    @Serializable
    private data class ProfileIdRow(@SerialName("idPerfil") val id: String)
}
