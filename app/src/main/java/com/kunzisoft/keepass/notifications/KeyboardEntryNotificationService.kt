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
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.preference.PreferenceManager
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.magikeyboard.MagikIME
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.LOCK_ACTION

class KeyboardEntryNotificationService : LockNotificationService() {

    override val notificationId = 486
    private var cleanNotificationTimerTask: Thread? = null
    private var notificationTimeoutMilliSecs: Long = 0

    private var pendingDeleteIntent: PendingIntent? = null

    private fun stopNotificationAndSendLockIfNeeded() {
        // Clear the entry if define in preferences
        if (PreferencesUtil.isClearKeyboardNotificationEnable(this)) {
            sendBroadcast(Intent(LOCK_ACTION))
        }
        // Stop the service
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        //Get settings
        notificationTimeoutMilliSecs = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(getString(R.string.keyboard_entry_timeout_key),
                getString(R.string.timeout_default))?.toLong() ?: TimeoutHelper.DEFAULT_TIMEOUT

        when {
            intent == null -> Log.w(TAG, "null intent")
            ACTION_CLEAN_KEYBOARD_ENTRY == intent.action -> {
                stopNotificationAndSendLockIfNeeded()
            }
            else -> {
                notificationManager?.cancel(notificationId)
                if (intent.hasExtra(ENTRY_INFO_KEY)) {
                    intent.getParcelableExtra<EntryInfo>(ENTRY_INFO_KEY)?.let {
                        newNotification(it)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun newNotification(entryInfo: EntryInfo) {

        var entryTitle = getString(R.string.keyboard_notification_entry_content_title_text)
        var entryUsername = ""
        if (entryInfo.title.isNotEmpty())
            entryTitle = entryInfo.title
        if (entryInfo.username.isNotEmpty())
            entryUsername = entryInfo.username

        val deleteIntent = Intent(this, KeyboardEntryNotificationService::class.java).apply {
            action = ACTION_CLEAN_KEYBOARD_ENTRY
        }
        pendingDeleteIntent = PendingIntent.getService(this, 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = buildNewNotification()
                .setSmallIcon(R.drawable.notification_ic_keyboard_key_24dp)
                .setContentTitle(getString(R.string.keyboard_notification_entry_content_title, entryTitle))
                .setContentText(getString(R.string.keyboard_notification_entry_content_text, entryUsername))
                .setAutoCancel(false)
                .setContentIntent(null)
                .setDeleteIntent(pendingDeleteIntent)

        notificationManager?.cancel(notificationId)
        notificationManager?.notify(notificationId, builder.build())

        stopTask(cleanNotificationTimerTask)
        // Timeout only if notification clear is available
        if (PreferencesUtil.isClearKeyboardNotificationEnable(this)) {
            if (notificationTimeoutMilliSecs != TimeoutHelper.NEVER) {
                cleanNotificationTimerTask = Thread {
                    val maxPos = 100
                    val posDurationMills = notificationTimeoutMilliSecs / maxPos
                    for (pos in maxPos downTo 0) {
                        builder.setProgress(maxPos, pos, false)
                        notificationManager?.notify(notificationId, builder.build())
                        try {
                            Thread.sleep(posDurationMills)
                        } catch (e: InterruptedException) {
                            break
                        }
                        if (pos <= 0) {
                            stopNotificationAndSendLockIfNeeded()
                        }
                    }
                }
                cleanNotificationTimerTask?.start()
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        MagikIME.removeEntry(this)

        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        // Remove the entry from the keyboard
        MagikIME.removeEntry(this)

        stopTask(cleanNotificationTimerTask)
        cleanNotificationTimerTask = null
        pendingDeleteIntent?.cancel()

        super.onDestroy()
    }

    companion object {

        private const val TAG = "KeyboardEntryNotifSrv"

        const val ENTRY_INFO_KEY = "ENTRY_INFO_KEY"

        const val ACTION_CLEAN_KEYBOARD_ENTRY = "ACTION_CLEAN_KEYBOARD_ENTRY"

        fun launchNotificationIfAllowed(context: Context, entry: EntryInfo) {

            val containsUsernameToCopy = entry.username.isNotEmpty()
            val containsPasswordToCopy = entry.password.isNotEmpty()
            val containsExtraFieldToCopy = entry.customFields.isNotEmpty()

            var startService = false
            val intent = Intent(context, KeyboardEntryNotificationService::class.java)

            // Show the notification if allowed in Preferences
            if (PreferencesUtil.isKeyboardNotificationEntryEnable(context)) {
                if (containsUsernameToCopy || containsPasswordToCopy || containsExtraFieldToCopy) {
                    startService = true
                    context.startService(intent.apply {
                        putExtra(ENTRY_INFO_KEY, entry)
                    })
                }
            }

            if (!startService)
                context.stopService(intent)
        }
    }

}
