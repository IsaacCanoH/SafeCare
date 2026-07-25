package mx.utng.ich.safecaretv.data.sound

import android.content.Context
import mx.utng.ich.safecaretv.R

data class AlertTone(
    val id: Int,
    val name: String,
    val description: String,
    val soundResId: Int
)

object AlertTones {
    val all = listOf(
        AlertTone(1, "Alerta clásica", "Dos pulsos claros", R.raw.alert_tone_1),
        AlertTone(2, "Campana", "Aviso suave y brillante", R.raw.alert_tone_2),
        AlertTone(3, "Urgente", "Pulsos rápidos", R.raw.alert_tone_3),
        AlertTone(4, "Radar", "Barrido ascendente", R.raw.alert_tone_4),
        AlertTone(5, "Digital", "Secuencia electrónica", R.raw.alert_tone_5),
        AlertTone(6, "Doble aviso", "Dos notas alternadas", R.raw.alert_tone_6),
        AlertTone(7, "Emergencia", "Sirena breve", R.raw.alert_tone_7),
        AlertTone(8, "Atención", "Tres campanadas", R.raw.alert_tone_8)
    )

    fun find(id: Int): AlertTone = all.firstOrNull { it.id == id } ?: all.first()
}

object AlertTonePreferences {
    private const val PREFERENCES_NAME = "tv_alert_tone_preferences"
    private const val SELECTED_TONE_KEY = "selected_tone"

    fun selected(context: Context): AlertTone {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        return AlertTones.find(preferences.getInt(SELECTED_TONE_KEY, AlertTones.all.first().id))
    }

    fun select(context: Context, tone: AlertTone) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(SELECTED_TONE_KEY, tone.id)
            .apply()
    }
}
