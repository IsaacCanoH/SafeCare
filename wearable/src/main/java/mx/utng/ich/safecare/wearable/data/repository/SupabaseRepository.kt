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
                client.postgrest["Ubicacion"].insert(locationData)
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
}
