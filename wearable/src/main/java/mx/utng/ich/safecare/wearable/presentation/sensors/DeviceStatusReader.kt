package mx.utng.ich.safecare.wearable.presentation.sensors

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import mx.utng.ich.safecare.wearable.presentation.data.DeviceStatus

/**
 * Servicio lector especializado en monitorear y recolectar continuamente el estado fÃ­sico y de conectividad del dispositivo.
 *  * Proporciona el flujo de datos necesario para evaluar si el equipo se encuentra en condiciones Ã³ptimas para el monitoreo del paciente.
 */
class DeviceStatusReader(
    private val context: Context
) {

    /** Reúne el estado de batería y conexión del reloj. */
    fun getDeviceStatus(): DeviceStatus {
        return DeviceStatus(
            batteryText = getBatteryStatusText(),
            connectionText = getConnectionStatusText()
        )
    }

    /** Obtiene el porcentaje actual de batería del dispositivo. */
    fun getBatteryLevel(): Int {
        val batteryIntent: Intent? =
            context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100) / scale else -1
    }

    /** Verifica si el reloj tiene una conexión de red activa. */
    fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /** Genera el texto de estado según la carga actual. */
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

    /** Genera el texto de estado según la conectividad actual. */
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