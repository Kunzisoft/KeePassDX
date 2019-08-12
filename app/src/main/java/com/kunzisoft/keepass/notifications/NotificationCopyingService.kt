/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.notifications

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.util.Log
import android.util.TypedValue
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.stylish.Stylish
import com.kunzisoft.keepass.database.exception.SamsungClipboardException
import com.kunzisoft.keepass.timeout.ClipboardHelper
import com.kunzisoft.keepass.timeout.TimeoutHelper.NEVER
import java.util.*

class NotificationCopyingService : Service() {

    private var notificationManager: NotificationManager? = null
    private var clipboardHelper: ClipboardHelper? = null
    private var cleanNotificationTimer: Thread? = null
    private var countingDownTask: Thread? = null
    private var notificationId = 1
    private var notificationTimeoutMilliSecs: Long = 0

    private var colorNotificationAccent: Int = 0

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        clipboardHelper = ClipboardHelper(this)

        // Create notification channel for Oreo+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID_COPYING,
                    CHANNEL_NAME_COPYING,
                    NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager?.createNotificationChannel(channel)
        }

        // Get the color
        setTheme(Stylish.getThemeId(this))
        val typedValue = TypedValue()
        val theme = theme
        theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
        colorNotificationAccent = typedValue.data
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //Get settings
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val timeoutClipboardClear = prefs.getString(getString(R.string.clipboard_timeout_key),
                getString(R.string.clipboard_timeout_default))
        notificationTimeoutMilliSecs = java.lang.Long.parseLong(timeoutClipboardClear)

        when {
            intent == null -> Log.w(TAG, "null intent")
            ACTION_NEW_NOTIFICATION == intent.action -> {
                val title = intent.getStringExtra(EXTRA_ENTRY_TITLE)
                newNotification(title, constructListOfField(intent))
            }
            ACTION_CLEAN_CLIPBOARD == intent.action -> {
                stopTask(countingDownTask)
                try {
                    clipboardHelper?.cleanClipboard()
                } catch (e: SamsungClipboardException) {
                    Log.e(TAG, "Clipboard can't be cleaned", e)
                }
            }
            else -> for (actionKey in NotificationField.allActionKeys) {
                if (actionKey == intent.action) {
                    val fieldToCopy = intent.getParcelableExtra<NotificationField>(
                            NotificationField.getExtraKeyLinkToActionKey(actionKey))
                    val nextFields = constructListOfField(intent)
                    // Remove the current field from the next fields
                    nextFields.remove(fieldToCopy)
                    copyField(fieldToCopy, nextFields)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun constructListOfField(intent: Intent?): ArrayList<NotificationField> {
        var fieldList = ArrayList<NotificationField>()
        if (intent != null && intent.extras != null) {
            if (intent.extras!!.containsKey(EXTRA_FIELDS))
                fieldList = intent.getParcelableArrayListExtra(EXTRA_FIELDS)
        }
        return fieldList
    }

    private fun getCopyPendingIntent(fieldToCopy: NotificationField, fieldsToAdd: ArrayList<NotificationField>): PendingIntent {
        val copyIntent = Intent(this, NotificationCopyingService::class.java)
        copyIntent.action = fieldToCopy.actionKey
        copyIntent.putExtra(fieldToCopy.extraKey, fieldToCopy)
        copyIntent.putParcelableArrayListExtra(EXTRA_FIELDS, fieldsToAdd)

        return PendingIntent.getService(
                this, 0, copyIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun newNotification(title: String?, fieldsToAdd: ArrayList<NotificationField>) {
        stopTask(countingDownTask)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID_COPYING)
                .setSmallIcon(R.drawable.ic_key_white_24dp)
                .setColor(colorNotificationAccent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)

        if (title != null)
            builder.setContentTitle(title)

        if (fieldsToAdd.size > 0) {
            val field = fieldsToAdd[0]
            builder.setContentText(field.copyText)
            builder.setContentIntent(getCopyPendingIntent(field, fieldsToAdd))

            // Add extra actions without 1st field
            val fieldsWithoutFirstField = ArrayList(fieldsToAdd)
            fieldsWithoutFirstField.remove(field)
            // Add extra actions
            for (fieldToAdd in fieldsWithoutFirstField) {
                builder.addAction(R.drawable.ic_key_white_24dp, fieldToAdd.label,
                        getCopyPendingIntent(fieldToAdd, fieldsToAdd))
            }
        }

        notificationManager?.cancel(notificationId)
        notificationManager?.notify(++notificationId, builder.build())

        val myNotificationId = notificationId
        stopTask(cleanNotificationTimer)
        // If timer
        if (notificationTimeoutMilliSecs != NEVER) {
            cleanNotificationTimer = Thread {
                try {
                    Thread.sleep(notificationTimeoutMilliSecs)
                } catch (e: InterruptedException) {
                    cleanNotificationTimer = null
                }
                notificationManager?.cancel(myNotificationId)
            }
            cleanNotificationTimer?.start()
        }
    }

    private fun copyField(fieldToCopy: NotificationField, nextFields: ArrayList<NotificationField>) {
        stopTask(countingDownTask)
        stopTask(cleanNotificationTimer)

        try {
            clipboardHelper?.copyToClipboard(fieldToCopy.label, fieldToCopy.value)

            val builder = NotificationCompat.Builder(this, CHANNEL_ID_COPYING)
                    .setSmallIcon(R.drawable.ic_key_white_24dp)
                    .setColor(colorNotificationAccent)
                    .setContentTitle(fieldToCopy.label)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setVisibility(NotificationCompat.VISIBILITY_SECRET)

            // New action with next field if click
            if (nextFields.size > 0) {
                val nextField = nextFields[0]
                builder.setContentText(nextField.copyText)
                builder.setContentIntent(getCopyPendingIntent(nextField, nextFields))
                // Else tell to swipe for a clean
            } else {
                builder.setContentText(getString(R.string.clipboard_swipe_clean))
            }

            val cleanIntent = Intent(this, NotificationCopyingService::class.java)
            cleanIntent.action = ACTION_CLEAN_CLIPBOARD
            val cleanPendingIntent = PendingIntent.getService(
                    this, 0, cleanIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            builder.setDeleteIntent(cleanPendingIntent)

            val myNotificationId = notificationId

            if (notificationTimeoutMilliSecs != NEVER) {
                countingDownTask = Thread {
                    val maxPos = 100
                    val posDurationMills = notificationTimeoutMilliSecs / maxPos
                    for (pos in maxPos downTo 1) {
                        builder.setProgress(maxPos, pos, false)
                        notificationManager?.notify(myNotificationId, builder.build())
                        try {
                            Thread.sleep(posDurationMills)
                        } catch (e: InterruptedException) {
                            break
                        }

                    }
                    countingDownTask = null
                    notificationManager?.cancel(myNotificationId)
                    // Clean password only if no next field
                    if (nextFields.size <= 0)
                        try {
                            clipboardHelper?.cleanClipboard()
                        } catch (e: SamsungClipboardException) {
                            Log.e(TAG, "Clipboard can't be cleaned", e)
                        }
                }
                countingDownTask?.start()
            } else {
                // No timer
                notificationManager?.notify(myNotificationId, builder.build())
            }

        } catch (e: Exception) {
            Log.e(TAG, "Clipboard can't be populate", e)
        }

    }

    private fun stopTask(task: Thread?) {
        if (task != null && task.isAlive)
            task.interrupt()
    }

    companion object {

        private val TAG = NotificationCopyingService::class.java.name
        private const val CHANNEL_ID_COPYING = "CHANNEL_ID_COPYING"
        private const val CHANNEL_NAME_COPYING = "Copy fields"

        const val ACTION_NEW_NOTIFICATION = "ACTION_NEW_NOTIFICATION"
        const val EXTRA_ENTRY_TITLE = "EXTRA_ENTRY_TITLE"
        const val EXTRA_FIELDS = "EXTRA_FIELDS"
        const val ACTION_CLEAN_CLIPBOARD = "ACTION_CLEAN_CLIPBOARD"
    }

}
