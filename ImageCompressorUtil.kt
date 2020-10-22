package com.wezom.haulk.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@Suppress("BlockingMethodInNonBlockingContext")
object ImageCompressorUtil {
    private const val JPEG_FORMAT = ".jpeg"
    private const val JPG_FORMAT = ".jpg"
    private const val PNG_FORMAT = ".png"

    private const val MAX_QUALITY = 100
    private const val QUALITY_DECREMENT_STEP = 20

    private const val DEFAULT_SIZE_KB = 999


    suspend fun compressPhotoFile(photoFile: File, compressSizeKb: Int = DEFAULT_SIZE_KB): Boolean = withContext(Dispatchers.IO) {
        //check is photo file format valid
        if (!isPhotoFormatCorrect(photoFile.path)) return@withContext false

        //check is current file need compress
        val photoFileSize = photoFile.length().byteToKb()
        if (photoFileSize <= compressSizeKb) return@withContext false

        var imageQuality = MAX_QUALITY
        val image = BitmapFactory.decodeFile(photoFile.absolutePath)
        val compressingDone = false
        while (!compressingDone) {
            // save exif orientation metadata for this photo file
            val oldExif = ExifInterface(photoFile.path)
            val exifOrientation = oldExif.getAttribute(ExifInterface.TAG_ORIENTATION)

            val bytes = ByteArrayOutputStream()
            image.compress(Bitmap.CompressFormat.JPEG, imageQuality, bytes)
            val newImage = File(photoFile.path)
            newImage.createNewFile()
            val outStream = FileOutputStream(newImage)
            outStream.write(bytes.toByteArray())
            outStream.close()

            //update exif metadata for new photo file
            if (exifOrientation != null) {
                val newExif = ExifInterface(photoFile.path)
                newExif.setAttribute(ExifInterface.TAG_ORIENTATION, exifOrientation)
                newExif.saveAttributes()
            }

            val imageSize = photoFile.length().byteToKb()// size in kbs
            Timber.d("size: $imageSize")

            // decrease quality if size is bigger than target
            if (imageSize > compressSizeKb)
                imageQuality -= QUALITY_DECREMENT_STEP
            else {
                return@withContext true
            }
            if (imageQuality <= QUALITY_DECREMENT_STEP) {
                return@withContext false
            }
        }
        return@withContext compressingDone
    }

    private fun isPhotoFormatCorrect(filePath: String): Boolean {
        return (filePath.contains(JPEG_FORMAT) || filePath.contains(JPG_FORMAT) || filePath.contains(PNG_FORMAT))
    }

    private const val BYTE_IN_KB = 1024
    private fun Long.byteToKb() = this / BYTE_IN_KB
}