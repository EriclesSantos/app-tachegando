package org.example.project.core.ui.maps

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import org.example.project.getPlatform

@Composable
internal fun Modifier.reserveSpaceAroundHtmlMap(top: Int = 0, bottom: Int = 0): Modifier {
    if (!getPlatform().embedsHtmlMap) return this
    // HTML map layers occupy their own browser surface and would cover Compose controls.
    return with(LocalDensity.current) { padding(top = top.toDp(), bottom = bottom.toDp()) }
}
