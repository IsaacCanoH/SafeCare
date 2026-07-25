package mx.utng.ich.safecare.data.repository

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import mx.utng.ich.safecare.data.remote.SupabaseClient
import mx.utng.ich.safecare.data.local.entity.UsuarioEntity
import mx.utng.ich.safecare.data.local.entity.UbicacionEntity
import mx.utng.ich.safecare.data.local.entity.AlertaEntity
import android.util.Log
import java.util.UUID
import java.util.Locale
import java.text.SimpleDateFormat

class SupabaseRepository {

    private val client = SupabaseClient.client

    suspend fun saveLocation(location: UbicacionEntity): Boolean = withContext(Dispatchers.IO) {
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
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "Error syncing location ${location.idUbicacion}", e)
            false
        }
    }

    suspend fun saveAlert(alert: AlertaEntity): Boolean = withContext(Dispatchers.IO) {
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
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "Error syncing alert ${alert.idAlerta}", e)
            false
        }
    }

    suspend fun saveUser(usuario: UsuarioEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val userJson = buildJsonObject {
                put("idUsuario", usuario.idUsuario)
                put("nombre", usuario.nombre)
                put("correo", usuario.correo)
                put("contrasena", usuario.contrasena)
                put("telefono", usuario.telefono ?: "")
                put("estado", usuario.estado)
            }
            
            client.postgrest["Usuario"].insert(userJson)
            true
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "Error saving user: ${e.message}")
            false
        }
    }

    suspend fun createProfile(
        nombre: String, 
        edad: Int, 
        tipo: String, 
        idCuidador: String,
        numeroSerie: String? = null,
        fechaNacimiento: String? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            // Mapeamos el tipo amigable al valor EXACTO de tu imagen
            val tipoMapeado = when(tipo) {
                "Menor de edad" -> "menor" 
                "Adulto mayor" -> "adulto_mayor"
                "Cuidador" -> "cuidador"
                else -> "menor" // Valor por defecto seguro
            }

            val idPerfil = UUID.randomUUID().toString()
            val profileJson = buildJsonObject {
                put("idPerfil", idPerfil)
                put("nombre", nombre)
                put("edad", edad)
                formatBirthDate(fechaNacimiento)?.let { put("fechaNacimiento", it) }
                put("tipoPerfil", tipoMapeado)
                put("idCuidador", idCuidador)
                put("estadoActual", true)
            }
            client.postgrest["PerfilMonitoreado"].insert(profileJson)
            Log.d("SupabaseRepo", "Profile inserted successfully in Supabase: $idPerfil with type $tipoMapeado")
            
            // Si tiene smartwatch, lo vinculamos
            numeroSerie?.let {
                val watchJson = buildJsonObject {
                    put("numeroSerie", it)
                    put("idPerfil", idPerfil)
                    put("bateria", 100)
                    put("conexion", "online")
                }
                client.postgrest["SmartWatch"].insert(watchJson)
                Log.d("SupabaseRepo", "Smartwatch linked successfully: $it")
            }
            
            idPerfil
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "CRITICAL ERROR creating profile: ${e.message}", e)
            null
        }
    }

    suspend fun updateProfile(
        idPerfil: String,
        nombre: String,
        edad: Int,
        fechaNacimiento: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("SupabaseRepo", "Attempting update for ID: $idPerfil with name: $nombre")
            
            val fechaFormateada = formatBirthDate(fechaNacimiento)

            val updateData = buildJsonObject {
                put("nombre", nombre)
                put("edad", edad)
                // Solo enviamos la fecha si es válida, de lo contrario no la incluimos 
                // para evitar el error de sintaxis en Supabase
                fechaFormateada?.let { put("fechaNacimiento", it) }
            }

            client.postgrest["PerfilMonitoreado"].update(updateData) {
                filter {
                    eq("idPerfil", idPerfil)
                }
            }
            Log.d("SupabaseRepo", "Supabase update request sent successfully")
            true
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "Error updating profile in Supabase: ${e.message}", e)
            false
        }
    }

    suspend fun deleteProfile(idPerfil: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Primero intentamos borrar el smartwatch vinculado si existe (dependiendo de tus FK)
            try {
                client.postgrest["SmartWatch"].delete {
                    filter { eq("idPerfil", idPerfil) }
                }
            } catch (e: Exception) { /* Ignorable si no hay reloj */ }

            client.postgrest["PerfilMonitoreado"].delete {
                filter {
                    eq("idPerfil", idPerfil)
                }
            }
            true
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "Error deleting profile: ${e.message}")
            false
        }
    }

    suspend fun createSafeZone(
        nombre: String,
        lat: Double,
        lng: Double,
        radio: Double,
        idPerfil: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val zoneJson = buildJsonObject {
                put("nombre", nombre)
                put("latitudCentro", lat)
                put("longitudCentro", lng)
                put("radioMetros", radio)
                put("idPerfil", idPerfil)
                put("activa", true)
            }
            client.postgrest["ZonaSegura"].insert(zoneJson)
            true
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "Error creating zone: ${e.message}")
            false
        }
    }

    suspend fun updateSafeZone(
        idZona: String,
        nombre: String,
        lat: Double,
        lng: Double,
        radio: Double,
        activa: Boolean? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val zoneJson = buildJsonObject {
                put("nombre", nombre)
                put("latitudCentro", lat)
                put("longitudCentro", lng)
                put("radioMetros", radio)
                activa?.let { put("activa", it) }
            }
            client.postgrest["ZonaSegura"].update(zoneJson) {
                filter {
                    eq("idZona", idZona)
                }
            }
            true
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "Error updating zone: ${e.message}")
            false
        }
    }

    suspend fun toggleSafeZoneStatus(idZona: String, activa: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val updateData = buildJsonObject {
                put("activa", activa)
            }
            client.postgrest["ZonaSegura"].update(updateData) {
                filter {
                    eq("idZona", idZona)
                }
            }
            true
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "Error toggling zone: ${e.message}")
            false
        }
    }

    private fun formatBirthDate(value: String?): String? {
        if (value.isNullOrBlank()) return null

        val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
            isLenient = false
        }
        val supportedFormats = listOf("dd/MM/yyyy", "yyyy-MM-dd")

        return supportedFormats.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.ROOT).apply {
                    isLenient = false
                }.parse(value)?.let(outputFormat::format)
            }.getOrNull()
        }
    }
}
