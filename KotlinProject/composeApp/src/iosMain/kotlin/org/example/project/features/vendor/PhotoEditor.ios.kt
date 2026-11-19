package org.example.project.features.vendor

import androidx.compose.runtime.Composable

@Composable
internal actual fun rememberProductPhotoPicker(
    onPhoto: (ByteArray) -> Unit,
    onError: (String) -> Unit
): (() -> Unit)? = null
