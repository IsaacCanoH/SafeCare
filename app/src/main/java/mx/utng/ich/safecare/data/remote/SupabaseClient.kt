package mx.utng.ich.safecare.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.ktor.client.engine.okhttp.OkHttp
import mx.utng.ich.safecare.BuildConfig

/**
 * Cliente singleton de Supabase para la aplicación móvil del cuidador.
 *
 * Configura e inicializa una única instancia del cliente de Supabase con los módulos
 * de autenticación ([Auth]), consultas a la base de datos ([Postgrest]) y
 * suscripciones en tiempo real ([Realtime]) utilizando el motor HTTP OkHttp.
 *
 * Las credenciales se obtienen de forma segura desde [BuildConfig], generadas
 * a partir de `local.properties` durante la compilación.
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
