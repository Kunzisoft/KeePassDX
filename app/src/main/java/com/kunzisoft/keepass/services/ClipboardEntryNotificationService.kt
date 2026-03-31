/*
 * Copyright 2025 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.services

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.OtpModel
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.otp.OtpType
import com.kunzisoft.keepass.settings.PreferencesUtil.isOtpNotificationEnable
import com.kunzisoft.keepass.timeout.ClipboardHelper
import com.kunzisoft.keepass.utils.getParcelableExtraCompat
import com.kunzisoft.keepass.utils.getParcelableList
import com.kunzisoft.keepass.utils.putParcelableList

class ClipboardEntryNotificationService : LockNotificationServiceParam<OtpElement>() {

    override val notificationId = 485
    private var clipboardHelper: ClipboardHelper? = null

    private var pendingCopyIntent: PendingIntent? = null
    private var pendingDeleteIntent: PendingIntent? = null

    override fun retrieveChannelId(): String {
        return CHANNEL_CLIPBOARD_ID
    }

    override fun retrieveChannelName(): String {
        return getString(R.string.clipboard)
    }

    override fun onCreate() {
        super.onCreate()
        clipboardHelper = ClipboardHelper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val otpModels: List<OtpModel>? = intent?.getParcelableList(EXTRA_LIST_OTP)
        val otpModelToCopy: OtpModel? = intent?.getParcelableExtraCompat(EXTRA_OTP_TO_COPY)

        when (intent?.action) {
            null -> Log.w(TAG, "null intent")
            ACTION_NEW_NOTIFICATION -> {
                if (!otpModels.isNullOrEmpty()) {
                    newNotification(otpModels)
                }
            }
            ACTION_COPY_CLIPBOARD -> {
                otpModelToCopy?.let {
                    copyToClipboard(OtpElement(otpModelToCopy).token)
                }
                stopService()
            }
            ACTION_CLEAN_OTP -> {
                stopService()
            }
            else -> {}
        }
        return START_NOT_STICKY
    }

    override fun timerContentText(data: OtpElement?): String? {
        return data?.token
    }

    private fun newNotification(otpModels: List<OtpModel>) {
        // Retrieve the first OTP
        val firstOtpModel = otpModels[0]
        val otpElement = OtpElement(firstOtpModel)
        val builder = buildNewNotification()
        pendingCopyIntent = buildCopyPendingIntent(firstOtpModel)
        pendingDeleteIntent = buildDeletePendingIntent()
        builder.run {
            setSmallIcon(R.drawable.notification_ic_clipboard_key_24dp)
            setContentTitle(firstOtpModel.toString())
            setAutoCancel(false)
            setContentText(otpElement.token)
            setContentIntent(pendingCopyIntent)
            setDeleteIntent(pendingDeleteIntent)
        }
        // Add others OTP
        if (otpModels.size > 1) {
            for (i in 1..<otpModels.size) {
                builder.addAction(
                    R.drawable.notification_ic_clipboard_key_24dp,
                    otpModels[i].toString(),
                    buildChangeOtpPendingIntent(otpModels[i])
                )
            }
        }
        if (otpElement.type == OtpType.TOTP) {
            defineTimerJob(
                builder,
                type = NotificationServiceType.CLIPBOARD,
                timeoutMilliseconds = otpElement.period * 1000L,
                timerData = otpElement
            ) {
                stopService()
            }
        }
        try {
            checkNotificationsPermission(this) {
                notificationManager?.notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Unable to notify the entry in clipboard", e)
            stopService()
        }
    }

    private fun buildChangeOtpPendingIntent(otpToOpen: OtpModel): PendingIntent {
        return buildServicePendingIntent(
            Intent(
                this,
                ClipboardEntryNotificationService::class.java
            ).apply {
                action = ACTION_NEW_NOTIFICATION
                putParcelableList(EXTRA_LIST_OTP, listOf(otpToOpen))
            }
        )
    }

    private fun buildCopyPendingIntent(otpToCopy: OtpModel): PendingIntent {
        return buildServicePendingIntent(
            Intent(this, ClipboardEntryNotificationService::class.java).apply {
                action = ACTION_COPY_CLIPBOARD
                putExtra(EXTRA_OTP_TO_COPY, otpToCopy)
            }
        )
    }

    private fun buildDeletePendingIntent(): PendingIntent {
        return buildServicePendingIntent(
            Intent(this, ClipboardEntryNotificationService::class.java).apply {
                action = ACTION_CLEAN_OTP
            }
        )
    }

    private fun copyToClipboard(otpToCopy: String) {
        clipboardHelper?.copyToClipboard(
            getString(R.string.entry_otp),
            otpToCopy
        )
    }

    override fun onDestroy() {
        pendingCopyIntent?.cancel()
        pendingDeleteIntent?.cancel()
        super.onDestroy()
    }

    companion object {

        private val TAG = ClipboardEntryNotificationService::class.simpleName

        private const val CHANNEL_CLIPBOARD_ID = "com.kunzisoft.keepass.notification.channel.clipboard"
        private const val EXTRA_LIST_OTP = "com.kunzisoft.keepass.EXTRA_LIST_OTP"
        private const val EXTRA_OTP_TO_COPY = "com.kunzisoft.keepass.EXTRA_OTP_TO_COPY"

        private const val ACTION_NEW_NOTIFICATION = "com.kunzisoft.keepass.ACTION_NEW_NOTIFICATION"
        private const val ACTION_COPY_CLIPBOARD = "com.kunzisoft.keepass.ACTION_COPY_CLIPBOARD"
        private const val ACTION_CLEAN_OTP = "com.kunzisoft.keepass.ACTION_CLEAN_OTP"

        fun launchOtpNotificationIfAllowed(
            context: Context,
            entries: List<EntryInfo>
        ) {
            var startService = false
            val intent = Intent(
                context,
                ClipboardEntryNotificationService::class.java
            )
            val otpList = entries.mapNotNull { it.otpModel }
            if (otpList.isNotEmpty() && isOtpNotificationEnable(context)) {
                checkNotificationsPermission(context, showError = false) {
                    startService = true
                    context.startService(intent.apply {
                        action = ACTION_NEW_NOTIFICATION
                        putParcelableList(EXTRA_LIST_OTP, otpList)
                    })
                }
            }
            if (!startService)
                context.stopService(intent)
        }
    }
}
