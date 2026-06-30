package mx.utng.ich.safecare.wearable.presentation.geofence

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.util.Log
import mx.utng.ich.safecare.wearable.R
import mx.utng.ich.safecare.wearable.presentation.AlertActivity

object SafeCareAlertNotifier {

    fun showSafeZoneExitNotification(
        context: Context,
        zoneLabel: String? = null,
        location: Location? = null
    ): Boolean {
        val detail = if (zoneLabel.isNullOrBlank()) {
            "Se detecto que saliste del perimetro de seguridad."
        } else {
            "Se detecto salida de $zoneLabel."
        }

        return showNotification(
            context = context,
            notificationId = SAFE_ZONE_EXIT_NOTIFICATION_ID,
            requestCode = SAFE_ZONE_EXIT_REQUEST_CODE,
            title = "Saliste de zona segura",
            text = detail,
            alertMessage = "Saliste de zona segura",
            alertAddress = zoneLabel ?: "Zona segura",
            alertLocation = location,
            fullScreen = true
        )
    }

    fun showGeofenceErrorNotification(
        context: Context,
        errorMessage: String
    ): Boolean {
        return showNotification(
            context = context,
            notificationId = GEOFENCE_ERROR_NOTIFICATION_ID,
            requestCode = GEOFENCE_ERROR_REQUEST_CODE,
            title = "Zona segura sin monitoreo",
            text = errorMessage,
            alertMessage = "No se pudo monitorear la zona segura",
            alertAddress = errorMessage,
            alertLocation = null,
            fullScreen = false
        )
    }

    fun dismissSafeZoneExitNotification(context: Context) {
        val notificationManager =
            context.applicationContext.getSystemService(NotificationManager::class.java)
        notificationManager.cancel(SAFE_ZONE_EXIT_NOTIFICATION_ID)
    }

    private fun showNotification(
        context: Context,
        notificationId: Int,
        requestCode: Int,
        title: String,
        text: String,
        alertMessage: String,
        alertAddress: String,
        alertLocation: Location?,
        fullScreen: Boolean
    ): Boolean {
        val appContext = context.applicationContext

        if (!canPostNotifications(appContext)) {
            Log.w(TAG, "No se publico notificacion porque falta POST_NOTIFICATIONS")
            return false
        }

        val notificationManager = appContext.getSystemService(NotificationManager::class.java)
        ensureNotificationChannel(notificationManager)

        val alertIntent = Intent(appContext, AlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_MESSAGE", alertMessage)
            putExtra("EXTRA_ADDRESS", alertAddress)
            alertLocation?.let { location ->
                putExtra("EXTRA_LATITUDE", location.latitude)
                putExtra("EXTRA_LONGITUDE", location.longitude)
            }
        }

        val contentIntent = PendingIntent.getActivity(
            appContext,
            requestCode,
            alertIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_alert)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setFullScreenIntent(contentIntent, fullScreen)
            .setOngoing(fullScreen)
            .setAutoCancel(!fullScreen)
            .setCategory(Notification.CATEGORY_ALARM)
            .setColor(Color.rgb(211, 47, 47))
            .setPriority(Notification.PRIORITY_MAX)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setVibrate(VIBRATION_PATTERN)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .build()

        notificationManager.notify(notificationId, notification)
        return true
    }

    private fun canPostNotifications(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun ensureNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESCRIPTION
            enableVibration(true)
            vibrationPattern = VIBRATION_PATTERN
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        notificationManager.createNotificationChannel(channel)
    }

    private const val TAG = "SafeCareNotifier"
    private const val CHANNEL_ID = "safe_zone_alerts"
    private const val CHANNEL_NAME = "Alertas de zona segura"
    private const val CHANNEL_DESCRIPTION = "Avisos cuando el usuario sale de una zona segura"
    private const val SAFE_ZONE_EXIT_NOTIFICATION_ID = 2001
    private const val GEOFENCE_ERROR_NOTIFICATION_ID = 2002
    private const val SAFE_ZONE_EXIT_REQUEST_CODE = 3001
    private const val GEOFENCE_ERROR_REQUEST_CODE = 3002
    private val VIBRATION_PATTERN = longArrayOf(0, 500, 200, 500)
}
