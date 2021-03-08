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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

object BinaryStreamManager {

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

    fun downloadFromDatabase(outputStream: OutputStream,
                             binaryFile: BinaryFile,
                             update: ((percent: Int)->Unit)? = null,
                             canceled: ()-> Boolean = { false },
                             bufferSize: Int = DEFAULT_BUFFER_SIZE) {
        val fileSize = binaryFile.length
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
                        Log.e(TAG, "", e)
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

    // TODO Better implementation
    fun uploadToDatabase(inputStream: InputStream,
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
                        Log.e(TAG, "", e)
                    }
                }
            }
        }
    }

    fun resizeBitmapAndStoreDataInBinaryFile(contentResolver: ContentResolver,
                                             bitmapUri: Uri?,
                                             binaryFile: BinaryFile) {
        UriUtil.getUriInputStream(contentResolver, bitmapUri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)?.let { bitmap ->
                val bitmapResized = bitmap.resize(MAX_ICON_SIZE)
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


    /**
     * reduces the size of the image
     * @param image
     * @param maxSize
     * @return
     */
    fun Bitmap.resize(maxSize: Int): Bitmap? {
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

    private const val MAX_ICON_SIZE = 128

    private val TAG = BinaryStreamManager::class.java.name
}