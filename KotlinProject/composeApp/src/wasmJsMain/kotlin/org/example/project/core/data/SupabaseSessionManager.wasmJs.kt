package org.example.project.core.data

import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.SessionManager

// Keep web sessions in their own tabs so startup does not depend on browser storage.
internal actual fun platformSessionManager(): SessionManager = MemorySessionManager()
