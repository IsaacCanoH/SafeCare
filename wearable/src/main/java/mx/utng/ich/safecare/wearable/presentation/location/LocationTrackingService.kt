package mx.utng.ich.safecare.wearable.presentation.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mx.utng.ich.safecare.wearable.R
import mx.utng.ich.safecare.wearable.data.local.SafeCareProfileResolver
import mx.utng.ich.safecare.wearable.data.local.database.DatabaseProvider
import mx.utng.ich.safecare.wearable.data.local.entity.SmartwatchEntity
import mx.utng.ich.safecare.wearable.data.local.entity.UbicacionEntity
import mx.utng.ich.safecare.wearable.presentation.MainActivity
import mx.utng.ich.safecare.wearable.presentation.sensors.DeviceStatusReader

class LocationTrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var deviceStatusReader: DeviceStatusReader
    private var isTrackingStarted = false
    private var isStatusMonitoringStarted = false

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        LOCATION_INTERVAL_MILLIS
    )
        .setMinUpdateIntervalMillis(LOCATION_INTERVAL_MILLIS)
        .setMaxUpdateDelayMillis(LOCATION_INTERVAL_MILLIS)
        .setWaitForAccurateLocation(false)
        .build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { location ->
                saveLocation(location)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        deviceStatusReader = DeviceStatusReader(this)
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Servicio de ubicacion detenido: falta ACCESS_FINE_LOCATION")
            stopSelf()
            return START_NOT_STICKY
        }

        startAsForegroundService()
        startLocationUpdates()
        startStatusMonitoring()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
        super.onDestroy()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (isTrackingStarted) {
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        ).addOnSuccessListener {
            isTrackingStarted = true
            Log.i(TAG, "Tracking de ubicacion iniciado cada ${LOCATION_INTERVAL_MILLIS / 1000}s")
        }.addOnFailureListener { exception ->
            Log.e(TAG, "No se pudo iniciar tracking de ubicacion", exception)
            stopSelf()
        }
    }

    private fun saveLocation(location: Location) {
        serviceScope.launch {
            val database = DatabaseProvider.getDatabase(applicationContext)
            val ubicacionDao = database.ubicacionDao()

            val insertedId = ubicacionDao.insertar(
                UbicacionEntity(
                    latitud = location.latitude,
                    longitud = location.longitude,
                    fechaHora = location.time.takeIf { it > 0L } ?: System.currentTimeMillis(),
                    idSmartwatch = Build.MODEL
                )
            )

            ubicacionDao.conservarSoloRegistrosRecientes(MAX_LOCATION_RECORDS)
            Log.d(TAG, "Ubicacion guardada id=$insertedId")
        }
    }

    private fun startStatusMonitoring() {
        if (isStatusMonitoringStarted) {
            return
        }

        isStatusMonitoringStarted = true
        serviceScope.launch {
            while (isActive) {
                saveStatusIfNeeded()
                delay(STATUS_CHECK_INTERVAL_MILLIS)
            }
        }
    }

    private suspend fun saveStatusIfNeeded() {
        val database = DatabaseProvider.getDatabase(applicationContext)
        val smartwatchDao = database.smartwatchDao()
        val serialNumber = Build.MODEL
        val now = System.currentTimeMillis()
        val battery = deviceStatusReader.getBatteryLevel()
        val isOnline = deviceStatusReader.isOnline()
        val connection = if (isOnline) "online" else "offline"
        val currentStatus = smartwatchDao.obtenerPorNumeroSerie(serialNumber)

        val batteryChanged = currentStatus?.bateria != battery
        val connectionChanged = currentStatus?.conexion != connection
        val heartbeatDue = currentStatus == null ||
                now - currentStatus.ultimaConexion >= STATUS_HEARTBEAT_INTERVAL_MILLIS

        if (!batteryChanged && !connectionChanged && !heartbeatDue) {
            return
        }

        val idPerfil = currentStatus?.idPerfil ?: SafeCareProfileResolver.resolveProfileId(database)
        val smartwatchId = smartwatchDao.insertarOActualizar(
            SmartwatchEntity(
                numeroSerie = serialNumber,
                bateria = battery,
                conexion = connection,
                ultimaConexion = now,
                estado = if (isOnline) "ACTIVO" else "INACTIVO",
                idPerfil = idPerfil
            )
        )

        smartwatchDao.conservarSoloRegistrosRecientes(MAX_SMARTWATCH_RECORDS)
        Log.d(TAG, "Estado wearable guardado en smartwatch id=$smartwatchId")
    }

    private fun startAsForegroundService() {
        val notification = createNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                TRACKING_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(TRACKING_NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            TRACKING_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_alert)
            .setContentTitle("SafeCare activo")
            .setContentText("Monitoreando ubicacion y estado del wearable")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .build()
    }

    private fun ensureNotificationChannel() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = CHANNEL_DESCRIPTION
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        notificationManager.createNotificationChannel(channel)
    }

    private fun hasLocationPermission(): Boolean {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "LocationTrackingService"
        private const val CHANNEL_ID = "safe_location_tracking"
        private const val CHANNEL_NAME = "Seguimiento de ubicacion"
        private const val CHANNEL_DESCRIPTION =
            "Servicio que registra la ubicacion del wearable periodicamente"
        private const val TRACKING_NOTIFICATION_ID = 2101
        private const val TRACKING_REQUEST_CODE = 3101
        private const val LOCATION_INTERVAL_MILLIS = 5_000L
        private const val MAX_LOCATION_RECORDS = 5_000
        private const val STATUS_CHECK_INTERVAL_MILLIS = 5_000L
        private const val STATUS_HEARTBEAT_INTERVAL_MILLIS = 60_000L
        private const val MAX_SMARTWATCH_RECORDS = 10_000

        fun start(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
