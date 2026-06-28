package mx.utng.ich.safecare.wearable.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TipoPerfil {
    @SerialName("menor") MENOR,
    @SerialName("adulto_mayor") ADULTO_MAYOR,
    @SerialName("cuidador") CUIDADOR
}

@Serializable
enum class EstadoAlerta {
    @SerialName("activa") ACTIVA,
    @SerialName("atendida") ATENDIDA,
    @SerialName("falsa_alarma") FALSA_ALARMA
}

@Serializable
enum class TipoAlerta {
    @SerialName("sos") SOS,
    @SerialName("zona_segura") ZONA_SEGURA,
    @SerialName("bateria_baja") BATERIA_BAJA,
    @SerialName("sin_conexion") SIN_CONEXION
}

@Serializable
enum class TipoConexion {
    @SerialName("online") ONLINE,
    @SerialName("offline") OFFLINE
}

@Serializable
data class SmartWatch(
    @SerialName("id_smartwatch") val id: String? = null,
    @SerialName("numero_serie") val numeroSerie: String,
    val bateria: Int,
    val conexion: TipoConexion,
    @SerialName("ultima_conexion") val ultimaConexion: String? = null,
    @SerialName("id_perfil") val idPerfil: String? = null
)

@Serializable
data class Ubicacion(
    @SerialName("id_ubicacion") val id: String? = null,
    val latitud: Double,
    val longitud: Double,
    @SerialName("fecha_hora") val fechaHora: String? = null,
    @SerialName("id_smartwatch") val idSmartwatch: String
)

@Serializable
data class Alerta(
    @SerialName("id_alerta") val id: String? = null,
    @SerialName("tipo_alerta") val tipoAlerta: TipoAlerta,
    val descripcion: String,
    @SerialName("fecha_hora") val fechaHora: String? = null,
    val estado: EstadoAlerta = EstadoAlerta.ACTIVA,
    @SerialName("id_perfil") val idPerfil: String,
    @SerialName("id_ubicacion") val idUbicacion: String? = null
)

@Serializable
data class ZonaSegura(
    @SerialName("id_zona") val id: String,
    val nombre: String,
    @SerialName("latitud_centro") val latitudCentro: Double,
    @SerialName("longitud_centro") val longitudCentro: Double,
    @SerialName("radio_metros") val radioMetros: Double,
    val activa: Boolean,
    @SerialName("id_perfil") val idPerfil: String
)
