package mx.utng.ich.safecare.wearable.presentation.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mx.utng.ich.safecare.wearable.data.model.Alerta
import mx.utng.ich.safecare.wearable.data.model.TipoAlerta
import mx.utng.ich.safecare.wearable.data.model.Ubicacion
import mx.utng.ich.safecare.wearable.data.repository.SupabaseRepository
import mx.utng.ich.safecare.wearable.presentation.AlertActivity
import mx.utng.ich.safecare.wearable.presentation.location.WearLocationReader
import mx.utng.ich.safecare.wearable.presentation.sensors.DeviceStatusReader

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    private val repository = SupabaseRepository()
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("GeofenceReceiver", "Evento de geocerca RECIBIDO")
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: run {
            Log.e("GeofenceReceiver", "El Intent no contenía un GeofencingEvent")
            return
        }

        if (geofencingEvent.hasError()) {
            Log.e("GeofenceReceiver", "Error en evento: ${geofencingEvent.errorCode}")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        Log.i("GeofenceReceiver", "Transición detectada: $geofenceTransition (EXIT=${Geofence.GEOFENCE_TRANSITION_EXIT}, ENTER=${Geofence.GEOFENCE_TRANSITION_ENTER})")

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            Log.w("GeofenceReceiver", "¡USUARIO SALIÓ DE ZONA SEGURA!")
            
            triggerVibration(context)
            
            val deviceStatusReader = DeviceStatusReader(context)
            val wearLocationReader = WearLocationReader(context)

            // Registrar alerta completa en Supabase
            scope.launch {
                val serialIdentificador = Build.MODEL
                val idPerfil = "b84236e7-578d-4a1e-8761-0b5c1792f582" 
                
                // 1. Actualizar estado del Smartwatch
                val swUpdated = repository.updateSmartWatchStatus(
                    numeroSerie = serialIdentificador,
                    bateria = deviceStatusReader.getBatteryLevel(),
                    conexion = if (deviceStatusReader.isOnline()) "online" else "offline"
                )
                val uuidSmartwatch = swUpdated?.id ?: ""

                // 2. Insertar Ubicación
                val locationData = wearLocationReader.getCurrentLocationData()
                var idUbicacion: String? = null
                if (locationData != null && uuidSmartwatch.isNotEmpty()) {
                    val nuevaUbicacion = repository.insertUbicacion(
                        Ubicacion(
                            latitud = locationData.latitude,
                            longitud = locationData.longitude,
                            idSmartwatch = uuidSmartwatch
                        )
                    )
                    idUbicacion = nuevaUbicacion?.id
                }

                // 3. Insertar Alerta de Zona Segura
                val alerta = Alerta(
                    tipoAlerta = TipoAlerta.ZONA_SEGURA,
                    descripcion = "El usuario ha salido del perímetro de seguridad",
                    idPerfil = idPerfil,
                    idUbicacion = idUbicacion
                )
                repository.insertAlerta(alerta)
                Log.i("GeofenceReceiver", "Alerta de salida registrada en DB")
            }

            // Abrir la pantalla de alerta
            val alertIntent = Intent(context, AlertActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_MESSAGE", "Saliste de zona segura")
                putExtra("EXTRA_ADDRESS", "Zona: Casa Abuelo") // Puedes dinámicamente obtener esto del geofencingEvent.triggeringGeofences
            }
            context.startActivity(alertIntent)
        }
    }

    private fun triggerVibration(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 500, 200, 500)
            val amplitudes = intArrayOf(0, 255, 0, 255)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            vibrator.vibrate(1000)
        }
    }
}
