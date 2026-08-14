
package mx.utng.ich.safecare.wearable.data.local

import android.util.Log
import androidx.room.withTransaction
import mx.utng.ich.safecare.wearable.data.local.database.SafeCareDatabase
import mx.utng.ich.safecare.wearable.data.repository.SupabaseRepository

object SafeCareProfileResolver {
    /**
     * Obtiene el perfil vinculado al reloj. Si Room todavía no tiene la configuración,
     * la recupera desde Supabase y la deja disponible para los siguientes eventos.
     *
     * Nunca fabrica un identificador: una alerta sin perfil asociado no se debe publicar,
     * porque ningún cuidador podría relacionarla ni atenderla correctamente.
     */
    suspend fun resolveProfileId(
        database: SafeCareDatabase,
        watchId: String,
        repository: SupabaseRepository = SupabaseRepository()
    ): String? {
        database.perfilMonitoreadoDao().obtenerPerfilActivo()?.idPerfil?.let { return it }

        val configuration = repository.fetchLinkedConfiguration(watchId)
        if (configuration == null) {
            Log.w(TAG, "No hay un perfil vinculado para el reloj $watchId")
            return null
        }

        database.withTransaction {
            database.perfilMonitoreadoDao().desactivarTodos()
            database.perfilMonitoreadoDao().insertar(
                configuration.profile.copy(estadoActual = true)
            )
            database.zonaSeguraDao().eliminarPorPerfil(configuration.profile.idPerfil)
            if (configuration.zones.isNotEmpty()) {
                database.zonaSeguraDao().insertarZonas(configuration.zones)
            }
            database.smartwatchDao().obtenerPorNumeroSerie(watchId)?.let { smartwatch ->
                database.smartwatchDao().insertarOActualizar(
                    smartwatch.copy(idPerfil = configuration.profile.idPerfil)
                )
            }
        }
        Log.i(TAG, "Perfil vinculado recuperado para el reloj $watchId")
        return configuration.profile.idPerfil
    }

    private const val TAG = "ProfileResolver"
}
