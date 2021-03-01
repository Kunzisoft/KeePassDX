package com.kunzisoft.keepass.tasks

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.database.BinaryFile
import com.kunzisoft.keepass.stream.readAllBytes
import com.kunzisoft.keepass.utils.UriUtil

object BinaryStreamManager {

    fun downloadFromDatabase(attachmentToUploadUri: Uri,
                             binaryFile: BinaryFile,
                             contentResolver: ContentResolver,
                             update: ((percent: Int)->Unit)? = null,
                             canceled: ()-> Boolean = { false },
                             bufferSize: Int = DEFAULT_BUFFER_SIZE) {
        var dataDownloaded = 0L
        val fileSize = binaryFile.length
        UriUtil.getUriOutputStream(contentResolver, attachmentToUploadUri)?.use { outputStream ->
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
    }

    fun uploadToDatabase(attachmentFromDownloadUri: Uri,
                         binaryFile: BinaryFile,
                         contentResolver: ContentResolver,
                         update: ((percent: Int)->Unit)? = null,
                         canceled: ()-> Boolean = { false },
                         bufferSize: Int = DEFAULT_BUFFER_SIZE) {
        var dataUploaded = 0L
        val fileSize = contentResolver.openFileDescriptor(attachmentFromDownloadUri, "r")?.statSize ?: 0
        UriUtil.getUriInputStream(contentResolver, attachmentFromDownloadUri)?.use { inputStream ->
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
    }

    private val TAG = BinaryStreamManager::class.java.name
}