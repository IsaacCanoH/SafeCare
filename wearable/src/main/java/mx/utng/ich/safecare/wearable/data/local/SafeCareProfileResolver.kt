package mx.utng.ich.safecare.wearable.data.local

import mx.utng.ich.safecare.wearable.data.local.database.SafeCareDatabase

object SafeCareProfileResolver {
    const val FALLBACK_PROFILE_ID = "b84236e7-578d-4a1e-8761-0b5c1792f582"

    suspend fun resolveProfileId(database: SafeCareDatabase): String {
        return database.perfilMonitoreadoDao().obtenerPerfilActivo()?.idPerfil
            ?: FALLBACK_PROFILE_ID
    }
}
