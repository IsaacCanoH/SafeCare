package mx.utng.ich.safecare.wearable.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import mx.utng.ich.safecare.wearable.data.datalayer.WearIdentityStore
import mx.utng.ich.safecare.wearable.data.local.entity.SmartwatchEntity
import mx.utng.ich.safecare.wearable.data.local.entity.UbicacionEntity
import mx.utng.ich.safecare.wearable.data.repository.SupabaseRepository
import mx.utng.ich.safecare.wearable.presentation.location.WearLocationReader
import mx.utng.ich.safecare.wearable.presentation.sensors.DeviceStatusReader

class StatusWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    // Publica el estado del reloj y agenda su siguiente actualización.
    override suspend fun doWork(): Result {
        val reader = DeviceStatusReader(applicationContext)
        val watchId = WearIdentityStore(applicationContext).getOrCreateWatchId()
        val online = reader.isOnline()
        val repository = SupabaseRepository()
        val status = SmartwatchEntity(watchId, watchId, reader.getBatteryLevel(), if (online) "online" else "offline")
        if (online) repository.updateSmartWatchStatus(watchId, status.bateria, status.conexion)
        WearLocationReader(applicationContext).getCurrentLocationData()?.let { location ->
            val entity = UbicacionEntity(latitud = location.latitude, longitud = location.longitude, idSmartwatch = watchId)
            if (online) repository.saveLocation(entity)
        }
        return Result.success()
    }
}
