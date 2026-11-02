package io.github.eyalshimshi.betterstips.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream

/** Prepares a picked photo for Stips: upright, center square, at most [maxSize] px, JPEG. Call off the main thread. */
object ImagePrep {
    fun squareJpeg(context: Context, uri: Uri, maxSize: Int = 720): Pair<ByteArray, Int> {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: error("Can't read the photo")
        val shortest = minOf(bounds.outWidth, bounds.outHeight)
        if (shortest <= 0) error("Not a photo")
        var sample = 1
        while (shortest / (sample * 2) >= maxSize) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Can't decode the photo")
        val orientation = try {
            resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        }
        val upright = if (matrix.isIdentity) decoded else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        val side = minOf(upright.width, upright.height)
        val cropped = Bitmap.createBitmap(upright, (upright.width - side) / 2, (upright.height - side) / 2, side, side)
        val square = if (side > maxSize) Bitmap.createScaledBitmap(cropped, maxSize, maxSize, true) else cropped
        val out = ByteArrayOutputStream()
        square.compress(Bitmap.CompressFormat.JPEG, 90, out)
        return out.toByteArray() to square.width
    }
}
