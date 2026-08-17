package mx.utng.ich.safecare.data.datalayer

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.utng.ich.safecare.data.local.entity.PerfilMonitoreadoEntity
import mx.utng.ich.safecare.data.local.entity.AlertaEntity
import mx.utng.ich.safecare.data.local.entity.ZonaSeguraEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Representa un dispositivo Wear OS disponible para vincular.
 *
 * @property nodeId Identificador del nodo en la red Wearable Data Layer.
 * @property watchInstallationId Identificador único de instalación del reloj.
 * @property displayName Nombre visible del dispositivo.
 * @property model Modelo del hardware del reloj.
 * @property batteryLevel Nivel de batería reportado por el reloj (0-100), o -1 si no está disponible.
 * @property isNearby Indica si el dispositivo se encuentra cerca del teléfono.
 */
data class AvailableWearDevice(
    val nodeId: String,
    val watchInstallationId: String,
    val displayName: String,
    val model: String,
    val batteryLevel: Int,
    val isNearby: Boolean
)

/**
 * Repositorio para la comunicación con dispositivos Wear OS mediante la Wearable Data Layer.
 *
 * Permite descubrir relojes cercanos, vincular y desvincular perfiles monitoreados,
 * sincronizar zonas seguras y enviar alertas personalizadas al smartwatch.
 *
 * @param context Contexto de Android utilizado para obtener los clientes de Wearable.
 */
class WearDataLayerRepository(context: Context) {
    private val appContext = context.applicationContext
    private val capabilityClient = Wearable.getCapabilityClient(appContext)
    private val messageClient = Wearable.getMessageClient(appContext)

    /**
     * Detecta relojes cercanos que pueden vincularse a un perfil.
     *
     * Consulta la capacidad [CAPABILITY_WATCH] para encontrar nodos alcanzables
     * y solicita a cada uno su información de dispositivo.
     *
     * @return Lista de [AvailableWearDevice] ordenada por cercanía y nombre.
     */
    suspend fun discoverAvailableWatches(): List<AvailableWearDevice> =
        withContext(Dispatchers.IO) {
            val capability = Tasks.await(
                capabilityClient.getCapability(
                    CAPABILITY_WATCH,
                    CapabilityClient.FILTER_REACHABLE
                ),
                RPC_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
            )

            capability.nodes.mapNotNull { node ->
                runCatching {
                    val response = Tasks.await(
                        messageClient.sendRequest(
                            node.id,
                            PATH_DEVICE_INFO,
                            ByteArray(0)
                        ),
                        RPC_TIMEOUT_SECONDS,
                        TimeUnit.SECONDS
                    )
                    val data = JSONObject(response.toString(Charsets.UTF_8))
                    AvailableWearDevice(
                        nodeId = node.id,
                        watchInstallationId = data.getString(KEY_WATCH_ID),
                        displayName = data.optString(KEY_DISPLAY_NAME, node.displayName),
                        model = data.optString(KEY_MODEL, node.displayName),
                        batteryLevel = data.optInt(KEY_BATTERY, -1),
                        isNearby = node.isNearby
                    )
                }.getOrNull()
            }.sortedWith(
                compareByDescending<AvailableWearDevice> { it.isNearby }
                    .thenBy { it.displayName }
            )
        }

    /**
     * Envía al reloj los datos del perfil que se va a monitorear.
     *
     * @param device Dispositivo Wear OS destino de la vinculación.
     * @param profile Perfil monitoreado que se vinculará al reloj.
     * @return [Result] exitoso si el reloj acepta, o con error si rechaza la vinculación.
     */
    suspend fun linkProfile(
        device: AvailableWearDevice,
        profile: PerfilMonitoreadoEntity
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val request = JSONObject()
                .put(KEY_WATCH_ID, device.watchInstallationId)
                .put(KEY_PROFILE_ID, profile.idPerfil)
                .put(KEY_NAME, profile.nombre)
                .put(KEY_AGE, profile.edad)
                .put(KEY_BIRTH_DATE, profile.fechaNacimiento)
                .put(KEY_PROFILE_TYPE, profile.tipoPerfil)
                .put(KEY_PHOTO, profile.foto)
                .put(KEY_CAREGIVER_ID, profile.idCuidador)

            val response = sendRequest(device.nodeId, PATH_LINK_PROFILE, request)
            check(response.optBoolean(KEY_SUCCESS)) {
                response.optString(KEY_ERROR, "El reloj rechazó la vinculación")
            }
        }
    }

    /**
     * Sincroniza las zonas seguras activas con el reloj.
     *
     * @param nodeId Identificador del nodo Wear OS destino.
     * @param profileId Identificador del perfil monitoreado al que pertenecen las zonas.
     * @param zones Lista de zonas seguras a sincronizar en el reloj.
     * @return [Result] exitoso si el reloj acepta las zonas, o con error en caso contrario.
     */
    suspend fun syncZones(
        nodeId: String,
        profileId: String,
        zones: List<ZonaSeguraEntity>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val zoneArray = JSONArray()
            zones.forEach { zone ->
                zoneArray.put(
                    JSONObject()
                        .put(KEY_ZONE_ID, zone.idZona)
                        .put(KEY_NAME, zone.nombre)
                        .put(KEY_LATITUDE, zone.latitudCentro)
                        .put(KEY_LONGITUDE, zone.longitudCentro)
                        .put(KEY_RADIUS, zone.radioMetros)
                        .put(KEY_ACTIVE, zone.activa)
                )
            }
            val request = JSONObject()
                .put(KEY_PROFILE_ID, profileId)
                .put(KEY_ZONES, zoneArray)
            val response = sendRequest(nodeId, PATH_SYNC_ZONES, request)
            check(response.optBoolean(KEY_SUCCESS)) {
                response.optString(KEY_ERROR, "El reloj rechazó las zonas")
            }
        }
    }

    /**
     * Solicita al reloj eliminar el perfil vinculado.
     *
     * @param nodeId Identificador del nodo Wear OS destino.
     * @param profileId Identificador del perfil a desvincular.
     * @return [Result] exitoso si el reloj acepta la desvinculación.
     */
    suspend fun unlinkProfile(nodeId: String, profileId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = sendRequest(
                    nodeId,
                    PATH_UNLINK_PROFILE,
                    JSONObject().put(KEY_PROFILE_ID, profileId)
                )
                check(response.optBoolean(KEY_SUCCESS)) {
                    response.optString(KEY_ERROR, "El reloj rechazó la desvinculación")
                }
            }
        }

    /**
     * Envía una alerta personalizada al reloj conectado.
     *
     * @param nodeId Identificador del nodo Wear OS destino.
     * @param alert Entidad de alerta con tipo, descripción y datos del perfil asociado.
     * @return [Result] exitoso si el reloj acepta la alerta.
     */
    suspend fun sendCustomAlert(
        nodeId: String,
        alert: AlertaEntity
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val request = JSONObject()
                .put(KEY_ALERT_ID, alert.idAlerta)
                .put(KEY_PROFILE_ID, alert.idPerfil)
                .put(KEY_ALERT_TYPE, alert.tipoAlerta)
                .put(KEY_DESCRIPTION, alert.descripcion)
                .put(KEY_TIMESTAMP, alert.fechaHora)
                .put(KEY_STATE, alert.estado)
            val response = sendRequest(nodeId, PATH_CUSTOM_ALERT, request)
            check(response.optBoolean(KEY_SUCCESS)) {
                response.optString(KEY_ERROR, "El reloj rechazó la alerta")
            }
        }
    }

    /**
     * Ejecuta una petición con respuesta hacia un nodo Wear OS.
     *
     * @param nodeId Identificador del nodo destino.
     * @param path Ruta del mensaje en el protocolo de la Data Layer.
     * @param payload Objeto JSON con los datos de la solicitud.
     * @return [JSONObject] con la respuesta del nodo.
     */
    private fun sendRequest(nodeId: String, path: String, payload: JSONObject): JSONObject {
        val response = Tasks.await(
            messageClient.sendRequest(
                nodeId,
                path,
                payload.toString().toByteArray(Charsets.UTF_8)
            ),
            RPC_TIMEOUT_SECONDS,
            TimeUnit.SECONDS
        )
        return JSONObject(response.toString(Charsets.UTF_8))
    }

    companion object {
        const val CAPABILITY_WATCH = "safecare_watch"
        const val PATH_DEVICE_INFO = "/safecare/device-info"
        const val PATH_LINK_PROFILE = "/safecare/link-profile"
        const val PATH_SYNC_ZONES = "/safecare/sync-zones"
        const val PATH_UNLINK_PROFILE = "/safecare/unlink-profile"
        const val PATH_CUSTOM_ALERT = "/safecare/custom-alert"

        const val KEY_SUCCESS = "success"
        const val KEY_ERROR = "error"
        const val KEY_WATCH_ID = "watchInstallationId"
        const val KEY_DISPLAY_NAME = "displayName"
        const val KEY_MODEL = "model"
        const val KEY_BATTERY = "battery"
        const val KEY_PROFILE_ID = "profileId"
        const val KEY_NAME = "name"
        const val KEY_AGE = "age"
        const val KEY_BIRTH_DATE = "birthDate"
        const val KEY_PROFILE_TYPE = "profileType"
        const val KEY_PHOTO = "photo"
        const val KEY_CAREGIVER_ID = "caregiverId"
        const val KEY_ZONE_ID = "zoneId"
        const val KEY_LATITUDE = "latitude"
        const val KEY_LONGITUDE = "longitude"
        const val KEY_RADIUS = "radius"
        const val KEY_ACTIVE = "active"
        const val KEY_ZONES = "zones"
        const val KEY_ALERT_ID = "alertId"
        const val KEY_ALERT_TYPE = "alertType"
        const val KEY_DESCRIPTION = "description"
        const val KEY_TIMESTAMP = "timestamp"
        const val KEY_STATE = "state"

        private const val RPC_TIMEOUT_SECONDS = 12L
    }
}
