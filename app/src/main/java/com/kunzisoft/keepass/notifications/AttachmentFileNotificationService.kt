package com.kunzisoft.keepass.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.security.BinaryAttachment
import com.kunzisoft.keepass.tasks.AttachmentFileAsyncTask
import java.util.*
import kotlin.collections.ArrayList


class AttachmentFileNotificationService: LockNotificationService() {

    // Base notification Id, used only to create ids dynamically
    override val notificationId: Int = 610
    private val downloadFileUris = ArrayList<Uri>()

    private var mActionTaskBinder = ActionTaskBinder()
    private var mActionTaskListeners = LinkedList<ActionTaskListener>()

    inner class ActionTaskBinder: Binder() {

        fun getService(): AttachmentFileNotificationService = this@AttachmentFileNotificationService

        fun addActionTaskListener(actionTaskListener: ActionTaskListener) {
            mActionTaskListeners.add(actionTaskListener)
        }

        fun removeActionTaskListener(actionTaskListener: ActionTaskListener) {
            mActionTaskListeners.remove(actionTaskListener)
        }
    }

    interface ActionTaskListener {
        fun onStartAction(fileUri: Uri, attachment: BinaryAttachment)
        fun onUpdateAction(fileUri: Uri, attachment: BinaryAttachment, percentProgression: Int)
        fun onStopAction(fileUri: Uri, attachment: BinaryAttachment, success: Boolean)
    }

    override fun onBind(intent: Intent): IBinder? {
        return mActionTaskBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when(intent?.action) {
            ACTION_ATTACHMENT_FILE_START_DOWNLOAD -> {
                if (intent.hasExtra(DOWNLOAD_FILE_URI_KEY)
                        && intent.hasExtra(BINARY_ATTACHMENT_KEY)) {

                    val downloadFileUri: Uri = intent.getParcelableExtra(DOWNLOAD_FILE_URI_KEY)
                    val attachmentToDownload: BinaryAttachment = intent.getParcelableExtra(BINARY_ATTACHMENT_KEY)

                    downloadFileUris.add(downloadFileUri)

                    try {
                        AttachmentFileAsyncTask(downloadFileUri, contentResolver, {
                            newNotification(downloadFileUri)
                            mActionTaskListeners.forEach {
                                it.onStartAction(downloadFileUri, attachmentToDownload)
                            }

                        }, { percent ->
                            newNotification(downloadFileUri, false, percent)
                            mActionTaskListeners.forEach {
                                it.onUpdateAction(downloadFileUri, attachmentToDownload, percent)
                            }
                        }, { success ->
                            newNotification(downloadFileUri, true)
                            mActionTaskListeners.forEach {
                                it.onStopAction(downloadFileUri, attachmentToDownload, success)
                            }
                        }).execute(attachmentToDownload)
                    } catch (e: Exception) {
                        Log.e(TAG, "Unable to download $downloadFileUri", e)
                    }
                }
            }
            ACTION_ATTACHMENT_FILE_STOP_DOWNLOAD -> {
                stopSelf()
            }
            else -> { }
        }

        return START_REDELIVER_INTENT
    }

    private fun newNotification(downloadFileUri: Uri, complete: Boolean = false, progress: Int? = null) {
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
                    action = ACTION_ATTACHMENT_FILE_STOP_DOWNLOAD
                }, PendingIntent.FLAG_UPDATE_CURRENT)

        val fileName = DocumentFile.fromSingleUri(this, downloadFileUri)?.name ?: ""

        val builder = buildNewNotification().apply {
            setSmallIcon(R.drawable.ic_file_download_white_24dp)
            setContentTitle(getString(R.string.download_attachment, fileName))
            when {
                complete -> {
                    setContentText(getString(R.string.download_complete))
                    setContentIntent(pendingContentIntent)
                    setDeleteIntent(pendingDeleteIntent)
                    setOngoing(false)
                }
                progress != null -> {
                    if (progress > 100) {
                        setContentText(getString(R.string.download_finalization))
                    } else {
                        setProgress(100, progress, false)
                        setContentText(getString(R.string.download_progression, progress))
                    }
                    setOngoing(true)
                }
                else -> {
                    setContentText(getString(R.string.download_initialization))
                    setOngoing(true)
                }
            }
            setAutoCancel(false)
        }

        val notificationIdDynamic = notificationId + downloadFileUris.indexOf(downloadFileUri)
        startForeground(notificationIdDynamic, builder.build())
    }

    companion object {
        private val TAG = AttachmentFileNotificationService::javaClass.name

        const val ACTION_ATTACHMENT_FILE_START_DOWNLOAD = "ACTION_ATTACHMENT_FILE_START_DOWNLOAD"
        const val ACTION_ATTACHMENT_FILE_STOP_DOWNLOAD = "ACTION_ATTACHMENT_FILE_STOP_DOWNLOAD"

        const val DOWNLOAD_FILE_URI_KEY = "DOWNLOAD_FILE_URI_KEY"
        const val BINARY_ATTACHMENT_KEY = "BINARY_ATTACHMENT_KEY"

        fun start(context: Context) {
            context.startService(Intent(context, AttachmentFileNotificationService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AttachmentFileNotificationService::class.java))
        }
    }

}