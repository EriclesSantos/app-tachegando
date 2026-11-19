package org.example.project.features.vendor

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.example.project.core.data.PhotoChange

@Composable
internal expect fun rememberProductPhotoPicker(
    onPhoto: (ByteArray) -> Unit,
    onError: (String) -> Unit
): (() -> Unit)?

@Composable
internal fun PhotoEditor(
    label: String,
    current: ByteArray?,
    change: PhotoChange?,
    busy: Boolean,
    onChange: (PhotoChange) -> Unit
) {
    var error by remember { mutableStateOf<String?>(null) }
    val picker = rememberProductPhotoPicker(
        onPhoto = { error = null; onChange(PhotoChange(it)) },
        onError = { error = it }
    )
    val photo = if (change == null) current else change.bytes
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        PhotoPreview(photo, label, Modifier.size(96.dp))
        if (picker != null) {
            Row {
                OutlinedButton(onClick = { error = null; picker() }, enabled = !busy) {
                    Text(if (photo == null) "Selecionar foto" else "Trocar foto")
                }
                if (photo != null || change != null) {
                    TextButton(onClick = { onChange(PhotoChange(null)) }, enabled = !busy && photo != null) {
                        Text("Remover foto")
                    }
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
internal fun PhotoPreview(bytes: ByteArray?, description: String?, modifier: Modifier) {
    val image = remember(bytes) { bytes?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() } }
    if (image != null) {
        Image(image, description, modifier.clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
    }
}
