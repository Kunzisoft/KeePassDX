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
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.ClipboardHelper
import com.kunzisoft.keepass.timeout.TimeoutHelper.NEVER
import com.kunzisoft.keepass.utils.LOCK_ACTION
import java.util.*

class ClipboardEntryNotificationService : LockNotificationService() {

    override val notificationId = 485
    private var mEntryInfo: EntryInfo? = null
    private var clipboardHelper: ClipboardHelper? = null
    private var notificationTimeoutMilliSecs: Long = 0
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
        // Stop the service
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Get entry info from intent
        mEntryInfo = intent?.getParcelableExtra(EXTRA_ENTRY_INFO)

        //Get settings
        notificationTimeoutMilliSecs = PreferencesUtil.getClipboardTimeout(this)

        when {
            intent == null -> Log.w(TAG, "null intent")
            ACTION_NEW_NOTIFICATION == intent.action -> {
                newNotification(mEntryInfo?.title, constructListOfField(intent))
            }
            ACTION_CLEAN_CLIPBOARD == intent.action -> {
                stopTask(cleanCopyNotificationTimerTask)
                cleanClipboard()
                stopNotificationAndSendLockIfNeeded()
            }
            else -> for (actionKey in ClipboardEntryNotificationField.allActionKeys) {
                if (actionKey == intent.action) {
                    intent.getParcelableExtra<ClipboardEntryNotificationField>(
                            ClipboardEntryNotificationField.getExtraKeyLinkToActionKey(actionKey))?.let {
                        fieldToCopy ->
                        val nextFields = constructListOfField(intent)
                        // Remove the current field from the next fields
                        nextFields.remove(fieldToCopy)
                        copyField(fieldToCopy, nextFields)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun constructListOfField(intent: Intent?): ArrayList<ClipboardEntryNotificationField> {
        val fieldList = ArrayList<ClipboardEntryNotificationField>()
        if (intent?.extras?.containsKey(EXTRA_CLIPBOARD_FIELDS) == true) {
            intent.getParcelableArrayListExtra<ClipboardEntryNotificationField>(EXTRA_CLIPBOARD_FIELDS)?.let { retrieveFields ->
                fieldList.clear()
                fieldList.addAll(retrieveFields)
            }
        }
        return fieldList
    }

    private fun getCopyPendingIntent(fieldToCopy: ClipboardEntryNotificationField, fieldsToAdd: ArrayList<ClipboardEntryNotificationField>): PendingIntent {
        val copyIntent = Intent(this, ClipboardEntryNotificationService::class.java).apply {
            action = fieldToCopy.actionKey
            putExtra(EXTRA_ENTRY_INFO, mEntryInfo)
            putExtra(fieldToCopy.extraKey, fieldToCopy)
            putParcelableArrayListExtra(EXTRA_CLIPBOARD_FIELDS, fieldsToAdd)
        }
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
            builder.setContentText(getString(R.string.select_to_copy, field.label))
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
        notificationManager?.notify(notificationId, builder.build())
    }

    private fun copyField(fieldToCopy: ClipboardEntryNotificationField, nextFields: ArrayList<ClipboardEntryNotificationField>) {
        stopTask(cleanCopyNotificationTimerTask)

        try {
            var generatedValue = fieldToCopy.getGeneratedValue(mEntryInfo)
            clipboardHelper?.copyToClipboard(fieldToCopy.label, generatedValue)

            val builder = buildNewNotification()
                    .setSmallIcon(R.drawable.notification_ic_clipboard_key_24dp)
                    .setContentTitle(fieldToCopy.label)

            // New action with next field if click
            if (nextFields.size > 0) {
                val nextField = nextFields[0]
                builder.setContentText(getString(R.string.select_to_copy, nextField.label))
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
                        val newGeneratedValue = fieldToCopy.getGeneratedValue(mEntryInfo)
                        // New auto generated value
                        if (generatedValue != newGeneratedValue) {
                            generatedValue = newGeneratedValue
                            clipboardHelper?.copyToClipboard(fieldToCopy.label, generatedValue)
                        }
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
                        cleanClipboard()
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

    private fun cleanClipboard() {
        try {
            clipboardHelper?.cleanClipboard()
        } catch (e: Exception) {
            Log.e(TAG, "Clipboard can't be cleaned", e)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        cleanClipboard()

        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        cleanClipboard()

        stopTask(cleanCopyNotificationTimerTask)
        cleanCopyNotificationTimerTask = null

        super.onDestroy()
    }

    companion object {

        private val TAG = ClipboardEntryNotificationService::class.java.name

        const val ACTION_NEW_NOTIFICATION = "ACTION_NEW_NOTIFICATION"
        const val EXTRA_ENTRY_INFO = "EXTRA_ENTRY_INFO"
        const val EXTRA_CLIPBOARD_FIELDS = "EXTRA_CLIPBOARD_FIELDS"
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

            var startService = false
            val intent = Intent(context, ClipboardEntryNotificationService::class.java)

            // If notifications enabled in settings
            // Don't if application timeout
            if (PreferencesUtil.isClipboardNotificationsEnable(context)) {
                if (containsUsernameToCopy || containsPasswordToCopy || containsExtraFieldToCopy) {

                    // username already copied, waiting for user's action before copy password.
                    intent.action = ACTION_NEW_NOTIFICATION
                    intent.putExtra(EXTRA_ENTRY_INFO, entry)
                    // Construct notification fields
                    val notificationFields = ArrayList<ClipboardEntryNotificationField>()
                    // Add username if exists to notifications
                    if (containsUsernameToCopy)
                        notificationFields.add(
                                ClipboardEntryNotificationField(
                                        ClipboardEntryNotificationField.NotificationFieldId.USERNAME,
                                        context.getString(R.string.entry_user_name)))
                    // Add password to notifications
                    if (containsPasswordToCopy) {
                        notificationFields.add(
                                ClipboardEntryNotificationField(
                                        ClipboardEntryNotificationField.NotificationFieldId.PASSWORD,
                                        context.getString(R.string.entry_password)))
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
                                                    field.name))
                                    anonymousFieldNumber++
                                }
                            }
                        } catch (e: ArrayIndexOutOfBoundsException) {
                            Log.w("NotificationEntryCopyMg", "Only " + ClipboardEntryNotificationField.NotificationFieldId.anonymousFieldId.size +
                                    " anonymous notifications are available")
                        }

                    }
                    // Add notifications
                    startService = true
                    intent.putParcelableArrayListExtra(EXTRA_CLIPBOARD_FIELDS, notificationFields)
                    context.startService(intent)
                }
            }

            if (!startService)
                context.stopService(intent)
        }
    }
}
