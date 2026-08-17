package mx.utng.ich.safecare.data.local.entity

import java.util.UUID

/**
 * Entidad relacional para mapear los detalles de los perfiles monitoreados en las tablas de SQLite (Room).
 *  * Define el esquema, las restricciones y las relaciones de los datos del paciente en el almacenamiento persistente local.
 */
data class PerfilMonitoreadoEntity(
    val idPerfil: String = UUID.randomUUID().toString(),
    val nombre: String,
    val edad: Int,
    val fechaNacimiento: String? = null,
    val tipoPerfil: String,
    val foto: String? = null,
    val estadoActual: Boolean = true,
    val idCuidador: String
)

/**
 * Registro base que almacena los parÃ¡metros exactos de una zona de contenciÃ³n espacial (geocerca).
 *  * Define atributos vitales como las coordenadas del centro, el radio de tolerancia (en metros) y la identidad de la zona.
 */
data class ZonaSeguraEntity(
    val idZona: String = UUID.randomUUID().toString(),
    val nombre: String,
    val latitudCentro: Double,
    val longitudCentro: Double,
    val radioMetros: Double,
    val activa: Boolean = true,
    // Se conserva como perfil principal por compatibilidad con datos anteriores.
    val idPerfil: String,
    // Una zona puede estar asignada a varios perfiles monitoreados.
    val idPerfiles: Set<String> = setOf(idPerfil)
)

/**
 * Entidad de modelo relacional que representa fÃ­sicamente un reloj Wear OS asociado a un perfil.
 *  * Guarda parÃ¡metros tÃ©cnicos como la direcciÃ³n MAC, nombre del dispositivo y tokens de vinculaciÃ³n en la base de datos local.
 */
data class SmartwatchEntity(
    val idSmartwatch: String = UUID.randomUUID().toString(),
    val numeroSerie: String,
    val watchInstallationId: String? = null,
    val nombreDispositivo: String? = null,
    val modelo: String? = null,
    val dataLayerNodeId: String? = null,
    val bateria: Int = 100,
    val conexion: String = "online",
    val ultimaConexion: Long = System.currentTimeMillis(),
    val idPerfil: String? = null
)

/**
 * Entidad de base de datos que representa una alerta o incidente generado en el sistema.
 *  * Almacena informaciÃ³n crÃ­tica como el tipo de alerta, el nivel de gravedad, la fecha y hora exacta, y el perfil afectado.
 *  * Se utiliza en conjunciÃ³n con Room para garantizar la persistencia local de los datos.
 */
data class AlertaEntity(
    val idAlerta: String = UUID.randomUUID().toString(),
    val tipoAlerta: String,
    val descripcion: String,
    val fechaHora: Long = System.currentTimeMillis(),
    val estado: String = "ACTIVA",
    val idPerfil: String,
    val idUbicacion: String? = null
)

/**
 * Modelo que combina una alerta con el nombre del perfil que la generó.
 *
 * Utilizado en la capa de presentación para mostrar alertas con contexto
 * sin necesidad de consultar el perfil por separado.
 *
 * @property alerta Entidad de alerta con todos sus datos.
 * @property nombrePerfil Nombre de la persona monitoreada que generó la alerta, o `null`.
 */
data class AlertaConPerfil(
    val alerta: AlertaEntity,
    val nombrePerfil: String?
)

/**
 * Estructura de entidad que encapsula un punto geogrÃ¡fico (latitud, longitud) y un timestamp.
 *  * Forma el nÃºcleo de la traza de movimiento del usuario monitoreado para su posterior anÃ¡lisis.
 */
data class UbicacionEntity(
    val idUbicacion: String = UUID.randomUUID().toString(),
    val latitud: Double,
    val longitud: Double,
    val fechaHora: Long = System.currentTimeMillis(),
    val idSmartwatch: String
)

/**
 * Modelo que representa la última ubicación conocida de un perfil monitoreado.
 *
 * Combina los datos de ubicación con el identificador del perfil para
 * alimentar el mapa en tiempo real de la aplicación del cuidador.
 *
 * @property idPerfil Identificador del perfil monitoreado.
 * @property idUbicacion Identificador de la ubicación reportada.
 * @property latitud Coordenada de latitud de la última posición conocida.
 * @property longitud Coordenada de longitud de la última posición conocida.
 * @property fechaHora Marca de tiempo de la última ubicación registrada.
 * @property idSmartwatch Identificador del smartwatch que reportó la ubicación.
 */
data class LatestProfileLocation(
    val idPerfil: String,
    val idUbicacion: String,
    val latitud: Double,
    val longitud: Double,
    val fechaHora: Long,
    val idSmartwatch: String
)