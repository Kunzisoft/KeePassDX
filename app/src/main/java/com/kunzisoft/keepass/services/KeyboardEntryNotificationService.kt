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
package com.kunzisoft.keepass.services

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.preference.PreferenceManager
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.credentialprovider.magikeyboard.MagikeyboardService
import com.kunzisoft.keepass.credentialprovider.magikeyboard.MagikeyboardService.Companion.getSwitchMagikeyboardIntent
import com.kunzisoft.keepass.credentialprovider.magikeyboard.MagikeyboardService.Companion.isAutoSwitchMagikeyboardAllowed
import com.kunzisoft.keepass.credentialprovider.magikeyboard.MagikeyboardService.Companion.isMagikeyboardActivated
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.timeout.TimeoutHelper.NEVER
import com.kunzisoft.keepass.utils.EXTRA_PROGRESS
import com.kunzisoft.keepass.utils.LOCK_ACTION
import com.kunzisoft.keepass.utils.UPDATE_TIMEOUT_PROGRESS_ACTION

class KeyboardEntryNotificationService : LockNotificationService() {

    override val notificationId = 486
    private var mNotificationTimeoutMilliSecs: Long = 0

    private var mainPendingIntent: PendingIntent? = null
    private var pendingDeleteIntent: PendingIntent? = null

    override fun retrieveChannelId(): String {
        return CHANNEL_MAGIKEYBOARD_ID
    }

    override fun retrieveChannelName(): String {
        return getString(R.string.magic_keyboard_title)
    }

    private fun stopNotificationAndSendLockIfNeeded() {
        // Clear the entry if define in preferences
        if (PreferencesUtil.isClearKeyboardNotificationEnable(this)) {
            sendBroadcast(Intent(LOCK_ACTION))
        }
        // Stop the service
        stopService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (this.isMagikeyboardActivated().not())
            stopService()

        //Get settings
        mNotificationTimeoutMilliSecs = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(getString(R.string.keyboard_entry_timeout_key),
                getString(R.string.timeout_default))?.toLong() ?: TimeoutHelper.DEFAULT_TIMEOUT

        when (intent?.action) {
            null -> Log.w(TAG, "null intent")
            ACTION_CLEAN_KEYBOARD_ENTRY -> {
                stopNotificationAndSendLockIfNeeded()
            }
            ACTION_NEW_NOTIFICATION -> {
                notificationManager?.cancel(notificationId)
                newNotification(intent.getStringExtra(TITLE_INFO_KEY))
            }
            else -> {}
        }
        return START_NOT_STICKY
    }

    private fun newNotification(title: String?) {

        mainPendingIntent =
            if (isAutoSwitchMagikeyboardAllowed(this)) {
                buildActivityPendingIntent(getSwitchMagikeyboardIntent(this))
            } else null
        pendingDeleteIntent = buildServicePendingIntent(
            Intent(this, KeyboardEntryNotificationService::class.java).apply {
                action = ACTION_CLEAN_KEYBOARD_ENTRY
            }
        )

        val entryTitle = title ?: getString(R.string.keyboard_notification_entry_content_title_text)
        val builder = buildNewNotification()
        builder.run {
            setSmallIcon(R.drawable.notification_ic_keyboard_key_24dp)
            setContentTitle(getString(R.string.keyboard_notification_entry_content_title, entryTitle))
            setAutoCancel(false)
            mainPendingIntent?.let {
                setContentIntent(it)
            }
            setDeleteIntent(pendingDeleteIntent)
        }
        // Timeout only if notification clear is available
        if (PreferencesUtil.isClearKeyboardNotificationEnable(this)) {
            if (mNotificationTimeoutMilliSecs > NEVER) {
                defineTimerJob(
                    builder = builder,
                    type = NotificationServiceType.KEYBOARD,
                    timeoutMilliseconds = mNotificationTimeoutMilliSecs,
                    actionAfterASecond = { progress ->
                        sendBroadcast(Intent(UPDATE_TIMEOUT_PROGRESS_ACTION).apply {
                            putExtra(EXTRA_PROGRESS, progress)
                        })
                    }
                ) {
                    sendBroadcast(Intent(UPDATE_TIMEOUT_PROGRESS_ACTION).apply {
                        putExtra(EXTRA_PROGRESS, 0)
                    })
                    stopNotificationAndSendLockIfNeeded()
                }
            }
        }
        try {
            checkNotificationsPermission(this) {
                notificationManager?.notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Unable to notify the entry in keyboard", e)
            stopService()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        MagikeyboardService.removeEntry(this)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        // Remove the entry from the keyboard
        MagikeyboardService.removeEntry(this)
        mainPendingIntent?.cancel()
        pendingDeleteIntent?.cancel()
        super.onDestroy()
    }

    companion object {

        private const val TAG = "KeyboardEntryNotifSrv"

        private const val CHANNEL_MAGIKEYBOARD_ID = "com.kunzisoft.keepass.notification.channel.magikeyboard"
        private const val TITLE_INFO_KEY = "com.kunzisoft.keepass.TITLE_INFO_KEY"
        const val ACTION_NEW_NOTIFICATION = "com.kunzisoft.keepass.ACTION_NEW_NOTIFICATION"
        private const val ACTION_CLEAN_KEYBOARD_ENTRY = "com.kunzisoft.keepass.ACTION_CLEAN_KEYBOARD_ENTRY"

        fun launchNotificationIfAllowed(context: Context, title: String) {

            var startService = false
            val intent = Intent(context, KeyboardEntryNotificationService::class.java)

            // Show the notification if allowed in Preferences
            if (PreferencesUtil.isKeyboardNotificationEntryEnable(context)
                && context.isMagikeyboardActivated()) {
                checkNotificationsPermission(context, showError = false) {
                    startService = true
                    context.startService(intent.apply {
                        putExtra(TITLE_INFO_KEY, title)
                        action = ACTION_NEW_NOTIFICATION
                    })
                }
            }

            if (!startService)
                context.stopService(intent)
        }
    }

}
