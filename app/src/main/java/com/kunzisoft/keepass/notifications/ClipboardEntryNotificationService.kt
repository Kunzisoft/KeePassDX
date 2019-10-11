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

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.util.Log
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.exception.SamsungClipboardException
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.ClipboardHelper
import com.kunzisoft.keepass.timeout.TimeoutHelper.NEVER
import com.kunzisoft.keepass.utils.LOCK_ACTION
import java.util.*

class ClipboardEntryNotificationService : NotificationService() {

    private var notificationId = 485
    private var cleanNotificationTimerTask: Thread? = null
    private var notificationTimeoutMilliSecs: Long = 0

    private var clipboardHelper: ClipboardHelper? = null
    private var cleanCopyNotificationTimerTask: Thread? = null

    override fun onCreate() {
        super.onCreate()

        clipboardHelper = ClipboardHelper(this)
    }

    private fun stopNotificationAndSendLockIfNeeded() {
        // Clear the entry if define in preferences
        if (PreferencesUtil.isClearClipboardNotificationEnable(this)) {
            sendBroadcast(Intent(LOCK_ACTION))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //Get settings
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val timeoutClipboardClear = prefs.getString(getString(R.string.clipboard_timeout_key),
                getString(R.string.clipboard_timeout_default)) ?: "6000"
        notificationTimeoutMilliSecs = java.lang.Long.parseLong(timeoutClipboardClear)

        when {
            intent == null -> Log.w(TAG, "null intent")
            ACTION_NEW_NOTIFICATION == intent.action -> {
                val title = intent.getStringExtra(EXTRA_ENTRY_TITLE)
                newNotification(title, constructListOfField(intent))
            }
            ACTION_CLEAN_CLIPBOARD == intent.action -> {
                stopTask(cleanCopyNotificationTimerTask)
                try {
                    clipboardHelper?.cleanClipboard()
                } catch (e: SamsungClipboardException) {
                    Log.e(TAG, "Clipboard can't be cleaned", e)
                }
                stopNotificationAndSendLockIfNeeded()
            }
            else -> for (actionKey in ClipboardEntryNotificationField.allActionKeys) {
                if (actionKey == intent.action) {
                    val fieldToCopy = intent.getParcelableExtra<ClipboardEntryNotificationField>(
                            ClipboardEntryNotificationField.getExtraKeyLinkToActionKey(actionKey))
                    val nextFields = constructListOfField(intent)
                    // Remove the current field from the next fields
                    nextFields.remove(fieldToCopy)
                    copyField(fieldToCopy, nextFields)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun constructListOfField(intent: Intent?): ArrayList<ClipboardEntryNotificationField> {
        var fieldList = ArrayList<ClipboardEntryNotificationField>()
        if (intent != null && intent.extras != null) {
            if (intent.extras!!.containsKey(EXTRA_FIELDS))
                fieldList = intent.getParcelableArrayListExtra(EXTRA_FIELDS)
        }
        return fieldList
    }

    private fun getCopyPendingIntent(fieldToCopy: ClipboardEntryNotificationField, fieldsToAdd: ArrayList<ClipboardEntryNotificationField>): PendingIntent {
        val copyIntent = Intent(this, ClipboardEntryNotificationService::class.java)
        copyIntent.action = fieldToCopy.actionKey
        copyIntent.putExtra(fieldToCopy.extraKey, fieldToCopy)
        copyIntent.putParcelableArrayListExtra(EXTRA_FIELDS, fieldsToAdd)

        return PendingIntent.getService(
                this, 0, copyIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun newNotification(title: String?, fieldsToAdd: ArrayList<ClipboardEntryNotificationField>) {
        stopTask(cleanCopyNotificationTimerTask)

        val builder = buildNewNotification()
                .setSmallIcon(R.drawable.notification_ic_clipboard_key_24dp)

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
                builder.addAction(R.drawable.notification_ic_clipboard_key_24dp, fieldToAdd.label,
                        getCopyPendingIntent(fieldToAdd, fieldsToAdd))
            }
        }

        notificationManager?.cancel(notificationId)
        notificationManager?.notify(++notificationId, builder.build())

        val myNotificationId = notificationId
        stopTask(cleanNotificationTimerTask)
        // If timer
        if (notificationTimeoutMilliSecs != NEVER) {
            cleanNotificationTimerTask = Thread {
                try {
                    Thread.sleep(notificationTimeoutMilliSecs)
                } catch (e: InterruptedException) {
                    cleanNotificationTimerTask = null
                }
                notificationManager?.cancel(myNotificationId)
            }
            cleanNotificationTimerTask?.start()
        }
    }

    private fun copyField(fieldToCopy: ClipboardEntryNotificationField, nextFields: ArrayList<ClipboardEntryNotificationField>) {
        stopTask(cleanCopyNotificationTimerTask)
        stopTask(cleanNotificationTimerTask)

        try {
            clipboardHelper?.copyToClipboard(fieldToCopy.label, fieldToCopy.value)

            val builder = buildNewNotification()
                    .setSmallIcon(R.drawable.notification_ic_clipboard_key_24dp)
                    .setContentTitle(fieldToCopy.label)

            // New action with next field if click
            if (nextFields.size > 0) {
                val nextField = nextFields[0]
                builder.setContentText(nextField.copyText)
                builder.setContentIntent(getCopyPendingIntent(nextField, nextFields))
                // Else tell to swipe for a clean
            } else {
                builder.setContentText(getString(R.string.clipboard_swipe_clean))
            }

            val cleanIntent = Intent(this, ClipboardEntryNotificationService::class.java)
            cleanIntent.action = ACTION_CLEAN_CLIPBOARD
            val cleanPendingIntent = PendingIntent.getService(
                    this, 0, cleanIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            builder.setDeleteIntent(cleanPendingIntent)

            val myNotificationId = notificationId

            if (notificationTimeoutMilliSecs != NEVER) {
                cleanCopyNotificationTimerTask = Thread {
                    val maxPos = 100
                    val posDurationMills = notificationTimeoutMilliSecs / maxPos
                    for (pos in maxPos downTo 0) {
                        builder.setProgress(maxPos, pos, false)
                        notificationManager?.notify(myNotificationId, builder.build())
                        try {
                            Thread.sleep(posDurationMills)
                        } catch (e: InterruptedException) {
                            break
                        }
                        if (pos <= 0) {
                            stopNotificationAndSendLockIfNeeded()
                        }
                    }
                    stopTask(cleanCopyNotificationTimerTask)
                    notificationManager?.cancel(myNotificationId)
                    // Clean password only if no next field
                    if (nextFields.size <= 0)
                        try {
                            clipboardHelper?.cleanClipboard()
                        } catch (e: SamsungClipboardException) {
                            Log.e(TAG, "Clipboard can't be cleaned", e)
                        }
                }
                cleanCopyNotificationTimerTask?.start()
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

        private val TAG = ClipboardEntryNotificationService::class.java.name

        const val ACTION_NEW_NOTIFICATION = "ACTION_NEW_NOTIFICATION"
        const val EXTRA_ENTRY_TITLE = "EXTRA_ENTRY_TITLE"
        const val EXTRA_FIELDS = "EXTRA_FIELDS"
        const val ACTION_CLEAN_CLIPBOARD = "ACTION_CLEAN_CLIPBOARD"

        fun launchNotificationIfAllowed(context: Context, entry: EntryInfo) {

            val containsUsernameToCopy = entry.username.isNotEmpty()
            val containsPasswordToCopy = entry.password.isNotEmpty()
                    && PreferencesUtil.allowCopyPasswordAndProtectedFields(context)
            val containsExtraFieldToCopy = entry.customFields.isNotEmpty()
                    && (entry.containsCustomFieldsNotProtected()
                        ||
                        (entry.containsCustomFieldsProtected() && PreferencesUtil.allowCopyPasswordAndProtectedFields(context))
                    )

            // If notifications enabled in settings
            // Don't if application timeout
            if (PreferencesUtil.isClipboardNotificationsEnable(context)) {
                if (containsUsernameToCopy || containsPasswordToCopy || containsExtraFieldToCopy) {

                    // username already copied, waiting for user's action before copy password.
                    val intent = Intent(context, ClipboardEntryNotificationService::class.java)
                    intent.action = ACTION_NEW_NOTIFICATION
                    intent.putExtra(EXTRA_ENTRY_TITLE, entry.title)
                    // Construct notification fields
                    val notificationFields = ArrayList<ClipboardEntryNotificationField>()
                    // Add username if exists to notifications
                    if (containsUsernameToCopy)
                        notificationFields.add(
                                ClipboardEntryNotificationField(
                                        ClipboardEntryNotificationField.NotificationFieldId.USERNAME,
                                        entry.username,
                                        context.resources))
                    // Add password to notifications
                    if (containsPasswordToCopy) {
                        notificationFields.add(
                                ClipboardEntryNotificationField(
                                        ClipboardEntryNotificationField.NotificationFieldId.PASSWORD,
                                        entry.password,
                                        context.resources))
                    }
                    // Add extra fields
                    if (containsExtraFieldToCopy) {
                        try {
                            var anonymousFieldNumber = 0
                            entry.customFields.forEach { field ->
                                //If value is not protected or allowed
                                if (!field.protectedValue.isProtected
                                        || PreferencesUtil.allowCopyPasswordAndProtectedFields(context)) {
                                    notificationFields.add(
                                            ClipboardEntryNotificationField(
                                                    ClipboardEntryNotificationField.NotificationFieldId.anonymousFieldId[anonymousFieldNumber],
                                                    field.protectedValue.toString(),
                                                    field.name,
                                                    context.resources))
                                    anonymousFieldNumber++
                                }
                            }
                        } catch (e: ArrayIndexOutOfBoundsException) {
                            Log.w("NotificationEntryCopyMg", "Only " + ClipboardEntryNotificationField.NotificationFieldId.anonymousFieldId.size +
                                    " anonymous notifications are available")
                        }

                    }
                    // Add notifications
                    intent.putParcelableArrayListExtra(EXTRA_FIELDS, notificationFields)
                    context.startService(intent)
                }
            }
        }
    }
}
