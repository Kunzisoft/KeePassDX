package com.kunzisoft.keepass.tasks

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.database.BinaryFile
import com.kunzisoft.keepass.stream.readAllBytes
import com.kunzisoft.keepass.utils.UriUtil
import kotlinx.coroutines.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow

object BinaryDatabaseManager {

    fun downloadFromDatabase(attachmentToUploadUri: Uri,
                             binaryFile: BinaryFile,
                             contentResolver: ContentResolver,
                             update: ((percent: Int)->Unit)? = null,
                             canceled: ()-> Boolean = { false },
                             bufferSize: Int = DEFAULT_BUFFER_SIZE) {
        UriUtil.getUriOutputStream(contentResolver, attachmentToUploadUri)?.use { outputStream ->
            downloadFromDatabase(outputStream, binaryFile, update, canceled, bufferSize)
        }
    }

    private fun downloadFromDatabase(outputStream: OutputStream,
                             binaryFile: BinaryFile,
                             update: ((percent: Int)->Unit)? = null,
                             canceled: ()-> Boolean = { false },
                             bufferSize: Int = DEFAULT_BUFFER_SIZE) {
        val fileSize = binaryFile.getSize()
        var dataDownloaded = 0L
        Database.getInstance().loadedCipherKey?.let { binaryCipherKey ->
            binaryFile.getUnGzipInputDataStream(binaryCipherKey).use { inputStream ->
                inputStream.readAllBytes(bufferSize, canceled) { buffer ->
                    outputStream.write(buffer)
                    dataDownloaded += buffer.size
                    try {
                        val percentDownload = (100 * dataDownloaded / fileSize).toInt()
                        update?.invoke(percentDownload)
                    } catch (e: Exception) {
                        Log.w(TAG, "Unable to call update callback during download", e)
                    }
                }
            }
        }
    }

    fun uploadToDatabase(attachmentFromDownloadUri: Uri,
                         binaryFile: BinaryFile,
                         contentResolver: ContentResolver,
                         update: ((percent: Int)->Unit)? = null,
                         canceled: ()-> Boolean = { false },
                         bufferSize: Int = DEFAULT_BUFFER_SIZE) {
        val fileSize = contentResolver.openFileDescriptor(attachmentFromDownloadUri, "r")?.statSize ?: 0
        UriUtil.getUriInputStream(contentResolver, attachmentFromDownloadUri)?.use { inputStream ->
            uploadToDatabase(inputStream, fileSize, binaryFile, update, canceled, bufferSize)
        }
    }

    private fun uploadToDatabase(inputStream: InputStream,
                         fileSize: Long,
                         binaryFile: BinaryFile,
                         update: ((percent: Int)->Unit)? = null,
                         canceled: ()-> Boolean = { false },
                         bufferSize: Int = DEFAULT_BUFFER_SIZE) {
        var dataUploaded = 0L
        Database.getInstance().loadedCipherKey?.let { binaryCipherKey ->
            binaryFile.getGzipOutputDataStream(binaryCipherKey).use { outputStream ->
                inputStream.readAllBytes(bufferSize, canceled) { buffer ->
                    outputStream.write(buffer)
                    dataUploaded += buffer.size
                    try {
                        val percentDownload = (100 * dataUploaded / fileSize).toInt()
                        update?.invoke(percentDownload)
                    } catch (e: Exception) {
                        Log.w(TAG, "Unable to call update callback during upload", e)
                    }
                }
            }
        }
    }

    fun resizeBitmapAndStoreDataInBinaryFile(contentResolver: ContentResolver,
                                             bitmapUri: Uri?,
                                             binaryFile: BinaryFile?) {
        try {
            binaryFile?.let {
                UriUtil.getUriInputStream(contentResolver, bitmapUri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)?.let { bitmap ->
                        val bitmapResized = bitmap.resize(DEFAULT_ICON_WIDTH)
                        val byteArrayOutputStream = ByteArrayOutputStream()
                        bitmapResized?.compress(Bitmap.CompressFormat.PNG, 0, byteArrayOutputStream)
                        val bitmapData: ByteArray = byteArrayOutputStream.toByteArray()
                        val byteArrayInputStream = ByteArrayInputStream(bitmapData)
                        uploadToDatabase(
                                byteArrayInputStream,
                                bitmapData.size.toLong(),
                                binaryFile
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to resize bitmap to store it in binary", e)
        }
    }


    /**
     * reduces the size of the image
     * @param image
     * @param maxSize
     * @return
     */
    private fun Bitmap.resize(maxSize: Int): Bitmap? {
        var width = this.width
        var height = this.height
        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = maxSize
            height = (width / bitmapRatio).toInt()
        } else {
            height = maxSize
            width = (height * bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(this, width, height, true)
    }

    fun loadBitmap(binaryFile: BinaryFile,
                   binaryCipherKey: Database.LoadedKey?,
                   maxWidth: Int,
                   actionOnFinish: (Bitmap?) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                val asyncResult: Deferred<Bitmap?> = async {
                    runCatching {
                        binaryCipherKey?.let { binaryKey ->
                            val bitmap: Bitmap? = decodeSampledBitmap(binaryFile,
                                    binaryKey,
                                    maxWidth)
                            bitmap
                        }
                    }.getOrNull()
                }
                withContext(Dispatchers.Main) {
                    actionOnFinish(asyncResult.await())
                }
            }
        }
    }

    private fun decodeSampledBitmap(binaryFile: BinaryFile,
                                    binaryCipherKey: Database.LoadedKey,
                                    maxWidth: Int): Bitmap? {
        // First decode with inJustDecodeBounds=true to check dimensions
        return BitmapFactory.Options().run {
            try {
                inJustDecodeBounds = true
                binaryFile.getUnGzipInputDataStream(binaryCipherKey).use {
                    BitmapFactory.decodeStream(it, null, this)
                }
                // Calculate inSampleSize
                var scale = 1
                if (outHeight > maxWidth || outWidth > maxWidth) {
                    scale = 2.0.pow(ceil(ln(maxWidth / max(outHeight, outWidth).toDouble()) / ln(0.5))).toInt()
                }
                inSampleSize = scale

                // Decode bitmap with inSampleSize set
                inJustDecodeBounds = false
                binaryFile.getUnGzipInputDataStream(binaryCipherKey).use {
                    BitmapFactory.decodeStream(it, null, this)
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private const val DEFAULT_ICON_WIDTH = 64

    private val TAG = BinaryDatabaseManager::class.java.name
}