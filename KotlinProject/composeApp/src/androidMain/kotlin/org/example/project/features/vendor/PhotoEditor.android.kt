package org.example.project.features.vendor

import java.io.ByteArrayOutputStream

import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import org.example.project.core.data.MAX_PHOTO_BYTES

@Composable
internal actual fun rememberProductPhotoPicker(
    onPhoto: (ByteArray) -> Unit,
    onError: (String) -> Unit
): (() -> Unit)? {
    val resolver = LocalContext.current.contentResolver
    val scope = rememberCoroutineScope()
    val deliver = rememberUpdatedState(onPhoto)
    val fail = rememberUpdatedState(onError)
    val readImage: (Uri?) -> Unit = { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val bytes = withContext(Dispatchers.IO) { encodePhoto(resolver, uri) }
                    deliver.value(bytes)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    fail.value("Não foi possível abrir esta imagem. Escolha outra foto.")
                }
            }
        }
    }
    val documents = rememberLauncherForActivityResult(OpenDocument(), onResult = readImage)
    val gallery = rememberLauncherForActivityResult(GetContent(), onResult = readImage)
    return {
        if (!openPicker { documents.launch(arrayOf("image/*")) } &&
            !openPicker { gallery.launch("image/*") }
        ) {
            fail.value("Não foi possível abrir a galeria neste aparelho.")
        }
    }
}

private inline fun openPicker(launch: () -> Unit): Boolean = try {
    launch()
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: SecurityException) {
    false
} catch (_: IllegalStateException) {
    false
}

private const val MAX_SIDE = 512

private fun encodePhoto(resolver: ContentResolver, uri: Uri): ByteArray {
    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
            val largest = maxOf(info.size.width, info.size.height)
            if (largest > MAX_SIDE) {
                val scale = MAX_SIDE.toFloat() / largest
                decoder.setTargetSize(
                    (info.size.width * scale).toInt().coerceAtLeast(1),
                    (info.size.height * scale).toInt().coerceAtLeast(1)
                )
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        readLegacyPhoto(resolver, uri)
    }
    return try {
        val output = ByteArrayOutputStream()
        check(bitmap.compress(Bitmap.CompressFormat.JPEG, 78, output))
        if (output.size() > MAX_PHOTO_BYTES) {
            output.reset()
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 55, output))
        }
        check(output.size() <= MAX_PHOTO_BYTES)
        output.toByteArray()
    } finally {
        bitmap.recycle()
    }
}

private fun readLegacyPhoto(resolver: ContentResolver, uri: Uri): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE * 2) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    val bitmap = checkNotNull(resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) })
    val orientation = runCatching {
        resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
    }.getOrNull()
    val transform = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
        }
        val scale = (MAX_SIDE.toFloat() / maxOf(bitmap.width, bitmap.height)).coerceAtMost(1f)
        postScale(scale, scale)
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, transform, true).also {
        if (it !== bitmap) bitmap.recycle()
    }
}
