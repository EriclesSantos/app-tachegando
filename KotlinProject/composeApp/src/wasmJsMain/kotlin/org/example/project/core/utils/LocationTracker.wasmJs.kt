@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.example.project.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private fun hasBrowserGeolocation(): Boolean =
    js("window.isSecureContext && !!navigator.geolocation")

private fun isBrowserPageVisible(): Boolean = js("document.visibilityState === 'visible'")

private fun readBrowserPosition(
    onPosition: (Double, Double) -> Unit,
    onError: (Int) -> Unit
): Unit = js("""
    {
        navigator.geolocation.getCurrentPosition(
            position => {
                if (document.visibilityState === 'visible') {
                    onPosition(position.coords.latitude, position.coords.longitude);
                } else {
                    onError(2);
                }
            },
            error => onError(error.code),
            { enableHighAccuracy: true, maximumAge: 0, timeout: 12000 }
        );
    }
""")

private class WasmLocationTracker(private val scope: CoroutineScope) : LocationTracker {
    private var trackingJob: Job? = null
    override val supportsTracking: Boolean get() = hasBrowserGeolocation()

    override suspend fun requestLocationPermission(): Boolean =
        supportsTracking && readFix().errorCode != 1

    override suspend fun getCurrentLocation(): Location? =
        if (supportsTracking) readFix().location else null

    override fun startTracking(onLocationUpdate: (Location) -> Unit) {
        stopTracking()
        if (!supportsTracking) return
        trackingJob = scope.launch {
            while (isActive) {
                if (isBrowserPageVisible()) {
                    val fix = readFix()
                    if (fix.errorCode == 1) break
                    fix.location?.let(onLocationUpdate)
                }
                delay(5_000)
            }
        }
    }

    override fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
    }

    private suspend fun readFix(): BrowserFix = suspendCancellableCoroutine { continuation ->
        readBrowserPosition(
            onPosition = { latitude, longitude ->
                if (continuation.isActive) continuation.resume(BrowserFix(Location(latitude, longitude)))
            },
            onError = { code ->
                if (continuation.isActive) continuation.resume(BrowserFix(errorCode = code))
            }
        )
    }

    private data class BrowserFix(val location: Location? = null, val errorCode: Int? = null)
}

@Composable
actual fun rememberLocationTracker(): LocationTracker {
    val scope = rememberCoroutineScope()
    return remember(scope) { WasmLocationTracker(scope) }
}
