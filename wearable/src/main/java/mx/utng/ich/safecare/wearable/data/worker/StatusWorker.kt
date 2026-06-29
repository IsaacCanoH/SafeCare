package mx.utng.ich.safecare.wearable.data.worker

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import mx.utng.ich.safecare.wearable.data.local.database.DatabaseProvider
import mx.utng.ich.safecare.wearable.data.local.entity.SmartwatchEntity
import mx.utng.ich.safecare.wearable.data.local.entity.UbicacionEntity
import mx.utng.ich.safecare.wearable.presentation.location.WearLocationReader
import mx.utng.ich.safecare.wearable.presentation.sensors.DeviceStatusReader

class StatusWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val deviceStatusReader = DeviceStatusReader(context)
    private val wearLocationReader = WearLocationReader(context)

    override suspend fun doWork(): Result {
        Log.i("StatusWorker", "Ejecutando monitoreo periódico...")

        val battery = deviceStatusReader.getBatteryLevel()
        val isOnline = deviceStatusReader.isOnline()
        val serialNumber = Build.MODEL

        val database = DatabaseProvider.getDatabase(applicationContext)
        val smartwatchDao = database.smartwatchDao()
        val ubicacionDao = database.ubicacionDao()

        // 1. Guardar estado del Smartwatch localmente
        val smartwatchLocal = SmartwatchEntity(
            numeroSerie = serialNumber,
            bateria = battery,
            conexion = if (isOnline) "online" else "offline",
            sincronizado = false
        )
        smartwatchDao.insertarOActualizar(smartwatchLocal)

        // 2. Guardar Ubicación localmente
        val location = wearLocationReader.getCurrentLocationData()
        if (location != null) {
            val nuevaUbicacion = UbicacionEntity(
                latitud = location.latitude,
                longitud = location.longitude,
                idSmartwatch = serialNumber,
                sincronizada = false
            )
            ubicacionDao.insertar(nuevaUbicacion)
        }

        Log.i("StatusWorker", "Datos guardados localmente en Room")

        return Result.success()
    }
}
