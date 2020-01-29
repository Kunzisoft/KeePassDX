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
        private val contentResolver: ContentResolver)
    : AsyncTask<Void, Int, Boolean>() {

    private val updateMinFrequency = 1000
    private var previousSaveTime = System.currentTimeMillis()
    var onUpdate: ((Uri, EntryAttachment, Int)->Unit)? = null

    override fun onPreExecute() {
        super.onPreExecute()
        attachmentNotification.attachmentTask = this
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
        attachmentNotification.attachmentTask = null
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