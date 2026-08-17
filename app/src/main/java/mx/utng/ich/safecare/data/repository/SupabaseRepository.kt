package mx.utng.ich.safecare.data.repository

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import mx.utng.ich.safecare.data.remote.SupabaseClient
import mx.utng.ich.safecare.data.local.entity.UsuarioEntity
import mx.utng.ich.safecare.data.local.entity.UbicacionEntity
import mx.utng.ich.safecare.data.local.entity.AlertaEntity
import mx.utng.ich.safecare.data.local.entity.LatestProfileLocation
import mx.utng.ich.safecare.data.local.entity.PerfilMonitoreadoEntity
import mx.utng.ich.safecare.data.local.entity.ZonaSeguraEntity
import android.util.Log
import java.util.UUID
import java.util.Locale
import java.text.SimpleDateFormat
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Repositorio de alto nivel que abstrae y consolida las llamadas a la API REST y Realtime de Supabase.
 *  * Proporciona un punto Ãºnico de verdad para que el resto de la aplicaciÃ³n solicite datos a la nube sin acoplarse a la librerÃ­a de red.
 */
class SupabaseRepository {

    private val client = SupabaseClient.client

    /**
     * Guarda o actualiza la ubicación recibida en Supabase.
     *
     * @param location Entidad de ubicación con coordenadas, timestamp e identificador del smartwatch.
     * @return `true` si la operación fue exitosa, `false` si ocurrió un error.
     */
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

    /**
     * Guarda o actualiza una alerta en Supabase.
     *
     * @param alert Entidad de alerta con tipo, descripción, estado y perfil asociado.
     * @return `true` si la operación fue exitosa, `false` si ocurrió un error.
     */
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

    /**
     * Marca una alerta como atendida para todos los dispositivos del cuidador.
     *
     * @param alertId Identificador único de la alerta a reconocer.
     * @return `true` si la actualización fue exitosa, `false` si ocurrió un error.
     */
    suspend fun acknowledgeAlert(alertId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val updateData = buildJsonObject {
                put("estado", "ATENDIDA")
            }
            client.postgrest["Alerta"].update(updateData) {
                filter { eq("idAlerta", alertId) }
            }
            true
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "Error acknowledging alert $alertId", e)
            false
        }
    }

    /**
     * Registra los datos del cuidador en la base remota.
     *
     * @param usuario Entidad con los datos del cuidador a registrar.
     * @return `true` si la inserción fue exitosa, `false` si ocurrió un error.
     */
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

    /**
     * Crea un perfil monitoreado y vincula su reloj si existe.
     *
     * Mapea el tipo amigable de perfil al valor almacenado en la base de datos
     * y opcionalmente crea el registro del smartwatch asociado.
     *
     * @param nombre Nombre completo de la persona monitoreada.
     * @param edad Edad de la persona.
     * @param tipo Tipo de perfil en formato amigable ("Menor de edad", "Adulto mayor", "Cuidador").
     * @param idCuidador Identificador del cuidador responsable.
     * @param numeroSerie Número de serie del smartwatch a vincular, o `null` si no tiene.
     * @param fechaNacimiento Fecha de nacimiento en formato texto, o `null`.
     * @return Identificador del perfil creado, o `null` si ocurrió un error.
     */
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

    /**
     * Actualiza los datos editables de un perfil monitoreado.
     *
     * @param idPerfil Identificador del perfil a actualizar.
     * @param nombre Nuevo nombre de la persona monitoreada.
     * @param edad Nueva edad de la persona.
     * @param fechaNacimiento Nueva fecha de nacimiento en formato texto, o `null`.
     * @return `true` si la actualización fue exitosa, `false` si ocurrió un error.
     */
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

    /**
     * Elimina un perfil y el reloj que tenga vinculado.
     *
     * Primero intenta eliminar el smartwatch asociado y luego el perfil.
     *
     * @param idPerfil Identificador del perfil a eliminar.
     * @return `true` si la eliminación fue exitosa, `false` si ocurrió un error.
     */
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

    /**
     * Crea una zona segura y sus relaciones con los perfiles seleccionados de forma atómica.
     *
     * Utiliza una función RPC de Supabase para garantizar la atomicidad de la operación.
     *
     * @param idZona Identificador único para la nueva zona segura.
     * @param nombre Nombre descriptivo de la zona.
     * @param lat Latitud del centro de la zona.
     * @param lng Longitud del centro de la zona.
     * @param radio Radio de la zona en metros.
     * @param profileIds Lista de identificadores de perfiles a asignar a la zona.
     * @return `true` si la creación fue exitosa, `false` si ocurrió un error.
     */
    suspend fun createSafeZone(
        idZona: String,
        nombre: String,
        lat: Double,
        lng: Double,
        radio: Double,
        profileIds: List<String>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            client.postgrest.rpc(
                "create_safe_zone_with_profiles",
                safeZoneMutationParameters(idZona, nombre, lat, lng, radio, profileIds)
            )
            true
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "Error creating zone: ${e.message}")
            false
        }
    }

    /**
     * Actualiza la ubicación, radio o estado de una zona segura existente.
     *
     * @param idZona Identificador de la zona a actualizar.
     * @param nombre Nuevo nombre de la zona.
     * @param lat Nueva latitud del centro.
     * @param lng Nueva longitud del centro.
     * @param radio Nuevo radio en metros.
     * @param profileIds Lista actualizada de perfiles asignados a la zona.
     * @return `true` si la actualización fue exitosa, `false` si ocurrió un error.
     */
    suspend fun updateSafeZone(
        idZona: String,
        nombre: String,
        lat: Double,
        lng: Double,
        radio: Double,
        profileIds: List<String>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            client.postgrest.rpc(
                "update_safe_zone_with_profiles",
                safeZoneMutationParameters(idZona, nombre, lat, lng, radio, profileIds)
            )
            true
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "Error updating zone: ${e.message}")
            false
        }
    }

    /**
     * Activa o desactiva el monitoreo de una zona segura.
     *
     * @param idZona Identificador de la zona a modificar.
     * @param activa `true` para activar el monitoreo, `false` para desactivarlo.
     * @return `true` si la operación fue exitosa, `false` si ocurrió un error.
     */
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

    /**
     * Sincroniza la batería y conexión actual del smartwatch en Supabase.
     *
     * @param numeroSerie Número de serie del reloj a actualizar.
     * @param bateria Nivel de batería actual (0-100).
     * @param conexion Estado de conexión actual ("online" u "offline").
     * @param ultimaConexion Marca de tiempo de la última conexión registrada.
     * @return `true` si la sincronización fue exitosa, `false` si ocurrió un error.
     */
    suspend fun updateSmartWatchStatus(
        numeroSerie: String,
        bateria: Int,
        conexion: String,
        ultimaConexion: Long
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val updateData = buildJsonObject {
                put("bateria", bateria)
                put("conexion", conexion.lowercase())
                put("ultimaConexion", ultimaConexion)
            }
            client.postgrest["SmartWatch"].update(updateData) {
                filter { eq("numeroSerie", numeroSerie) }
            }
            true
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "Error syncing smartwatch status $numeroSerie", e)
            false
        }
    }

    /**
     * Obtiene los perfiles asociados al cuidador autenticado.
     *
     * @param caregiverId Identificador del cuidador en Supabase Auth.
     * @return Lista de [PerfilMonitoreadoEntity] del cuidador.
     */
    suspend fun fetchProfilesForCaregiver(caregiverId: String): List<PerfilMonitoreadoEntity> =
        withContext(Dispatchers.IO) {
            client.postgrest["PerfilMonitoreado"].select {
                filter { eq("idCuidador", caregiverId) }
            }.decodeList<ProfileRow>().map { row ->
                PerfilMonitoreadoEntity(
                    idPerfil = row.id,
                    nombre = row.nombre,
                    edad = row.edad,
                    fechaNacimiento = row.fechaNacimiento,
                    tipoPerfil = row.tipoPerfil,
                    foto = row.foto,
                    estadoActual = row.estadoActual,
                    idCuidador = row.idCuidador
                )
            }
        }

    /**
     * Obtiene cada zona una vez, con todos los perfiles del cuidador a los que está asignada.
     *
     * Consulta la tabla intermedia `ZonaSeguraPerfil` para construir la relación
     * muchos a muchos entre zonas y perfiles monitoreados.
     *
     * @param caregiverId Identificador del cuidador en Supabase Auth.
     * @return Lista de [ZonaSeguraEntity] con los perfiles asignados a cada zona.
     */
    suspend fun fetchSafeZonesForCaregiver(caregiverId: String): List<ZonaSeguraEntity> =
        withContext(Dispatchers.IO) {
            val profileIds = client.postgrest["PerfilMonitoreado"].select(Columns.list("idPerfil")) {
                filter { eq("idCuidador", caregiverId) }
            }.decodeList<ProfileIdRow>().map(ProfileIdRow::id)
            if (profileIds.isEmpty()) return@withContext emptyList()

            val assignments = client.postgrest["ZonaSeguraPerfil"].select {
                filter { isIn("idPerfil", profileIds) }
            }.decodeList<SafeZoneProfileRow>()
            if (assignments.isEmpty()) return@withContext emptyList()

            val profileIdsByZone = assignments
                .groupBy(SafeZoneProfileRow::zoneId)
                .mapValues { (_, values) -> values.map(SafeZoneProfileRow::profileId).toSet() }

            client.postgrest["ZonaSegura"].select {
                filter { isIn("idZona", profileIdsByZone.keys.toList()) }
            }.decodeList<SafeZoneRow>().map { row ->
                ZonaSeguraEntity(
                    idZona = row.id,
                    nombre = row.nombre,
                    latitudCentro = row.latitudCentro,
                    longitudCentro = row.longitudCentro,
                    radioMetros = row.radioMetros,
                    activa = row.activa,
                    idPerfil = row.idPerfil,
                    idPerfiles = profileIdsByZone[row.id].orEmpty()
                )
            }
        }

    /**
     * Obtiene las alertas generadas por los perfiles del cuidador.
     *
     * @param caregiverId Identificador del cuidador en Supabase Auth.
     * @return Lista de [AlertaEntity] asociadas a los perfiles del cuidador.
     */
    suspend fun fetchAlertsForCaregiver(caregiverId: String): List<AlertaEntity> =
        withContext(Dispatchers.IO) {
            val profileIds = client.postgrest["PerfilMonitoreado"].select(Columns.list("idPerfil")) {
                filter { eq("idCuidador", caregiverId) }
            }.decodeList<ProfileIdRow>().map(ProfileIdRow::id)
            if (profileIds.isEmpty()) return@withContext emptyList()

            client.postgrest["Alerta"].select {
                filter { isIn("idPerfil", profileIds) }
            }.decodeList<AlertRow>().map { row ->
                AlertaEntity(
                    idAlerta = row.id,
                    tipoAlerta = row.tipoAlerta,
                    descripcion = row.descripcion,
                    fechaHora = row.fechaHora,
                    estado = row.estado,
                    idPerfil = row.idPerfil,
                    idUbicacion = row.idUbicacion
                )
            }
        }

    /**
     * Obtiene la última ubicación disponible de cada perfil monitoreado.
     *
     * Consulta los smartwatches vinculados y obtiene la ubicación más reciente
     * de cada uno para alimentar el mapa en tiempo real.
     *
     * @param caregiverId Identificador del cuidador en Supabase Auth.
     * @return Lista de [LatestProfileLocation] con la última posición de cada perfil.
     */
    suspend fun fetchLatestLocationsForCaregiver(caregiverId: String): List<LatestProfileLocation> =
        withContext(Dispatchers.IO) {
            val profiles = client.postgrest["PerfilMonitoreado"].select(Columns.list("idPerfil")) {
                filter { eq("idCuidador", caregiverId) }
            }.decodeList<ProfileIdRow>()
            val profileIds = profiles.map(ProfileIdRow::id)
            if (profileIds.isEmpty()) return@withContext emptyList()

            val watches = client.postgrest["SmartWatch"].select {
                filter { isIn("idPerfil", profileIds) }
            }.decodeList<WatchRow>()
            val watchIds = watches.flatMap { listOfNotNull(it.id, it.numeroSerie) }.distinct()
            if (watchIds.isEmpty()) return@withContext emptyList()

            val latestByWatch = coroutineScope {
                watchIds.map { watchId ->
                    async {
                        watchId to client.postgrest["Ubicacion"].select {
                            filter { eq("idSmartwatch", watchId) }
                            order("fechaHora", Order.DESCENDING)
                            limit(1)
                        }.decodeList<LocationRow>().firstOrNull()
                    }
                }.map { it.await() }.toMap()
            }

            watches.mapNotNull { watch ->
                val location = latestByWatch[watch.id] ?: watch.numeroSerie?.let(latestByWatch::get)
                    ?: return@mapNotNull null
                val profileId = watch.idPerfil ?: return@mapNotNull null
                LatestProfileLocation(
                    idPerfil = profileId,
                    idUbicacion = location.id,
                    latitud = location.latitud,
                    longitud = location.longitud,
                    fechaHora = location.fechaHora,
                    idSmartwatch = location.idSmartwatch
                )
            }
        }

    /**
     * Busca el número de serie del reloj vinculado al perfil.
     *
     * @param profileId Identificador del perfil monitoreado.
     * @return Número de serie del smartwatch, o `null` si no tiene uno vinculado.
     */
    suspend fun fetchWatchSerial(profileId: String): String? = withContext(Dispatchers.IO) {
        client.postgrest["SmartWatch"].select(Columns.list("numeroSerie")) {
            filter { eq("idPerfil", profileId) }
        }.decodeList<WatchSerialRow>().firstOrNull()?.numeroSerie
    }

    /**
     * Convierte una fecha válida al formato requerido por Supabase (yyyy-MM-dd).
     *
     * Soporta los formatos de entrada "dd/MM/yyyy" y "yyyy-MM-dd".
     *
     * @param value Cadena con la fecha a formatear, o `null`.
     * @return Fecha en formato "yyyy-MM-dd", o `null` si el valor es inválido o nulo.
     */
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

    /** Fila auxiliar para decodificar el identificador de perfil desde Supabase. */
    @Serializable
    private data class ProfileIdRow(@SerialName("idPerfil") val id: String)

    /** Fila auxiliar para decodificar un perfil monitoreado completo desde Supabase. */
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

    /** Fila auxiliar para decodificar una zona segura desde Supabase. */
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

    /** Fila auxiliar para decodificar la relación zona-perfil desde Supabase. */
    @Serializable
    private data class SafeZoneProfileRow(
        @SerialName("idZona") val zoneId: String,
        @SerialName("idPerfil") val profileId: String
    )

    /** Fila auxiliar para decodificar una alerta desde Supabase. */
    @Serializable
    private data class AlertRow(
        @SerialName("idAlerta") val id: String,
        @SerialName("tipoAlerta") val tipoAlerta: String,
        val descripcion: String = "",
        @SerialName("fechaHora") val fechaHora: Long,
        val estado: String = "ACTIVA",
        @SerialName("idPerfil") val idPerfil: String,
        @SerialName("idUbicacion") val idUbicacion: String? = null
    )

    /** Fila auxiliar para decodificar un smartwatch desde Supabase. */
    @Serializable
    private data class WatchRow(
        @SerialName("idSmartwatch") val id: String? = null,
        @SerialName("numeroSerie") val numeroSerie: String? = null,
        @SerialName("idPerfil") val idPerfil: String? = null
    )

    /** Fila auxiliar para obtener solo el número de serie de un smartwatch. */
    @Serializable
    private data class WatchSerialRow(@SerialName("numeroSerie") val numeroSerie: String? = null)

    /** Fila auxiliar para decodificar una ubicación desde Supabase. */
    @Serializable
    private data class LocationRow(
        @SerialName("idUbicacion") val id: String,
        val latitud: Double,
        val longitud: Double,
        @SerialName("fechaHora") val fechaHora: Long,
        @SerialName("idSmartwatch") val idSmartwatch: String
    )

    /**
     * Construye los parámetros JSON para las funciones RPC de creación y actualización de zonas seguras.
     *
     * @param idZona Identificador de la zona.
     * @param nombre Nombre de la zona.
     * @param latitud Latitud del centro de la zona.
     * @param longitud Longitud del centro de la zona.
     * @param radio Radio en metros.
     * @param profileIds Lista de identificadores de perfiles a vincular.
     * @return Objeto JSON con los parámetros para la función RPC.
     */
    private fun safeZoneMutationParameters(
        idZona: String,
        nombre: String,
        latitud: Double,
        longitud: Double,
        radio: Double,
        profileIds: List<String>
    ) = buildJsonObject {
        put("p_id_zona", idZona)
        put("p_nombre", nombre)
        put("p_latitud", latitud)
        put("p_longitud", longitud)
        put("p_radio", radio)
        put("p_id_perfiles", buildJsonArray {
            profileIds.distinct().forEach { add(JsonPrimitive(it)) }
        })
    }
}