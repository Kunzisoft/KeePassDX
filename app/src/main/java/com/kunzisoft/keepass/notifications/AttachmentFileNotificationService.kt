package com.kunzisoft.keepass.notifications

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.model.AttachmentState
import com.kunzisoft.keepass.model.EntryAttachment
import com.kunzisoft.keepass.tasks.AttachmentFileAsyncTask
import java.util.*
import kotlin.collections.HashMap


class AttachmentFileNotificationService: LockNotificationService() {

    override val notificationId: Int = 10000

    private var mActionTaskBinder = ActionTaskBinder()
    private var mActionTaskListeners = LinkedList<ActionTaskListener>()

    inner class ActionTaskBinder: Binder() {

        fun getService(): AttachmentFileNotificationService = this@AttachmentFileNotificationService

        fun addActionTaskListener(actionTaskListener: ActionTaskListener) {
            mActionTaskListeners.add(actionTaskListener)

            downloadFileUris.forEach(object : (Map.Entry<Uri, AttachmentNotification>) -> Unit {
                override fun invoke(entry: Map.Entry<Uri, AttachmentNotification>) {
                    entry.value.attachmentTask?.onUpdate = { uri, attachment, notificationIdAttach ->
                        newNotification(uri, attachment, notificationIdAttach)
                        mActionTaskListeners.forEach { actionListener ->
                            actionListener.onAttachmentProgress(entry.key, attachment)
                        }
                    }
                }
            })

        }

        fun removeActionTaskListener(actionTaskListener: ActionTaskListener) {
            downloadFileUris.forEach(object : (Map.Entry<Uri, AttachmentNotification>) -> Unit {
                override fun invoke(entry: Map.Entry<Uri, AttachmentNotification>) {
                    entry.value.attachmentTask?.onUpdate = null
                }
            })

            mActionTaskListeners.remove(actionTaskListener)
        }
    }

    interface ActionTaskListener {
        fun onAttachmentProgress(fileUri: Uri, attachment: EntryAttachment)
    }

    override fun onBind(intent: Intent): IBinder? {
        return mActionTaskBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val downloadFileUri: Uri? = if (intent?.hasExtra(DOWNLOAD_FILE_URI_KEY) == true) {
            intent.getParcelableExtra(DOWNLOAD_FILE_URI_KEY)
        } else null

        when(intent?.action) {
            ACTION_ATTACHMENT_FILE_START_DOWNLOAD -> {
                if (downloadFileUri != null
                        && intent.hasExtra(ATTACHMENT_KEY)) {

                    val nextNotificationId = (downloadFileUris.values.maxBy { it.notificationId }
                            ?.notificationId ?: notificationId) + 1

                    val entryAttachment: EntryAttachment = intent.getParcelableExtra(ATTACHMENT_KEY)
                    val attachmentNotification = AttachmentNotification(nextNotificationId, entryAttachment)
                    downloadFileUris[downloadFileUri] = attachmentNotification
                    try {
                        AttachmentFileAsyncTask(downloadFileUri,
                                attachmentNotification,
                                contentResolver).apply {
                            onUpdate = { uri, attachment, notificationIdAttach ->
                                newNotification(uri, attachment, notificationIdAttach)
                                mActionTaskListeners.forEach { actionListener ->
                                    actionListener.onAttachmentProgress(downloadFileUri, attachment)
                                }
                            }
                        }.execute()
                    } catch (e: Exception) {
                        Log.e(TAG, "Unable to download $downloadFileUri", e)
                    }
                }
            }
            else -> {
                if (downloadFileUri != null) {
                    downloadFileUris[downloadFileUri]?.notificationId?.let {
                        notificationManager?.cancel(it)
                        downloadFileUris.remove(downloadFileUri)
                    }
                }
                if (downloadFileUris.isEmpty()) {
                    stopSelf()
                }
            }
        }

        return START_REDELIVER_INTENT
    }

    fun checkCurrentAttachmentProgress() {
        downloadFileUris.forEach(object : (Map.Entry<Uri, AttachmentNotification>) -> Unit {
            override fun invoke(entry: Map.Entry<Uri, AttachmentNotification>) {
                mActionTaskListeners.forEach { actionListener ->
                    actionListener.onAttachmentProgress(entry.key, entry.value.entryAttachment)
                }
            }
        })
    }

    private fun newNotification(downloadFileUri: Uri,
                                entryAttachment: EntryAttachment,
                                notificationIdAttachment: Int) {

        val pendingContentIntent = PendingIntent.getActivity(this,
                0,
                Intent().apply {
                    action = Intent.ACTION_VIEW
                    setDataAndType(downloadFileUri, contentResolver.getType(downloadFileUri))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, PendingIntent.FLAG_CANCEL_CURRENT)

        val pendingDeleteIntent =  PendingIntent.getService(this,
                0,
                Intent(this, AttachmentFileNotificationService::class.java).apply {
                    // No action to delete the service
                    putExtra(DOWNLOAD_FILE_URI_KEY, downloadFileUri)
                }, PendingIntent.FLAG_CANCEL_CURRENT)

        val fileName = DocumentFile.fromSingleUri(this, downloadFileUri)?.name ?: ""

        val builder = buildNewNotification().apply {
            setSmallIcon(R.drawable.ic_file_download_white_24dp)
            setContentTitle(getString(R.string.download_attachment, fileName))
            setAutoCancel(false)
            when (entryAttachment.downloadState) {
                AttachmentState.NULL, AttachmentState.START -> {
                    setContentText(getString(R.string.download_initialization))
                    setOngoing(true)
                }
                AttachmentState.IN_PROGRESS -> {
                    if (entryAttachment.downloadProgression > 100) {
                        setContentText(getString(R.string.download_finalization))
                    } else {
                        setProgress(100, entryAttachment.downloadProgression, false)
                        setContentText(getString(R.string.download_progression, entryAttachment.downloadProgression))
                    }
                    setOngoing(true)
                }
                AttachmentState.COMPLETE, AttachmentState.ERROR -> {
                    setContentText(getString(R.string.download_complete))
                    setContentIntent(pendingContentIntent)
                    setDeleteIntent(pendingDeleteIntent)
                    setOngoing(false)
                }
            }
        }
        notificationManager?.notify(notificationIdAttachment, builder.build())
    }

    override fun onDestroy() {
        downloadFileUris.forEach(object : (Map.Entry<Uri, AttachmentNotification>) -> Unit {
            override fun invoke(entry: Map.Entry<Uri, AttachmentNotification>) {
                entry.value.attachmentTask?.onUpdate = null
                notificationManager?.cancel(entry.value.notificationId)
            }
        })

        super.onDestroy()
    }

    data class AttachmentNotification(var notificationId: Int,
                                      var entryAttachment: EntryAttachment,
                                      var attachmentTask: AttachmentFileAsyncTask? = null) {
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

    companion object {
        private val TAG = AttachmentFileNotificationService::javaClass.name

        const val ACTION_ATTACHMENT_FILE_START_DOWNLOAD = "ACTION_ATTACHMENT_FILE_START_DOWNLOAD"

        const val DOWNLOAD_FILE_URI_KEY = "DOWNLOAD_FILE_URI_KEY"
        const val ATTACHMENT_KEY = "ATTACHMENT_KEY"

        private val downloadFileUris = HashMap<Uri, AttachmentNotification>()
    }

}