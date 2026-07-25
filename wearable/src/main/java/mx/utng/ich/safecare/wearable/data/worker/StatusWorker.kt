package mx.utng.ich.safecare.wearable.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import mx.utng.ich.safecare.wearable.data.local.database.DatabaseProvider
import mx.utng.ich.safecare.wearable.data.datalayer.WearIdentityStore
import mx.utng.ich.safecare.wearable.data.datalayer.WearDataPublisher
import mx.utng.ich.safecare.wearable.data.local.entity.SmartwatchEntity
import mx.utng.ich.safecare.wearable.data.local.entity.UbicacionEntity
import mx.utng.ich.safecare.wearable.data.repository.SupabaseRepository
import mx.utng.ich.safecare.wearable.presentation.location.WearLocationReader
import mx.utng.ich.safecare.wearable.presentation.sensors.DeviceStatusReader

class StatusWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val deviceStatusReader = DeviceStatusReader(context)
    private val wearLocationReader = WearLocationReader(context)
    private val supabaseRepository = SupabaseRepository()

    override suspend fun doWork(): Result {
        Log.i("StatusWorker", "Ejecutando monitoreo periódico...")

        val battery = deviceStatusReader.getBatteryLevel()
        val isOnline = deviceStatusReader.isOnline()
        val serialNumber = WearIdentityStore(applicationContext).getOrCreateWatchId()

        // 1. Guardar en Supabase (Sincronización Global)
        if (isOnline) {
            supabaseRepository.updateSmartWatchStatus(
                numeroSerie = serialNumber,
                bateria = battery,
                conexion = "online"
            )
        }

        val database = DatabaseProvider.getDatabase(applicationContext)
        val smartwatchDao = database.smartwatchDao()
        val ubicacionDao = database.ubicacionDao()

        // 1. Guardar estado del Smartwatch localmente
        val smartwatchLocal = SmartwatchEntity(
            idSmartwatch = serialNumber,
            numeroSerie = serialNumber,
            bateria = battery,
            conexion = if (isOnline) "online" else "offline",
            estado = if (isOnline) "ACTIVO" else "INACTIVO"
        )
        smartwatchDao.insertarOActualizar(smartwatchLocal)
        WearDataPublisher(applicationContext).publishStatus(smartwatchLocal)

        // 2. Guardar Ubicación localmente
        val location = wearLocationReader.getCurrentLocationData()
        if (location != null) {
            val nuevaUbicacion = UbicacionEntity(
                latitud = location.latitude,
                longitud = location.longitude,
                idSmartwatch = serialNumber
            )
            ubicacionDao.insertar(nuevaUbicacion)
            WearDataPublisher(applicationContext).publishLocation(nuevaUbicacion)
            if (isOnline) {
                supabaseRepository.saveLocation(nuevaUbicacion)
            }
        }

        Log.i("StatusWorker", "Datos guardados localmente en Room")

        return Result.success()
    }
}
