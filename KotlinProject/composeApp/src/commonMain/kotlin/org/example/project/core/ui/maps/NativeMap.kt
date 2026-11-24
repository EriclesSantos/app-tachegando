package org.example.project.core.ui.maps

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import org.example.project.core.data.VendorMapMarker
import org.example.project.core.utils.Location
import org.example.project.getPlatform

data class HeatZoneLocation(
    val location: Location,
    val intensity: Float, // 0.0 to 1.0
    val radius: Double = 1000.0 // meters
)

@Composable
expect fun NativeMap(
    modifier: Modifier = Modifier,
    userLocation: Location? = null,
    vendorLocations: List<Location> = emptyList(),
    routePoints: List<Location> = emptyList(),
    heatZones: List<HeatZoneLocation> = emptyList(),
    onMapClick: (Location) -> Unit = {},
    activeVendors: List<VendorMapMarker> = emptyList(),
    onVendorClick: (String) -> Unit = {}
)

@Composable
internal fun Modifier.reserveSpaceAroundHtmlMap(top: Int = 0, bottom: Int = 0): Modifier {
    if (!getPlatform().embedsHtmlMap) return this
    // HTML map layers occupy their own browser surface and would cover Compose controls.
    return with(LocalDensity.current) { padding(top = top.toDp(), bottom = bottom.toDp()) }
}
