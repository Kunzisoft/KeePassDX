package com.kunzisoft.keepass.tasks

import android.content.ContentResolver
import android.net.Uri
import android.os.AsyncTask
import android.util.Log
import com.kunzisoft.keepass.model.AttachmentState
import com.kunzisoft.keepass.model.EntryAttachment
import com.kunzisoft.keepass.notifications.AttachmentFileNotificationService

class AttachmentFileAsyncTask(
        private val fileUri: Uri,
        private val attachmentNotification: AttachmentFileNotificationService.AttachmentNotification,
        private val contentResolver: ContentResolver,
        private val onUpdate: ((Uri, EntryAttachment, Int)->Unit)? = null
        ): AsyncTask<Void, Int, Boolean>() {

    private val updateMinFrequency = 1000
    private var previousSaveTime = System.currentTimeMillis()

    override fun onPreExecute() {
        super.onPreExecute()
        attachmentNotification.entryAttachment.apply {
            downloadState = AttachmentState.START
            downloadProgression = 0
        }
        onUpdate?.invoke(fileUri, attachmentNotification.entryAttachment, attachmentNotification.notificationId)
    }

    override fun doInBackground(vararg params: Void?): Boolean {
        try {
            attachmentNotification.entryAttachment.apply {
                downloadState = AttachmentState.IN_PROGRESS
                binaryAttachment.download(fileUri, contentResolver, 1024) { percent ->
                    publishProgress(percent)
                }
            }
        } catch (e: Exception) {
            return false
        }
        return true
    }

    override fun onProgressUpdate(vararg values: Int?) {
        super.onProgressUpdate(*values)
        val percent = values[0] ?: 0

        val currentTime = System.currentTimeMillis()
        if (previousSaveTime + updateMinFrequency < currentTime) {
            attachmentNotification.entryAttachment.apply {
                downloadState = AttachmentState.IN_PROGRESS
                downloadProgression = percent
            }
            onUpdate?.invoke(fileUri, attachmentNotification.entryAttachment, attachmentNotification.notificationId)
            Log.d(TAG, "Download file $fileUri : $percent%")
            previousSaveTime = currentTime
        }
    }

    override fun onPostExecute(result: Boolean) {
        super.onPostExecute(result)
        attachmentNotification.entryAttachment.apply {
            downloadState = if (result) AttachmentState.COMPLETE else AttachmentState.ERROR
            downloadProgression = 100
        }
        onUpdate?.invoke(fileUri, attachmentNotification.entryAttachment, attachmentNotification.notificationId)
    }

    companion object {
        private val TAG = AttachmentFileAsyncTask::class.java.name
    }
}