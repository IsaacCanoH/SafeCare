package mx.utng.ich.safecare.wearable.data.repository

import mx.utng.ich.safecare.wearable.data.model.Alerta
import mx.utng.ich.safecare.wearable.data.model.SmartWatch
import mx.utng.ich.safecare.wearable.data.model.Ubicacion
import mx.utng.ich.safecare.wearable.data.model.ZonaSegura
import mx.utng.ich.safecare.wearable.data.remote.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put


class SupabaseRepository {

    private val client = SupabaseClient.client

    suspend fun insertUbicacion(ubicacion: Ubicacion): Ubicacion? = withContext(Dispatchers.IO) {
        Log.d("SupabaseRepo", "Intentando insertar ubicación: $ubicacion")
        try {
            val response = client.postgrest["ubicacion"].insert(ubicacion) {
                select(Columns.ALL)
            }.decodeSingleOrNull<Ubicacion>()
            Log.i("SupabaseRepo", "Ubicación insertada con éxito: ${response?.id}")
            response
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "ERROR insertando ubicación: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    suspend fun insertAlerta(alerta: Alerta) = withContext(Dispatchers.IO) {
        Log.d("SupabaseRepo", "Intentando insertar alerta: $alerta")
        try {
            client.postgrest["alerta"].insert(alerta)
            Log.i("SupabaseRepo", "Alerta insertada con éxito")
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "ERROR insertando alerta: ${e.message}")
            e.printStackTrace()
        }
    }

    suspend fun updateSmartWatchStatus(
        numeroSerie: String,
        bateria: Int,
        conexion: String
    ): SmartWatch? = withContext(Dispatchers.IO) {
        Log.d("SupabaseRepo", "Actualizando estado. Serial: '$numeroSerie', Bat: $bateria")
        try {
            // Creamos un JsonObject explícito para que el serializador no falle
            val updateData = buildJsonObject {
                put("bateria", bateria)
                put("conexion", conexion.lowercase())
                // Forzamos a que la base de datos actualice la fecha al momento actual
                put("ultima_conexion", java.time.OffsetDateTime.now().toString())
            }
            
            val response = client.postgrest["smartwatch"].update(updateData) {
                filter {
                    eq("numero_serie", numeroSerie)
                }
                select()
            }.decodeSingleOrNull<SmartWatch>()
            
            if (response == null) {
                Log.w("SupabaseRepo", "⚠️ NO se encontró el smartwatch con serial '$numeroSerie'")
            } else {
                Log.i("SupabaseRepo", "✅ Smartwatch actualizado en DB. UUID: ${response.id}")
            }
            response
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "❌ ERROR en updateSmartWatchStatus: ${e.message}")
            null
        }
    }

    suspend fun getZonasSeguras(idPerfil: String): List<ZonaSegura> = withContext(Dispatchers.IO) {
        try {
            client.postgrest["zona_segura"].select {
                filter {
                    eq("id_perfil", idPerfil)
                    eq("activa", true)
                }
            }.decodeList<ZonaSegura>()
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "Error obteniendo zonas seguras: ${e.message}")
            emptyList()
        }
    }

    suspend fun cleanOldUbicaciones() = withContext(Dispatchers.IO) {
        // Implementar lógica de eliminación de registros > 7 días si es necesario
        // En Supabase/Postgres es mejor hacerlo con una Function o Cron Job
    }
}
