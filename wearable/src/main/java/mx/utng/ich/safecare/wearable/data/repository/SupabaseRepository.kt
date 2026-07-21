package mx.utng.ich.safecare.wearable.data.repository

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import mx.utng.ich.safecare.wearable.data.remote.SupabaseClient
import android.util.Log

class SupabaseRepository {

    private val client = SupabaseClient.client

    suspend fun updateSmartWatchStatus(
        numeroSerie: String,
        bateria: Int,
        conexion: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val updateData = buildJsonObject {
                put("bateria", bateria)
                put("conexion", conexion.lowercase())
                put("ultima_conexion", java.time.OffsetDateTime.now().toString())
            }
            
            client.postgrest["smartwatch"].update(updateData) {
                filter {
                    eq("numero_serie", numeroSerie)
                }
            }
            "success"
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "Error: ${e.message}")
            null
        }
    }
}
