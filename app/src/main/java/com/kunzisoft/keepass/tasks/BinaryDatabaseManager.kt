package com.kunzisoft.keepass.tasks

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.binary.BinaryCache
import com.kunzisoft.keepass.database.element.binary.BinaryData
import com.kunzisoft.keepass.utils.getUriInputStream
import com.kunzisoft.keepass.utils.getUriOutputStream
import com.kunzisoft.keepass.utils.readAllBytes
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

    fun downloadFromDatabase(database: ContextualDatabase,
                             attachmentToUploadUri: Uri,
                             binaryData: BinaryData,
                             contentResolver: ContentResolver,
                             update: ((percent: Int)->Unit)? = null,
                             canceled: ()-> Boolean = { false },
                             bufferSize: Int = DEFAULT_BUFFER_SIZE) {
        contentResolver.getUriOutputStream(attachmentToUploadUri)?.use { outputStream ->
            downloadFromDatabase(database.binaryCache, outputStream, binaryData, update, canceled, bufferSize)
        }
    }

    private fun downloadFromDatabase(binaryCache: BinaryCache,
                                     outputStream: OutputStream,
                                     binaryData: BinaryData,
                                     update: ((percent: Int)->Unit)? = null,
                                     canceled: ()-> Boolean = { false },
                                     bufferSize: Int = DEFAULT_BUFFER_SIZE) {
        val fileSize = binaryData.getSize()
        var dataDownloaded = 0L
        binaryData.getUnGzipInputDataStream(binaryCache).use { inputStream ->
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

    fun uploadToDatabase(database: ContextualDatabase,
                         attachmentFromDownloadUri: Uri,
                         binaryData: BinaryData,
                         contentResolver: ContentResolver,
                         update: ((percent: Int)->Unit)? = null,
                         canceled: ()-> Boolean = { false },
                         bufferSize: Int = DEFAULT_BUFFER_SIZE) {
        val fileSize = contentResolver.openFileDescriptor(attachmentFromDownloadUri, "r")?.statSize ?: 0
        contentResolver.getUriInputStream(attachmentFromDownloadUri)?.use { inputStream ->
            uploadToDatabase(database.binaryCache, inputStream, fileSize, binaryData, update, canceled, bufferSize)
        }
    }

    private fun uploadToDatabase(binaryCache: BinaryCache,
                                 inputStream: InputStream,
                                 fileSize: Long,
                                 binaryData: BinaryData,
                                 update: ((percent: Int)->Unit)? = null,
                                 canceled: ()-> Boolean = { false },
                                 bufferSize: Int = DEFAULT_BUFFER_SIZE) {
        var dataUploaded = 0L
        binaryData.getGzipOutputDataStream(binaryCache).use { outputStream ->
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

    fun resizeBitmapAndStoreDataInBinaryFile(contentResolver: ContentResolver,
                                             database: ContextualDatabase,
                                             bitmapUri: Uri?,
                                             binaryData: BinaryData?) {
        try {
            binaryData?.let {
                contentResolver.getUriInputStream(bitmapUri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)?.let { bitmap ->
                        val bitmapResized = bitmap.resize(DEFAULT_ICON_WIDTH)
                        val byteArrayOutputStream = ByteArrayOutputStream()
                        bitmapResized?.compress(Bitmap.CompressFormat.PNG, 0, byteArrayOutputStream)
                        val bitmapData: ByteArray = byteArrayOutputStream.toByteArray()
                        val byteArrayInputStream = ByteArrayInputStream(bitmapData)
                        uploadToDatabase(
                                database.binaryCache,
                                byteArrayInputStream,
                                bitmapData.size.toLong(),
                                binaryData
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

    fun loadBitmap(database: ContextualDatabase,
                   binaryData: BinaryData,
                   maxWidth: Int,
                   actionOnFinish: (Bitmap?) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                val asyncResult: Deferred<Bitmap?> = async {
                    runCatching {
                        val bitmap: Bitmap? = decodeSampledBitmap(binaryData,
                                database.binaryCache,
                                maxWidth)
                        bitmap
                    }.getOrNull()
                }
                withContext(Dispatchers.Main) {
                    actionOnFinish(asyncResult.await())
                }
            }
        }
    }

    private fun decodeSampledBitmap(binaryData: BinaryData,
                                    binaryCache: BinaryCache,
                                    maxWidth: Int): Bitmap? {
        // First decode with inJustDecodeBounds=true to check dimensions
        return BitmapFactory.Options().run {
            try {
                inJustDecodeBounds = true
                binaryData.getUnGzipInputDataStream(binaryCache).use {
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
                binaryData.getUnGzipInputDataStream(binaryCache).use {
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