package mx.utng.ich.safecare.wearable.data.datalayer

import android.content.Context
import java.util.UUID

class WearIdentityStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

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
