package mx.utng.ich.safecare.data.local.entity

import java.util.UUID

/**
 * Modelo de datos para un perfil de persona monitoreada en la aplicación móvil.
 *
 * Representa la información básica de un menor de edad o adulto mayor
 * que está siendo supervisado por un cuidador a través de SafeCare.
 *
 * @property idPerfil Identificador único del perfil, generado automáticamente.
 * @property nombre Nombre completo de la persona monitoreada.
 * @property edad Edad actual de la persona.
 * @property fechaNacimiento Fecha de nacimiento en formato texto, o `null` si no se proporcionó.
 * @property tipoPerfil Tipo de perfil: "menor", "adulto_mayor" o "cuidador".
 * @property foto URL de la foto del perfil, o `null` si no tiene.
 * @property estadoActual Estado activo del perfil; `true` si está siendo monitoreado.
 * @property idCuidador Identificador del cuidador responsable de este perfil.
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
 * Modelo de datos para una zona segura definida por el cuidador.
 *
 * Define un área circular geográfica donde la persona monitoreada debe permanecer.
 * Si la persona sale de esta zona, se genera una alerta de seguridad.
 *
 * @property idZona Identificador único de la zona segura.
 * @property nombre Nombre descriptivo de la zona (ej. "Casa", "Escuela").
 * @property latitudCentro Latitud del centro de la zona segura.
 * @property longitudCentro Longitud del centro de la zona segura.
 * @property radioMetros Radio de la zona en metros.
 * @property activa Indica si el monitoreo de esta zona está habilitado.
 * @property idPerfil Perfil principal asignado a la zona, conservado por compatibilidad.
 * @property idPerfiles Conjunto de identificadores de perfiles asignados a esta zona.
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
 * Modelo de datos de un smartwatch vinculado a un perfil monitoreado.
 *
 * Almacena la información del dispositivo Wear OS que lleva la persona supervisada,
 * incluyendo su estado de batería, conexión e identificadores de vinculación.
 *
 * @property idSmartwatch Identificador único interno del smartwatch.
 * @property numeroSerie Número de serie del reloj utilizado como identificador principal.
 * @property watchInstallationId Identificador de instalación generado por el reloj.
 * @property nombreDispositivo Nombre visible del dispositivo Wear OS.
 * @property modelo Modelo del hardware del reloj.
 * @property dataLayerNodeId Identificador del nodo en la Wearable Data Layer.
 * @property bateria Nivel de batería actual del reloj (0-100).
 * @property conexion Estado de conexión: "online" u "offline".
 * @property ultimaConexion Marca de tiempo de la última conexión registrada.
 * @property idPerfil Identificador del perfil monitoreado vinculado, o `null` si no está vinculado.
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
 * Modelo de datos de una alerta de seguridad generada por el sistema.
 *
 * Representa un evento de emergencia como un SOS o una salida de zona segura,
 * asociado a un perfil monitoreado y opcionalmente a una ubicación.
 *
 * @property idAlerta Identificador único de la alerta.
 * @property tipoAlerta Tipo de evento: "SOS", "FUERA_DE_ZONA" u otro clasificador.
 * @property descripcion Descripción legible del evento de alerta.
 * @property fechaHora Marca de tiempo en milisegundos del momento de la alerta.
 * @property estado Estado de la alerta: "ACTIVA" o "ATENDIDA".
 * @property idPerfil Identificador del perfil monitoreado que generó la alerta.
 * @property idUbicacion Identificador de la ubicación asociada, o `null` si no aplica.
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
 * Modelo de datos para una ubicación geográfica registrada por el smartwatch.
 *
 * Almacena las coordenadas GPS capturadas por el dispositivo Wear OS
 * junto con la marca temporal y el identificador del reloj que las reportó.
 *
 * @property idUbicacion Identificador único de la ubicación.
 * @property latitud Coordenada de latitud de la ubicación.
 * @property longitud Coordenada de longitud de la ubicación.
 * @property fechaHora Marca de tiempo en milisegundos de la captura.
 * @property idSmartwatch Identificador del smartwatch que reportó esta ubicación.
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
