package mx.utng.ich.safecare.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.ktor.client.engine.okhttp.OkHttp
import mx.utng.ich.safecare.BuildConfig

/**
 * Instancia cliente nÃºcleo para la comunicaciÃ³n con la plataforma backend-as-a-service (Supabase).
 *  * Configura interceptores, tiempos de espera y mecanismos de reconexiÃ³n automÃ¡tica para las peticiones HTTP.
 */
object SupabaseClient {
    /** URL del proyecto de Supabase obtenida desde BuildConfig. */
    private val SUPABASE_URL = BuildConfig.SUPABASE_URL
    /** Clave anónima del proyecto de Supabase obtenida desde BuildConfig. */
    private val SUPABASE_KEY = BuildConfig.SUPABASE_KEY

    /** Instancia única del cliente de Supabase configurada con Auth, Postgrest y Realtime. */
    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        httpEngine = OkHttp.create()
        install(Postgrest)
        install(Auth)
        install(Realtime)
    }
}