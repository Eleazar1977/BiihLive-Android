package com.mision.biihlive.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

class ImageProcessor(private val context: Context) {

    data class ProcessedImages(
        val fullImageBytes: ByteArray,
        val thumbnailBytes: ByteArray
    )

    /**
     * Procesa imagen para PERFIL DE USUARIO
     * - Full: 1024x1024
     * - Thumbnail: 150x150
     */
    suspend fun processImage(uri: Uri): Result<ProcessedImages> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open image"))

            // Decode with efficient memory usage
            val bitmap = decodeBitmapEfficiently(inputStream, 2048)
                ?: return@withContext Result.failure(Exception("Cannot decode image"))

            // Apply EXIF rotation
            val rotatedBitmap = applyExifRotation(uri, bitmap)

            // Generate full size (max 1024x1024)
            val fullBitmap = resizeBitmap(rotatedBitmap, 1024, 1024)
            val fullBytes = compressBitmapToJpeg(fullBitmap, 85)

            // Generate thumbnail (150x150)
            val thumbnailBitmap = resizeBitmapCenterCrop(rotatedBitmap, 150, 150)
            val thumbnailBytes = compressBitmapToJpeg(thumbnailBitmap, 80)

            // Clean up bitmaps
            if (rotatedBitmap != bitmap) rotatedBitmap.recycle()
            fullBitmap.recycle()
            thumbnailBitmap.recycle()
            bitmap.recycle()

            Result.success(ProcessedImages(fullBytes, thumbnailBytes))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun decodeBitmapEfficiently(inputStream: InputStream, maxSize: Int): Bitmap? {
        // First decode with inJustDecodeBounds=true to check dimensions
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        val bytes = inputStream.readBytes()
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

        // Calculate sample size
        options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun applyExifRotation(uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val exif = ExifInterface(inputStream!!)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            inputStream.close()

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }

            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            rotatedBitmap ?: bitmap
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false)
        }

        val aspectRatio = width.toFloat() / height.toFloat()
        val (newWidth, newHeight) = if (aspectRatio > 1) {
            // Landscape
            val w = min(maxWidth, width)
            val h = (w / aspectRatio).toInt()
            w to h
        } else {
            // Portrait or square
            val h = min(maxHeight, height)
            val w = (h * aspectRatio).toInt()
            w to h
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun resizeBitmapCenterCrop(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val sourceWidth = bitmap.width
        val sourceHeight = bitmap.height

        val xScale = targetWidth.toFloat() / sourceWidth
        val yScale = targetHeight.toFloat() / sourceHeight
        val scale = max(xScale, yScale)

        val scaledWidth = (scale * sourceWidth).toInt()
        val scaledHeight = (scale * sourceHeight).toInt()

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)

        val startX = (scaledWidth - targetWidth) / 2
        val startY = (scaledHeight - targetHeight) / 2

        val croppedBitmap = Bitmap.createBitmap(scaledBitmap, startX, startY, targetWidth, targetHeight)

        if (scaledBitmap != croppedBitmap) {
            scaledBitmap.recycle()
        }

        return croppedBitmap
    }

    /**
     * Procesa imagen para GALERÍA
     * - Full: 1920x1920 máximo
     * - Thumbnail: 300x300 cuadrado
     */
    suspend fun processImageForGallery(uri: Uri): Result<ProcessedImages> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open image"))

            // Decode with efficient memory usage
            val bitmap = decodeBitmapEfficiently(inputStream, 2048)
                ?: return@withContext Result.failure(Exception("Cannot decode image"))

            // Apply EXIF rotation
            val rotatedBitmap = applyExifRotation(uri, bitmap)

            // Generate full size (max 1920x1920)
            val fullBitmap = resizeBitmap(rotatedBitmap, 1920, 1920)
            val fullBytes = compressBitmapToPng(fullBitmap)

            // Generate thumbnail (300x300 square)
            val thumbnailBitmap = resizeBitmapCenterCrop(rotatedBitmap, 300, 300)
            val thumbnailBytes = compressBitmapToPng(thumbnailBitmap)

            // Clean up bitmaps
            if (rotatedBitmap != bitmap) rotatedBitmap.recycle()
            fullBitmap.recycle()
            thumbnailBitmap.recycle()
            bitmap.recycle()

            Result.success(ProcessedImages(fullBytes, thumbnailBytes))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun compressBitmapToJpeg(bitmap: Bitmap, quality: Int): ByteArray {
        val outputStream = ByteArrayOutputStream()
        // Usar PNG en lugar de JPEG para evitar problemas de caché
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream) // PNG no usa quality
        return outputStream.toByteArray()
    }

    private fun compressBitmapToPng(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }
}