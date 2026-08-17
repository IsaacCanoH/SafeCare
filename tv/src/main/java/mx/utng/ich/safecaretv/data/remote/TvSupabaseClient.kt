package mx.utng.ich.safecaretv.data.remote

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import mx.utng.ich.safecaretv.BuildConfig

/**
 * VersiÃ³n especializada del cliente Supabase para el ecosistema de Android TV.
 *  * Incluye configuraciones de red y manejo de sesiones ajustados al ciclo de vida prolongado de los dispositivos de sala de estar.
 */
object TvSupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        install(Auth)
        install(Postgrest)
        install(Realtime)
    }
}