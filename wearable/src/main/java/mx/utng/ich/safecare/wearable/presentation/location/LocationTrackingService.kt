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
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mx.utng.ich.safecare.wearable.R
import mx.utng.ich.safecare.wearable.data.datalayer.WearIdentityStore
import mx.utng.ich.safecare.wearable.data.datalayer.WearDataPublisher
import mx.utng.ich.safecare.wearable.data.local.SafeCareProfileResolver
import mx.utng.ich.safecare.wearable.data.local.database.DatabaseProvider
import mx.utng.ich.safecare.wearable.data.local.entity.SmartwatchEntity
import mx.utng.ich.safecare.wearable.data.local.entity.UbicacionEntity
import mx.utng.ich.safecare.wearable.presentation.MainActivity
import mx.utng.ich.safecare.wearable.presentation.geofence.SafeZoneMonitor
import mx.utng.ich.safecare.wearable.presentation.sensors.DeviceStatusReader
import mx.utng.ich.safecare.wearable.data.repository.SupabaseRepository

class LocationTrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var locationManager: LocationManager
    private lateinit var deviceStatusReader: DeviceStatusReader
    private lateinit var safeZoneMonitor: SafeZoneMonitor
    private val supabaseRepository = SupabaseRepository()
    private var isTrackingStarted = false
    private var isStatusMonitoringStarted = false

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (isUsableWatchGpsLocation(location)) {
                saveLocation(location)
            } else {
                Log.w(
                    TAG,
                    "Lectura GPS descartada: provider=${location.provider}, " +
                            "ageMs=${locationAgeMillis(location)}, accuracy=${location.accuracy}"
                )
            }
        }

        override fun onProviderEnabled(provider: String) = Unit
        override fun onProviderDisabled(provider: String) {
            Log.w(TAG, "Proveedor GPS del reloj deshabilitado: $provider")
        }
        @Deprecated("Deprecated in Android")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LocationManager::class.java)
        deviceStatusReader = DeviceStatusReader(this)
        safeZoneMonitor = SafeZoneMonitor(this)
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
        locationManager.removeUpdates(locationListener)
        serviceScope.cancel()
        super.onDestroy()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (isTrackingStarted) {
            return
        }

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.w(TAG, "GPS del reloj deshabilitado; no se guardarán coordenadas del teléfono")
            return
        }

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            LOCATION_INTERVAL_MILLIS,
            MIN_LOCATION_DISTANCE_METERS,
            locationListener,
            mainLooper
        )
        run {
            isTrackingStarted = true
            Log.i(
                TAG,
                "Tracking GPS del reloj iniciado cada ${LOCATION_INTERVAL_MILLIS / 1000}s"
            )
        }
    }

    private fun isUsableWatchGpsLocation(location: Location): Boolean {
        return location.provider == LocationManager.GPS_PROVIDER &&
                location.latitude in -90.0..90.0 &&
                location.longitude in -180.0..180.0 &&
                locationAgeMillis(location) <= MAX_LOCATION_AGE_MILLIS &&
                (!location.hasAccuracy() || location.accuracy <= MAX_ACCURACY_METERS)
    }

    private fun locationAgeMillis(location: Location): Long {
        val elapsedNanos = location.elapsedRealtimeNanos
        if (elapsedNanos <= 0L) return Long.MAX_VALUE
        return (
            SystemClock.elapsedRealtimeNanos() - elapsedNanos
        ).coerceAtLeast(0L) / 1_000_000L
    }

    private fun saveLocation(location: Location) {
        serviceScope.launch {
            val database = DatabaseProvider.getDatabase(applicationContext)
            val ubicacionDao = database.ubicacionDao()

            val locationEntity = UbicacionEntity(
                latitud = location.latitude,
                longitud = location.longitude,
                fechaHora = location.time.takeIf { it > 0L } ?: System.currentTimeMillis(),
                idSmartwatch = WearIdentityStore(applicationContext).getOrCreateWatchId()
            )
            val insertedId = ubicacionDao.insertar(locationEntity)
            WearDataPublisher(applicationContext).publishLocation(locationEntity)
            safeZoneMonitor.evaluate(location)

            if (deviceStatusReader.isOnline()) {
                supabaseRepository.saveLocation(locationEntity)
            }

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
        val serialNumber = WearIdentityStore(applicationContext).getOrCreateWatchId()
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
        val status = SmartwatchEntity(
                idSmartwatch = serialNumber,
                numeroSerie = serialNumber,
                bateria = battery,
                conexion = connection,
                ultimaConexion = now,
                estado = if (isOnline) "ACTIVO" else "INACTIVO",
                idPerfil = idPerfil
            )
        val smartwatchId = smartwatchDao.insertarOActualizar(status)
        WearDataPublisher(applicationContext).publishStatus(status)

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
        private const val MIN_LOCATION_DISTANCE_METERS = 0f
        private const val MAX_LOCATION_AGE_MILLIS = 30_000L
        private const val MAX_ACCURACY_METERS = 200f
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
