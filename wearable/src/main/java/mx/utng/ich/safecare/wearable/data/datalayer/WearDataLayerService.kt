
package mx.utng.ich.safecare.wearable.data.datalayer

import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import androidx.room.withTransaction
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.WearableListenerService
import java.util.concurrent.Executors
import kotlinx.coroutines.runBlocking
import mx.utng.ich.safecare.wearable.data.local.database.DatabaseProvider
import mx.utng.ich.safecare.wearable.data.local.entity.PerfilMonitoreadoEntity
import mx.utng.ich.safecare.wearable.data.local.entity.SmartwatchEntity
import mx.utng.ich.safecare.wearable.data.local.entity.ZonaSeguraEntity
import mx.utng.ich.safecare.wearable.data.local.entity.AlertaEntity
import mx.utng.ich.safecare.wearable.presentation.AlertActivity
import mx.utng.ich.safecare.wearable.presentation.geofence.SafeCareAlertNotifier
import mx.utng.ich.safecare.wearable.presentation.geofence.GeofenceManager
import mx.utng.ich.safecare.wearable.presentation.geofence.SafeZoneGeofence
import mx.utng.ich.safecare.wearable.presentation.geofence.SafeZoneMonitor
import org.json.JSONObject

class WearDataLayerService : WearableListenerService() {
    private val executor = Executors.newSingleThreadExecutor()

    /** Atiende solicitudes recibidas desde el teléfono emparejado. */
    override fun onRequest(
        nodeId: String,
        path: String,
        request: ByteArray
    ): Task<ByteArray>? {
        if (!path.startsWith(PATH_PREFIX)) return null
        return Tasks.call(executor) {
            runCatching {
                when (path) {
                    PATH_DEVICE_INFO -> deviceInfo()
                    PATH_LINK_PROFILE -> linkProfile(JSONObject(request.toString(Charsets.UTF_8)))
                    PATH_SYNC_ZONES -> syncZones(JSONObject(request.toString(Charsets.UTF_8)))
                    PATH_CUSTOM_ALERT -> receiveCustomAlert(
                        JSONObject(request.toString(Charsets.UTF_8))
                    )
                    PATH_UNLINK_PROFILE -> unlinkProfile(
                        JSONObject(request.toString(Charsets.UTF_8))
                    )
                    else -> errorResponse("Ruta Data Layer desconocida: $path")
                }
            }.getOrElse { exception ->
                errorResponse(exception.message ?: "Error de sincronización")
            }.toString().toByteArray(Charsets.UTF_8)
        }
    }

    /** Libera los recursos locales al destruir el servicio. */
    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }

    /** Construye la información de identificación y batería del reloj. */
    private fun deviceInfo(): JSONObject {
        val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
        return successResponse()
            .put(KEY_WATCH_ID, WearIdentityStore(this).getOrCreateWatchId())
            .put(KEY_DISPLAY_NAME, "${Build.MANUFACTURER} ${Build.MODEL}".trim())
            .put(KEY_MODEL, Build.MODEL)
            .put(
                KEY_BATTERY,
                batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            )
    }

    /** Guarda localmente el perfil enviado para vincular el reloj. */
    private fun linkProfile(payload: JSONObject): JSONObject = runBlocking {
        val watchId = WearIdentityStore(this@WearDataLayerService).getOrCreateWatchId()
        val requestedWatchId = payload.getString(KEY_WATCH_ID)
        require(requestedWatchId == watchId) {
            "La identidad del reloj no coincide"
        }

        val database = DatabaseProvider.getDatabase(this@WearDataLayerService)
        val profile = PerfilMonitoreadoEntity(
            idPerfil = payload.getString(KEY_PROFILE_ID),
            nombre = payload.getString(KEY_NAME),
            edad = payload.getInt(KEY_AGE),
            fechaNacimiento = payload.optNullableString(KEY_BIRTH_DATE),
            tipoPerfil = payload.getString(KEY_PROFILE_TYPE),
            foto = payload.optNullableString(KEY_PHOTO),
            estadoActual = true,
            idCuidador = payload.getString(KEY_CAREGIVER_ID)
        )

        database.withTransaction {
            database.perfilMonitoreadoDao().desactivarTodos()
            database.perfilMonitoreadoDao().insertar(profile)
            database.smartwatchDao().insertarOActualizar(
                SmartwatchEntity(
                    idSmartwatch = watchId,
                    numeroSerie = watchId,
                    bateria = currentBatteryLevel(),
                    conexion = "online",
                    estado = "ACTIVO",
                    idPerfil = profile.idPerfil
                )
            )
        }
        successResponse()
    }

    /** Reemplaza las zonas locales por las recibidas del teléfono. */
    private fun syncZones(payload: JSONObject): JSONObject = runBlocking {
        val profileId = payload.getString(KEY_PROFILE_ID)
        val database = DatabaseProvider.getDatabase(this@WearDataLayerService)
        require(database.perfilMonitoreadoDao().obtenerPorId(profileId) != null) {
            "El perfil todavía no está vinculado en el reloj"
        }

        val zonesJson = payload.getJSONArray(KEY_ZONES)
        val zones = buildList {
            for (index in 0 until zonesJson.length()) {
                val zone = zonesJson.getJSONObject(index)
                add(
                    ZonaSeguraEntity(
                        idZona = zone.getString(KEY_ZONE_ID),
                        nombre = zone.getString(KEY_NAME),
                        latitudCentro = zone.getDouble(KEY_LATITUDE),
                        longitudCentro = zone.getDouble(KEY_LONGITUDE),
                        radioMetros = zone.getDouble(KEY_RADIUS),
                        activa = zone.getBoolean(KEY_ACTIVE),
                        idPerfil = profileId
                    )
                )
            }
        }

        database.withTransaction {
            database.zonaSeguraDao().eliminarPorPerfil(profileId)
            if (zones.isNotEmpty()) {
                database.zonaSeguraDao().insertarZonas(zones)
            }
        }

        SafeZoneMonitor(this@WearDataLayerService).reset(profileId)
        GeofenceManager(this@WearDataLayerService).replaceGeofences(
            zones.filter { it.activa }.map { zone ->
                SafeZoneGeofence(
                    id = zone.idZona,
                    lat = zone.latitudCentro,
                    lng = zone.longitudCentro,
                    radiusInMeters = zone.radioMetros.toFloat()
                )
            }
        ).getOrThrow()
        successResponse().put("count", zones.size)
    }

    /** Elimina localmente el perfil y sus zonas asociadas. */
    private fun unlinkProfile(payload: JSONObject): JSONObject = runBlocking {
        val profileId = payload.getString(KEY_PROFILE_ID)
        val database = DatabaseProvider.getDatabase(this@WearDataLayerService)
        database.withTransaction {
            database.zonaSeguraDao().eliminarPorPerfil(profileId)
            database.perfilMonitoreadoDao().eliminarPorId(profileId)
            database.smartwatchDao().obtenerEstado()?.let { current ->
                if (current.idPerfil == profileId) {
                    database.smartwatchDao().insertarOActualizar(
                        current.copy(idPerfil = null)
                    )
                }
            }
        }
        successResponse()
    }

    /** Guarda y muestra una alerta personalizada recibida del teléfono. */
    private fun receiveCustomAlert(payload: JSONObject): JSONObject = runBlocking {
        val profileId = payload.getString(KEY_PROFILE_ID)
        val message = payload.getString(KEY_DESCRIPTION).trim()
        require(message.isNotEmpty()) { "El mensaje de la alerta está vacío" }
        require(message.length <= MAX_CUSTOM_ALERT_LENGTH) {
            "El mensaje excede $MAX_CUSTOM_ALERT_LENGTH caracteres"
        }

        val database = DatabaseProvider.getDatabase(this@WearDataLayerService)
        val profile = database.perfilMonitoreadoDao().obtenerPorId(profileId)
            ?: error("El perfil no está vinculado en este reloj")

        val alert = AlertaEntity(
            idAlerta = payload.getString(KEY_ALERT_ID),
            tipoAlerta = "ALERTA",
            descripcion = message,
            fechaHora = payload.getLong(KEY_TIMESTAMP),
            estado = payload.optString(KEY_STATE, "ACTIVA"),
            idPerfil = profileId
        )
        database.alertaDao().insertar(alert)

        SafeCareAlertNotifier.showCustomAlertNotification(
            context = this@WearDataLayerService,
            message = message
        )
        startActivity(
            Intent(this@WearDataLayerService, AlertActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(AlertActivity.EXTRA_ALERT_TYPE, "ALERTA")
                putExtra(AlertActivity.EXTRA_MESSAGE, message)
                putExtra(AlertActivity.EXTRA_ADDRESS, message)
            }
        )
        successResponse()
    }

    /** Obtiene el porcentaje actual de batería del reloj. */
    private fun currentBatteryLevel(): Int {
        val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    /** Crea una respuesta JSON de operación exitosa. */
    private fun successResponse() = JSONObject().put(KEY_SUCCESS, true)

    /** Crea una respuesta JSON con el error de la operación. */
    private fun errorResponse(message: String) = JSONObject()
        .put(KEY_SUCCESS, false)
        .put(KEY_ERROR, message)

    /** Obtiene un texto JSON tratando valores nulos como ausencia. */
    private fun JSONObject.optNullableString(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    companion object {
        private const val PATH_PREFIX = "/safecare"
        private const val PATH_DEVICE_INFO = "/safecare/device-info"
        private const val PATH_LINK_PROFILE = "/safecare/link-profile"
        private const val PATH_SYNC_ZONES = "/safecare/sync-zones"
        private const val PATH_CUSTOM_ALERT = "/safecare/custom-alert"
        private const val PATH_UNLINK_PROFILE = "/safecare/unlink-profile"

        private const val KEY_SUCCESS = "success"
        private const val KEY_ERROR = "error"
        private const val KEY_WATCH_ID = "watchInstallationId"
        private const val KEY_DISPLAY_NAME = "displayName"
        private const val KEY_MODEL = "model"
        private const val KEY_BATTERY = "battery"
        private const val KEY_PROFILE_ID = "profileId"
        private const val KEY_NAME = "name"
        private const val KEY_AGE = "age"
        private const val KEY_BIRTH_DATE = "birthDate"
        private const val KEY_PROFILE_TYPE = "profileType"
        private const val KEY_PHOTO = "photo"
        private const val KEY_CAREGIVER_ID = "caregiverId"
        private const val KEY_ZONE_ID = "zoneId"
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
        private const val KEY_RADIUS = "radius"
        private const val KEY_ACTIVE = "active"
        private const val KEY_ZONES = "zones"
        private const val KEY_ALERT_ID = "alertId"
        private const val KEY_DESCRIPTION = "description"
        private const val KEY_TIMESTAMP = "timestamp"
        private const val KEY_STATE = "state"
        private const val MAX_CUSTOM_ALERT_LENGTH = 160
    }
}
