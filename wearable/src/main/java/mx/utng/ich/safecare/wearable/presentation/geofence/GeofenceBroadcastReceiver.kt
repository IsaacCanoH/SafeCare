package mx.utng.ich.safecare.wearable.presentation.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mx.utng.ich.safecare.wearable.data.local.SafeCareProfileResolver
import mx.utng.ich.safecare.wearable.data.datalayer.WearIdentityStore
import mx.utng.ich.safecare.wearable.data.datalayer.WearDataPublisher
import mx.utng.ich.safecare.wearable.data.local.database.DatabaseProvider
import mx.utng.ich.safecare.wearable.data.local.entity.AlertaEntity
import mx.utng.ich.safecare.wearable.data.local.entity.SmartwatchEntity
import mx.utng.ich.safecare.wearable.data.local.entity.UbicacionEntity
import mx.utng.ich.safecare.wearable.presentation.AlertActivity
import mx.utng.ich.safecare.wearable.presentation.location.WearLocationReader
import mx.utng.ich.safecare.wearable.presentation.sensors.DeviceStatusReader
import mx.utng.ich.safecare.wearable.data.repository.SupabaseRepository

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        Log.d(TAG, "Evento de geocerca recibido")

        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: run {
            Log.e(TAG, "El Intent no contenia un GeofencingEvent")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "Error en evento de geocerca: $errorMessage")
            SafeCareAlertNotifier.showGeofenceErrorNotification(appContext, errorMessage)
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        Log.i(
            TAG,
            "Transicion detectada: $geofenceTransition " +
                    "(EXIT=${Geofence.GEOFENCE_TRANSITION_EXIT}, " +
                    "ENTER=${Geofence.GEOFENCE_TRANSITION_ENTER})"
        )

        if (geofenceTransition != Geofence.GEOFENCE_TRANSITION_EXIT) {
            return
        }

        val geofenceId = geofencingEvent.triggeringGeofences?.firstOrNull()?.requestId
        val zoneLabel = geofenceId?.let { "Zona $it" }
        val triggeringLocation = geofencingEvent.triggeringLocation

        Log.w(TAG, "Usuario salio de zona segura: ${zoneLabel ?: "zona desconocida"}")

        val pendingResult = goAsync()
        handleSafeZoneExit(appContext, zoneLabel, triggeringLocation) {
            pendingResult.finish()
        }
    }

    private suspend fun saveSafeZoneExitAlert(
        context: Context,
        triggeringLocation: Location?
    ) {
        val deviceStatusReader = DeviceStatusReader(context)
        val wearLocationReader = WearLocationReader(context)
        val serialIdentificador = WearIdentityStore(context).getOrCreateWatchId()

        val database = DatabaseProvider.getDatabase(context)
        val alertaDao = database.alertaDao()
        val ubicacionDao = database.ubicacionDao()
        val smartwatchDao = database.smartwatchDao()
        val idPerfil = SafeCareProfileResolver.resolveProfileId(database)
        val profileName = database.perfilMonitoreadoDao()
            .obtenerPorId(idPerfil)
            ?.nombre
            ?.takeIf { it.isNotBlank() }
            ?: "El perfil monitoreado"

        val batteryLevel = deviceStatusReader.getBatteryLevel()
        val isOnline = deviceStatusReader.isOnline()
        val smartwatchLocal = SmartwatchEntity(
            idSmartwatch = serialIdentificador,
            numeroSerie = serialIdentificador,
            bateria = batteryLevel,
            conexion = if (isOnline) "online" else "offline",
            estado = if (isOnline) "ACTIVO" else "INACTIVO",
            idPerfil = idPerfil
        )
        smartwatchDao.insertarOActualizar(smartwatchLocal)

        val locationData = triggeringLocation ?: wearLocationReader.getCurrentLocationData()
        var localUbicacionId: String? = null

        if (locationData != null) {
            val nuevaUbicacion = UbicacionEntity(
                latitud = locationData.latitude,
                longitud = locationData.longitude,
                idSmartwatch = serialIdentificador
            )
            ubicacionDao.insertar(nuevaUbicacion)
            WearDataPublisher(context).publishLocation(nuevaUbicacion)
            if (isOnline) {
                SupabaseRepository().saveLocation(nuevaUbicacion)
            }
            localUbicacionId = nuevaUbicacion.idUbicacion
        }

        val alertaLocal = AlertaEntity(
            tipoAlerta = "FUERA_ZONA_SEGURA",
            descripcion = "$profileName salió del perímetro de la zona segura",
            idPerfil = idPerfil,
            idUbicacion = localUbicacionId
        )
        alertaDao.insertar(alertaLocal)
        WearDataPublisher(context).publishAlert(
            serialIdentificador,
            alertaLocal,
            locationData
        )
    }

    private fun triggerVibration(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (!vibrator.hasVibrator()) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 500, 200, 500)
            val amplitudes = intArrayOf(0, 255, 0, 255)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            vibrator.vibrate(1000)
        }
    }

    private fun launchAlertActivity(
        context: Context,
        zoneLabel: String?,
        triggeringLocation: Location?
    ) {
        val intent = Intent(context, AlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_MESSAGE", "Saliste de zona segura")
            putExtra("EXTRA_ADDRESS", zoneLabel ?: "Zona segura")
            triggeringLocation?.let { location ->
                putExtra("EXTRA_LATITUDE", location.latitude)
                putExtra("EXTRA_LONGITUDE", location.longitude)
            }
        }
        context.startActivity(intent)
    }

    companion object {
        private const val TAG = "GeofenceReceiver"

        fun handleSafeZoneExit(
            context: Context,
            zoneLabel: String?,
            triggeringLocation: Location?,
            onFinished: (() -> Unit)? = null
        ) {
            val receiver = GeofenceBroadcastReceiver()
            val appContext = context.applicationContext

            receiver.launchAlertActivity(appContext, zoneLabel, triggeringLocation)
            SafeCareAlertNotifier.showSafeZoneExitNotification(
                context = appContext,
                zoneLabel = zoneLabel,
                location = triggeringLocation
            )
            receiver.triggerVibration(appContext)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    receiver.saveSafeZoneExitAlert(appContext, triggeringLocation)
                    Log.i(TAG, "Alerta de salida guardada en Room y enviada por Data Layer")
                } catch (exception: Exception) {
                    Log.e(TAG, "No se pudo guardar la alerta de zona segura", exception)
                } finally {
                    onFinished?.invoke()
                }
            }
        }
    }
}
