package mx.utng.ich.safecaretv.data.alert

import io.github.jan.supabase.postgrest.postgrest
import java.time.Instant
import java.time.OffsetDateTime
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import mx.utng.ich.safecaretv.data.remote.TvSupabaseClient

class TvAlertsRepository {
    private val client = TvSupabaseClient.client

    suspend fun getActiveAlerts(): List<TvAlert> =
        client.postgrest["Alerta"].select {
            filter { eq("estado", "ACTIVA") }
        }.decodeList<JsonObject>()
            .mapNotNull(::toAlert)
            .filter { it.isSos || it.isSafeZoneExit }
            .sortedByDescending(TvAlert::timestamp)

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

    private fun parseTimestamp(value: String?): Long? {
        if (value == null) return null
        return value.toLongOrNull()
            ?: runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }.getOrNull()
    }

    private fun JsonObject.text(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key ->
            get(key)?.jsonPrimitive?.contentOrNull
        }
}
