package org.example.project.core.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.logging.LogLevel
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

// O site distribui a mesma chave publicável deste projeto. Permissões de dados são aplicadas via RLS.
const val SUPABASE_URL = "https://vhnkezwjknwpddqhlnfk.supabase.co"
const val SUPABASE_PUBLISHABLE_KEY = "sb_publishable_s_VPZ1wyZIbsxZ6re6s_5Q_ZIH4ER9V"

internal expect fun platformSessionManager(): SessionManager?

val supabase = createSupabaseClient(
    supabaseUrl = SUPABASE_URL,
    supabaseKey = SUPABASE_PUBLISHABLE_KEY
) {
    // Exceções do SDK podem incluir cabeçalhos de autenticação.
    Auth.setLogLevel(LogLevel.NONE)
    install(Auth) {
        val platformManager = platformSessionManager()
        if (platformManager != null) {
            sessionManager = platformManager
            autoLoadFromStorage = false
        }
        if (supportsGoogleLogin) {
            flowType = FlowType.PKCE
            if (platformManager == null) sessionManager = MemorySessionManager()
        }
        if (supportsGoogleLogin || platformManager != null) {
            codeVerifierCache = MemoryCodeVerifierCache()
        }
    }
    install(Postgrest)
    install(Realtime)
}
