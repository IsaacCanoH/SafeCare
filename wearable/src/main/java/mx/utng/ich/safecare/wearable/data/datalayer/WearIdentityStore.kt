package mx.utng.ich.safecare.wearable.data.datalayer

import android.content.Context
import java.util.UUID

/**
 * Componente de almacenamiento criptogrÃ¡ficamente seguro dentro del dispositivo Wear OS.
 *  * Gestiona, protege y audita los tokens de autenticaciÃ³n y los secretos criptogrÃ¡ficos necesarios para la vinculaciÃ³n confiable con el telÃ©fono maestro.
 */
class WearIdentityStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    /** Recupera o crea el identificador único de esta instalación. */
    fun getOrCreateWatchId(): String {
        preferences.getString(KEY_WATCH_ID, null)?.let { return it }
        val newId = UUID.randomUUID().toString()
        preferences.edit().putString(KEY_WATCH_ID, newId).apply()
        return newId
    }

    companion object {
        private const val PREFERENCES_NAME = "safecare_wear_identity"
        private const val KEY_WATCH_ID = "watch_installation_id"
    }
}