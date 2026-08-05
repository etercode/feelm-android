package org.feelm.app.ui.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

// Decode ceiling, not the upload size: the cropper needs headroom to zoom
// into, so a 1024px working copy leaves a 512 crop still sharp at 2x.
private const val DECODE_SIZE = 1024

/**
 * Decode a picked image at a workable size for the cropper.
 *
 * The picker hands back whatever the camera shot — a 12 megapixel portrait is
 * about five megabytes, and neither the cropper nor the server has any use for
 * that many pixels.
 *
 * Decoded with `inSampleSize` so the full-size bitmap never exists in memory —
 * decoding a large photo at full resolution to immediately shrink it is how an
 * avatar picker earns an OutOfMemoryError on a cheap device.
 */
fun decodeForCrop(context: Context, uri: Uri): Bitmap? {
    val resolver = context.contentResolver

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
    }
    val decoded = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    } ?: return null

    return decoded
}

/** JPEG bytes for upload, once a crop has been chosen. */
fun Bitmap.toJpegBytes(quality: Int = 88): ByteArray =
    ByteArrayOutputStream().use { out ->
        compress(Bitmap.CompressFormat.JPEG, quality, out)
        out.toByteArray()
    }

/** The largest power of two that still leaves more pixels than we need. */
private fun sampleSizeFor(width: Int, height: Int): Int {
    var sample = 1
    val shortest = max(1, min(width, height))
    while (shortest / (sample * 2) >= DECODE_SIZE) {
        sample *= 2
    }
    return sample
}
