package mx.utng.ich.safecare.wearable.presentation.sensors

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import mx.utng.ich.safecare.wearable.presentation.data.DeviceStatus

class DeviceStatusReader(
    private val context: Context
) {

    fun getDeviceStatus(): DeviceStatus {
        return DeviceStatus(
            batteryText = getBatteryStatusText(),
            connectionText = getConnectionStatusText()
        )
    }

    private fun getBatteryStatusText(): String {
        val batteryIntent: Intent? =
            context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )

        val level =
            batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1

        val scale =
            batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        val batteryPercentage =
            if (level >= 0 && scale > 0) {
                (level * 100) / scale
            } else {
                -1
            }

        val status =
            batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        val isCharging =
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

        val chargingText =
            if (isCharging) {
                "Cargando"
            } else {
                "No cargando"
            }

        return if (batteryPercentage >= 0) {
            "Batería: $batteryPercentage%\nEstado: $chargingText"
        } else {
            "No se pudo obtener batería"
        }
    }

    private fun getConnectionStatusText(): String {
        val connectivityManager =
            context.getSystemService(ConnectivityManager::class.java)

        val activeNetwork =
            connectivityManager.activeNetwork

        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(activeNetwork)

        if (networkCapabilities == null) {
            return "Conexión: Sin conexión"
        }

        val hasInternet =
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        val isValidated =
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        val connectionType =
            when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Datos móviles"
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "Bluetooth"
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Otro tipo de conexión"
            }

        val internetStatus =
            if (hasInternet && isValidated) {
                "Con internet"
            } else if (hasInternet) {
                "Red detectada"
            } else {
                "Sin internet"
            }

        return "Conexión: $internetStatus\nTipo: $connectionType"
    }
}