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
import com.kunzisoft.keepass.model.EntryAttachment
import com.kunzisoft.keepass.notifications.AttachmentFileNotificationService
import com.kunzisoft.keepass.notifications.AttachmentFileNotificationService.Companion.ACTION_ATTACHMENT_FILE_START_DOWNLOAD

class AttachmentFileBinderManager(private val activity: FragmentActivity) {

    var onActionTaskListener: AttachmentFileNotificationService.ActionTaskListener? = null

    private var mIntentTask = Intent(activity, AttachmentFileNotificationService::class.java)

    private var mBinder: AttachmentFileNotificationService.ActionTaskBinder? = null

    private var mServiceConnection: ServiceConnection? = null

    private val mActionTaskListener = object: AttachmentFileNotificationService.ActionTaskListener {
        override fun onAttachmentProgress(fileUri: Uri, attachment: EntryAttachment) {
            onActionTaskListener?.onAttachmentProgress(fileUri, attachment)
        }
    }

    // TODO Fix orientation change
    fun checkProgress() {
        mBinder?.getService()?.checkCurrentAttachmentProgress()
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
    private fun start(bundle: Bundle? = null, actionTask: String) {
        activity.stopService(mIntentTask)
        if (bundle != null)
            mIntentTask.putExtras(bundle)
        activity.runOnUiThread {
            mIntentTask.action = actionTask
            activity.startService(mIntentTask)
        }
    }

    fun startDownloadAttachment(downloadFileUri: Uri,
                                entryAttachment: EntryAttachment) {
        start(Bundle().apply {
            putParcelable(AttachmentFileNotificationService.DOWNLOAD_FILE_URI_KEY, downloadFileUri)
            putParcelable(AttachmentFileNotificationService.ATTACHMENT_KEY, entryAttachment)
        }, ACTION_ATTACHMENT_FILE_START_DOWNLOAD)
    }
}