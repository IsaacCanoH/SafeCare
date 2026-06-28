package mx.utng.ich.safecare.wearable.data.remote

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseClient {
    // TODO: Reemplaza estas credenciales con las de tu proyecto de Supabase
    private const val SUPABASE_URL = "https://TU_PROYECTO.supabase.co"
    private const val SUPABASE_KEY = "TU_ANON_KEY_AQUI"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Postgrest)
        install(Auth)
        install(Realtime)
    }
}
