package com.keymusicman.meshgradientplayground

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Writes this image to [uri] as a PNG, lossless and with the alpha channel intact.
 *
 * @return whether the file was written; the caller is expected to tell the user either way.
 */
internal suspend fun ImageBitmap.writePngTo(context: Context, uri: Uri): Boolean {
    val bitmap = asAndroidBitmap()
    return withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            } ?: false
        } catch (_: IOException) {
            false
        }
    }
}

/**
 * Writes the image into the app's shared cache and returns a `content://` [Uri] for it. Other apps
 * cannot read a `file://` path of ours, so sharing has to go through the [FileProvider] declared in
 * the manifest.
 *
 * The file name is reused on every share, so the cache holds one exported gradient at most.
 */
internal suspend fun ImageBitmap.toShareablePngUri(context: Context): Uri? {
    val file = File(context.cacheDir, "shared/mesh-gradient.png")
    val written = withContext(Dispatchers.IO) {
        try {
            file.parentFile?.mkdirs()
            true
        } catch (_: IOException) {
            false
        }
    } && writePngTo(context, Uri.fromFile(file))
    if (!written) return null
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

internal fun sharePngIntent(uri: Uri): Intent = Intent(Intent.ACTION_SEND).apply {
    type = "image/png"
    putExtra(Intent.EXTRA_STREAM, uri)
    // The clip data both carries the grant and gives the share sheet its preview thumbnail.
    clipData = ClipData.newRawUri("mesh-gradient.png", uri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}
