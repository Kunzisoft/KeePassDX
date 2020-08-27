/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.notifications

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.database.BinaryAttachment
import com.kunzisoft.keepass.model.AttachmentState
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.model.StreamDirection
import com.kunzisoft.keepass.stream.readBytes
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.UriUtil
import kotlinx.coroutines.*
import java.io.BufferedInputStream
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream


class AttachmentFileNotificationService: LockNotificationService() {

    override val notificationId: Int = 10000
    private val attachmentNotificationList = CopyOnWriteArrayList<AttachmentNotification>()

    private var mActionTaskBinder = ActionTaskBinder()
    private var mActionTaskListeners = LinkedList<ActionTaskListener>()

    private val mainScope = CoroutineScope(Dispatchers.Main)

    inner class ActionTaskBinder: Binder() {

        fun getService(): AttachmentFileNotificationService = this@AttachmentFileNotificationService

        fun addActionTaskListener(actionTaskListener: ActionTaskListener) {
            mActionTaskListeners.add(actionTaskListener)
            attachmentNotificationList.forEach {
                it.attachmentFileAction?.listener = attachmentFileActionListener
            }
        }

        fun removeActionTaskListener(actionTaskListener: ActionTaskListener) {
            attachmentNotificationList.forEach {
                it.attachmentFileAction?.listener = null
            }
            mActionTaskListeners.remove(actionTaskListener)
        }
    }

    private val attachmentFileActionListener = object: AttachmentFileAction.AttachmentFileActionListener {
        override fun onUpdate(attachmentNotification: AttachmentNotification) {
            newNotification(attachmentNotification)
            mActionTaskListeners.forEach { actionListener ->
                actionListener.onAttachmentAction(attachmentNotification.uri,
                        attachmentNotification.entryAttachmentState)
            }
        }
    }

    interface ActionTaskListener {
        fun onAttachmentAction(fileUri: Uri, entryAttachmentState: EntryAttachmentState)
    }

    override fun onBind(intent: Intent): IBinder? {
        return mActionTaskBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val downloadFileUri: Uri? = if (intent?.hasExtra(FILE_URI_KEY) == true) {
            intent.getParcelableExtra(FILE_URI_KEY)
        } else null

        when(intent?.action) {
            ACTION_ATTACHMENT_FILE_START_UPLOAD -> {
                actionUploadOrDownload(downloadFileUri,
                        intent,
                        StreamDirection.UPLOAD)
            }
            ACTION_ATTACHMENT_FILE_START_DOWNLOAD -> {
                actionUploadOrDownload(downloadFileUri,
                        intent,
                        StreamDirection.DOWNLOAD)
            }
            else -> {
                if (downloadFileUri != null) {
                    attachmentNotificationList.firstOrNull { it.uri == downloadFileUri }?.let { elementToRemove ->
                        notificationManager?.cancel(elementToRemove.notificationId)
                        attachmentNotificationList.remove(elementToRemove)
                    }
                }
                if (attachmentNotificationList.isEmpty()) {
                    stopSelf()
                }
            }
        }

        return START_REDELIVER_INTENT
    }

    @Synchronized
    fun checkCurrentAttachmentProgress() {
        attachmentNotificationList.forEach { attachmentNotification ->
            mActionTaskListeners.forEach { actionListener ->
                actionListener.onAttachmentAction(
                        attachmentNotification.uri,
                        attachmentNotification.entryAttachmentState
                )
            }
        }
    }

    @Synchronized
    fun removeAttachmentAction(entryAttachment: EntryAttachmentState) {
        attachmentNotificationList.firstOrNull {
            it.entryAttachmentState == entryAttachment
        }?.let {
            attachmentNotificationList.remove(it)
        }
    }

    private fun newNotification(attachmentNotification: AttachmentNotification) {

        val pendingContentIntent = PendingIntent.getActivity(this,
                0,
                Intent().apply {
                    action = Intent.ACTION_VIEW
                    setDataAndType(attachmentNotification.uri,
                            contentResolver.getType(attachmentNotification.uri))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, PendingIntent.FLAG_CANCEL_CURRENT)

        val pendingDeleteIntent =  PendingIntent.getService(this,
                0,
                Intent(this, AttachmentFileNotificationService::class.java).apply {
                    // No action to delete the service
                    putExtra(FILE_URI_KEY, attachmentNotification.uri)
                }, PendingIntent.FLAG_CANCEL_CURRENT)

        val fileName = DocumentFile.fromSingleUri(this, attachmentNotification.uri)?.name ?: ""

        val builder = buildNewNotification().apply {
            when (attachmentNotification.entryAttachmentState.streamDirection) {
                StreamDirection.UPLOAD -> {
                    setSmallIcon(R.drawable.ic_file_upload_white_24dp)
                    setContentTitle(getString(R.string.upload_attachment, fileName))
                }
                StreamDirection.DOWNLOAD -> {
                    setSmallIcon(R.drawable.ic_file_download_white_24dp)
                    setContentTitle(getString(R.string.download_attachment, fileName))
                }
            }
            setAutoCancel(false)
            when (attachmentNotification.entryAttachmentState.downloadState) {
                AttachmentState.NULL, AttachmentState.START -> {
                    setContentText(getString(R.string.download_initialization))
                    setOngoing(true)
                }
                AttachmentState.IN_PROGRESS -> {
                    if (attachmentNotification.entryAttachmentState.downloadProgression > 100) {
                        setContentText(getString(R.string.download_finalization))
                    } else {
                        setProgress(100,
                                attachmentNotification.entryAttachmentState.downloadProgression,
                                false)
                        setContentText(getString(R.string.download_progression,
                                attachmentNotification.entryAttachmentState.downloadProgression))
                    }
                    setOngoing(true)
                }
                AttachmentState.COMPLETE -> {
                    setContentText(getString(R.string.download_complete))
                    when (attachmentNotification.entryAttachmentState.streamDirection) {
                        StreamDirection.UPLOAD -> {

                        }
                        StreamDirection.DOWNLOAD -> {
                            setContentIntent(pendingContentIntent)
                        }
                    }
                    setDeleteIntent(pendingDeleteIntent)
                    setOngoing(false)
                }
                AttachmentState.ERROR -> {
                    setContentText(getString(R.string.error_file_not_create))
                    setOngoing(false)
                }
            }
        }
        when (attachmentNotification.entryAttachmentState.downloadState) {
            AttachmentState.ERROR,
            AttachmentState.COMPLETE -> {
                stopForeground(false)
                notificationManager?.notify(attachmentNotification.notificationId, builder.build())
            } else -> {
                startForeground(attachmentNotification.notificationId, builder.build())
            }
        }
    }

    override fun onDestroy() {
        attachmentNotificationList.forEach { attachmentNotification ->
            attachmentNotification.attachmentFileAction?.listener = null
            notificationManager?.cancel(attachmentNotification.notificationId)
        }
        attachmentNotificationList.clear()

        super.onDestroy()
    }

    private data class AttachmentNotification(var uri: Uri,
                                              var notificationId: Int,
                                              var entryAttachmentState: EntryAttachmentState,
                                              var attachmentFileAction: AttachmentFileAction? = null) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as AttachmentNotification

            if (notificationId != other.notificationId) return false

            return true
        }

        override fun hashCode(): Int {
            return notificationId
        }
    }

    private fun actionUploadOrDownload(downloadFileUri: Uri?,
                                       intent: Intent,
                                       streamDirection: StreamDirection) {
        if (downloadFileUri != null
                && intent.hasExtra(ATTACHMENT_KEY)) {
            try {
                intent.getParcelableExtra<Attachment>(ATTACHMENT_KEY)?.let { entryAttachment ->

                    val nextNotificationId = (attachmentNotificationList.maxByOrNull { it.notificationId }
                            ?.notificationId ?: notificationId) + 1
                    val entryAttachmentState = EntryAttachmentState(entryAttachment, streamDirection)
                    val attachmentNotification = AttachmentNotification(downloadFileUri, nextNotificationId, entryAttachmentState)
                    attachmentNotificationList.add(attachmentNotification)

                    mainScope.launch {
                        AttachmentFileAction(attachmentNotification,
                                contentResolver).apply {
                            listener = attachmentFileActionListener
                        }.executeAction()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unable to upload/download $downloadFileUri", e)
            }
        }
    }

    private class AttachmentFileAction(
            private val attachmentNotification: AttachmentNotification,
            private val contentResolver: ContentResolver) {

        private val updateMinFrequency = 1000
        private var previousSaveTime = System.currentTimeMillis()
        var listener: AttachmentFileActionListener? = null

        interface AttachmentFileActionListener {
            fun onUpdate(attachmentNotification: AttachmentNotification)
        }

        suspend fun executeAction() {
            TimeoutHelper.temporarilyDisableTimeout()

            // on pre execute
            attachmentNotification.attachmentFileAction = this
            attachmentNotification.entryAttachmentState.apply {
                downloadState = AttachmentState.START
                downloadProgression = 0
            }
            listener?.onUpdate(attachmentNotification)

            withContext(Dispatchers.IO) {
                // on Progress with thread
                val asyncResult: Deferred<Boolean> = async {
                    var progressResult = true
                    try {
                        attachmentNotification.entryAttachmentState.apply {
                            downloadState = AttachmentState.IN_PROGRESS

                            when (streamDirection) {
                                StreamDirection.UPLOAD -> {
                                    uploadToDatabase(
                                            attachmentNotification.uri,
                                            attachment.binaryAttachment,
                                            contentResolver, 1024) { percent ->
                                        publishProgress(percent)
                                    }
                                }
                                StreamDirection.DOWNLOAD -> {
                                    downloadFromDatabase(
                                            attachmentNotification.uri,
                                            attachment.binaryAttachment,
                                            contentResolver, 1024) { percent ->
                                        publishProgress(percent)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Unable to upload or download file", e)
                        progressResult = false
                    }
                    progressResult
                }

                // on post execute
                withContext(Dispatchers.Main) {
                    val result = asyncResult.await()
                    attachmentNotification.attachmentFileAction = null
                    attachmentNotification.entryAttachmentState.apply {
                        downloadState = if (result) AttachmentState.COMPLETE else AttachmentState.ERROR
                        downloadProgression = 100
                    }
                    listener?.onUpdate(attachmentNotification)
                    TimeoutHelper.releaseTemporarilyDisableTimeout()
                }

            }
        }

        fun downloadFromDatabase(attachmentToUploadUri: Uri,
                                 binaryAttachment: BinaryAttachment,
                                 contentResolver: ContentResolver,
                                 bufferSize: Int = DEFAULT_BUFFER_SIZE,
                                 update: ((percent: Int)->Unit)? = null) {
            var dataDownloaded = 0L
            val fileSize = binaryAttachment.length()
            UriUtil.getUriOutputStream(contentResolver, attachmentToUploadUri)?.use { outputStream ->
                if (binaryAttachment.isCompressed) {
                    GZIPInputStream(binaryAttachment.getInputDataStream())
                } else {
                    binaryAttachment.getInputDataStream()
                }.use { inputStream ->
                    inputStream.readBytes(bufferSize) { buffer ->
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
                             binaryAttachment: BinaryAttachment,
                             contentResolver: ContentResolver,
                             bufferSize: Int = DEFAULT_BUFFER_SIZE,
                             update: ((percent: Int)->Unit)? = null) {
            var dataUploaded = 0L
            val fileSize = contentResolver.openFileDescriptor(attachmentFromDownloadUri, "r")?.statSize ?: 0
            UriUtil.getUriInputStream(contentResolver, attachmentFromDownloadUri)?.let { inputStream ->
                if (binaryAttachment.isCompressed) {
                    GZIPOutputStream(binaryAttachment.getOutputDataStream())
                } else {
                    binaryAttachment.getOutputDataStream()
                }.use { outputStream ->
                    BufferedInputStream(inputStream).use { attachmentBufferedInputStream ->
                        attachmentBufferedInputStream.readBytes(bufferSize) { buffer ->
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

        private fun publishProgress(percent: Int) {
            // Publish progress
            val currentTime = System.currentTimeMillis()
            if (previousSaveTime + updateMinFrequency < currentTime) {
                attachmentNotification.entryAttachmentState.apply {
                    downloadState = AttachmentState.IN_PROGRESS
                    downloadProgression = percent
                }
                CoroutineScope(Dispatchers.Main).launch {
                    listener?.onUpdate(attachmentNotification)
                    Log.d(TAG, "Download file ${attachmentNotification.uri} : $percent%")
                }
                previousSaveTime = currentTime
            }
        }

        companion object {
            private val TAG = AttachmentFileAction::class.java.name
        }
    }

    companion object {
        private val TAG = AttachmentFileNotificationService::javaClass.name

        const val ACTION_ATTACHMENT_FILE_START_UPLOAD = "ACTION_ATTACHMENT_FILE_START_UPLOAD"
        const val ACTION_ATTACHMENT_FILE_START_DOWNLOAD = "ACTION_ATTACHMENT_FILE_START_DOWNLOAD"

        const val FILE_URI_KEY = "FILE_URI_KEY"
        const val ATTACHMENT_KEY = "ATTACHMENT_KEY"
    }

}