@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.example.project.core.ui.maps

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.WebElementView
import kotlinx.browser.document
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.example.project.core.data.VendorMapMarker
import org.example.project.core.utils.Location
import org.w3c.dom.HTMLElement

private fun createWebMap(
    element: HTMLElement,
    onMapClick: (Double, Double) -> Unit,
    onVendorClick: (String) -> Unit
): Unit = js("window.tachegandoMap.mount(element, onMapClick, onVendorClick)")

private fun updateWebMap(element: HTMLElement, data: String): Unit =
    js("window.tachegandoMap.update(element, data)")

private fun releaseWebMap(element: HTMLElement): Unit = js("window.tachegandoMap.release(element)")

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun NativeMap(
    modifier: Modifier,
    userLocation: Location?,
    vendorLocations: List<Location>,
    routePoints: List<Location>,
    heatZones: List<HeatZoneLocation>,
    onMapClick: (Location) -> Unit,
    activeVendors: List<VendorMapMarker>,
    onVendorClick: (String) -> Unit
) {
    val currentMapClick by rememberUpdatedState(onMapClick)
    val currentVendorClick by rememberUpdatedState(onVendorClick)
    val mapData = buildJsonObject {
        userLocation?.let { put("user", it.toMapCoordinates()) }
        put("locations", buildJsonArray { vendorLocations.forEach { add(it.toMapCoordinates()) } })
        put("route", buildJsonArray { routePoints.forEach { add(it.toMapCoordinates()) } })
        put("vendors", buildJsonArray {
            activeVendors.forEach { marker ->
                add(buildJsonObject {
                    put("id", marker.vendorId)
                    put("name", marker.name)
                    put("location", marker.location.toMapCoordinates())
                    marker.transport?.let { put("symbol", it.symbol) }
                })
            }
        })
        put("heatZones", buildJsonArray {
            heatZones.forEach { zone ->
                add(buildJsonObject {
                    put("location", zone.location.toMapCoordinates())
                    put("intensity", zone.intensity)
                    put("radius", zone.radius)
                })
            }
        })
    }.toString()

    WebElementView(
        factory = {
            (document.createElement("div") as HTMLElement).apply {
                className = "tachegando-map"
                createWebMap(this, { latitude, longitude ->
                    currentMapClick(Location(latitude, longitude))
                }, { currentVendorClick(it) })
            }
        },
        modifier = modifier,
        onRelease = ::releaseWebMap,
        update = { updateWebMap(it, mapData) }
    )
}

private fun Location.toMapCoordinates() = buildJsonArray {
    add(latitude)
    add(longitude)
}
