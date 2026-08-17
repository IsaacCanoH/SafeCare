package mx.utng.ich.safecare.wearable.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import mx.utng.ich.safecare.wearable.BuildConfig

/**
 * Instancia cliente nÃºcleo para la comunicaciÃ³n con la plataforma backend-as-a-service (Supabase).
 *  * Configura interceptores, tiempos de espera y mecanismos de reconexiÃ³n automÃ¡tica para las peticiones HTTP.
 */
object SupabaseClient {
    private val SUPABASE_URL = BuildConfig.SUPABASE_URL
    private val SUPABASE_KEY = BuildConfig.SUPABASE_KEY

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Postgrest)
        install(Auth)
        install(Realtime)
    }
}