package mx.utng.ich.safecare.wearable.data.worker

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import mx.utng.ich.safecare.wearable.data.model.SmartWatch
import mx.utng.ich.safecare.wearable.data.model.TipoConexion
import mx.utng.ich.safecare.wearable.data.model.Ubicacion
import mx.utng.ich.safecare.wearable.data.repository.SupabaseRepository
import mx.utng.ich.safecare.wearable.presentation.location.WearLocationReader
import mx.utng.ich.safecare.wearable.presentation.sensors.DeviceStatusReader

class StatusWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val repository = SupabaseRepository()
    private val deviceStatusReader = DeviceStatusReader(context)
    private val wearLocationReader = WearLocationReader(context)

    override suspend fun doWork(): Result {
        Log.i("StatusWorker", "Ejecutando monitoreo periódico...")

        val battery = deviceStatusReader.getBatteryLevel()
        val isOnline = deviceStatusReader.isOnline()
        val connectionType = if (isOnline) TipoConexion.ONLINE else TipoConexion.OFFLINE
        
        // Usamos Build.MODEL como identificador principal
        val serialNumber = Build.MODEL

        // 1. Actualizar estado del SmartWatch y obtener su UUID real
        val swUpdated = repository.updateSmartWatchStatus(
            numeroSerie = serialNumber,
            bateria = battery,
            conexion = if (isOnline) "online" else "offline"
        )
        val uuidSmartwatch = swUpdated?.id

        // 2. Registrar ubicación usando el UUID obtenido
        val location = wearLocationReader.getCurrentLocationData()
        Log.d("StatusWorker", "Datos obtenidos - UUID: $uuidSmartwatch, Location: $location")
        
        if (location != null && uuidSmartwatch != null) {
            repository.insertUbicacion(
                Ubicacion(
                    latitud = location.latitude,
                    longitud = location.longitude,
                    idSmartwatch = uuidSmartwatch
                )
            )
            Log.i("StatusWorker", "Monitoreo periódico guardado con éxito.")
        }

        return Result.success()
    }
}
