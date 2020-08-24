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

import android.content.ComponentName
import android.content.Context.BIND_ABOVE_CLIENT
import android.content.Context.BIND_NOT_FOREGROUND
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import androidx.fragment.app.FragmentActivity
import com.kunzisoft.keepass.database.element.EntryAttachment
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.notifications.AttachmentFileNotificationService
import com.kunzisoft.keepass.notifications.AttachmentFileNotificationService.Companion.ACTION_ATTACHMENT_FILE_START_DOWNLOAD
import com.kunzisoft.keepass.notifications.AttachmentFileNotificationService.Companion.ACTION_ATTACHMENT_FILE_START_UPLOAD

class AttachmentFileBinderManager(private val activity: FragmentActivity) {

    var onActionTaskListener: AttachmentFileNotificationService.ActionTaskListener? = null

    private var mIntentTask = Intent(activity, AttachmentFileNotificationService::class.java)

    private var mBinder: AttachmentFileNotificationService.ActionTaskBinder? = null

    private var mServiceConnection: ServiceConnection? = null

    private val mActionTaskListener = object: AttachmentFileNotificationService.ActionTaskListener {
        override fun onAttachmentAction(fileUri: Uri, entryAttachmentState: EntryAttachmentState) {
            onActionTaskListener?.onAttachmentAction(fileUri, entryAttachmentState)
        }
    }

    @Synchronized
    fun registerProgressTask() {
        // Check if a service is currently running else do nothing
        if (mServiceConnection == null) {
            mServiceConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, serviceBinder: IBinder?) {
                    mBinder = (serviceBinder as AttachmentFileNotificationService.ActionTaskBinder).apply {
                        addActionTaskListener(mActionTaskListener)
                    }
                    mBinder?.getService()?.checkCurrentAttachmentProgress()
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    mBinder?.removeActionTaskListener(mActionTaskListener)
                    mBinder = null
                }
            }
        }

        mServiceConnection?.let {
            activity.bindService(mIntentTask, it, BIND_NOT_FOREGROUND or BIND_ABOVE_CLIENT)
        }
    }

    @Synchronized
    fun unregisterProgressTask() {
        onActionTaskListener = null

        mBinder?.removeActionTaskListener(mActionTaskListener)
        mBinder = null

        mServiceConnection?.let {
            activity.unbindService(it)
        }
        mServiceConnection = null
    }

    @Synchronized
    fun removeAttachmentAction(attachment: EntryAttachmentState) {
        mBinder?.getService()?.removeAttachmentAction(attachment)
    }

    @Synchronized
    private fun start(bundle: Bundle? = null, actionTask: String) {
        if (bundle != null)
            mIntentTask.putExtras(bundle)
        mIntentTask.action = actionTask
        activity.startService(mIntentTask)
    }

    fun startUploadAttachment(uploadFileUri: Uri,
                              entryAttachment: EntryAttachment) {
        start(Bundle().apply {
            putParcelable(AttachmentFileNotificationService.FILE_URI_KEY, uploadFileUri)
            putParcelable(AttachmentFileNotificationService.ATTACHMENT_KEY, entryAttachment)
        }, ACTION_ATTACHMENT_FILE_START_UPLOAD)
    }

    fun startDownloadAttachment(downloadFileUri: Uri,
                                entryAttachment: EntryAttachment) {
        start(Bundle().apply {
            putParcelable(AttachmentFileNotificationService.FILE_URI_KEY, downloadFileUri)
            putParcelable(AttachmentFileNotificationService.ATTACHMENT_KEY, entryAttachment)
        }, ACTION_ATTACHMENT_FILE_START_DOWNLOAD)
    }
}