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

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.otp.OtpEntryFields.OTP_TOKEN_FIELD
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.ClipboardHelper
import com.kunzisoft.keepass.timeout.TimeoutHelper.NEVER
import com.kunzisoft.keepass.utils.LOCK_ACTION
import com.kunzisoft.keepass.utils.getParcelableExtraCompat
import com.kunzisoft.keepass.utils.getParcelableList
import com.kunzisoft.keepass.utils.putParcelableList

class ClipboardEntryNotificationService : LockNotificationService() {

    override val notificationId = 485
    private var mEntryInfo: EntryInfo? = null
    private var clipboardHelper: ClipboardHelper? = null

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

    private fun stopNotificationAndSendLockIfNeeded() {
        // Clear the entry if define in preferences
        if (PreferencesUtil.isClearClipboardNotificationEnable(this)) {
            sendBroadcast(Intent(LOCK_ACTION))
        }
        // Stop the service
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // Get entry info from intent
        mEntryInfo = intent?.getParcelableExtraCompat(EXTRA_ENTRY_INFO)

        when {
            intent == null -> Log.w(TAG, "null intent")
            ACTION_NEW_NOTIFICATION == intent.action -> {
                newNotification(mEntryInfo?.title, constructListOfField(intent))
            }
            ACTION_CLEAN_CLIPBOARD == intent.action -> {
                mTimerJob?.cancel()
                clipboardHelper?.cleanClipboard()
                stopNotificationAndSendLockIfNeeded()
            }
            else -> for (actionKey in ClipboardEntryNotificationField.allActionKeys) {
                if (actionKey == intent.action) {
                    intent.getParcelableExtraCompat<ClipboardEntryNotificationField>(
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

    private fun constructListOfField(intent: Intent?): MutableList<ClipboardEntryNotificationField> {
        val fieldList = mutableListOf<ClipboardEntryNotificationField>()
        if (intent?.extras?.containsKey(EXTRA_CLIPBOARD_FIELDS) == true) {
            intent.getParcelableList<ClipboardEntryNotificationField>(EXTRA_CLIPBOARD_FIELDS)?.let { retrieveFields ->
                fieldList.clear()
                fieldList.addAll(retrieveFields)
            }
        }
        return fieldList
    }

    private fun getCopyPendingIntent(fieldToCopy: ClipboardEntryNotificationField, fieldsToAdd: MutableList<ClipboardEntryNotificationField>): PendingIntent {
        val copyIntent = Intent(this, ClipboardEntryNotificationService::class.java).apply {
            action = fieldToCopy.actionKey
            putExtra(EXTRA_ENTRY_INFO, mEntryInfo)
            putExtra(fieldToCopy.extraKey, fieldToCopy)
            putParcelableList(EXTRA_CLIPBOARD_FIELDS, fieldsToAdd)
        }
        return PendingIntent.getService(
            this, 0, copyIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
    }

    private fun newNotification(title: String?, fieldsToAdd: MutableList<ClipboardEntryNotificationField>) {
        mTimerJob?.cancel()

        val builder = buildNewNotification()
                .setSmallIcon(R.drawable.notification_ic_clipboard_key_24dp)

        if (title != null)
            builder.setContentTitle(title)

        if (fieldsToAdd.size > 0) {
            val field = fieldsToAdd[0]
            builder.setContentText(getString(R.string.select_to_copy, field.label))
            builder.setContentIntent(getCopyPendingIntent(field, fieldsToAdd))

            // Add extra actions without 1st field
            val fieldsWithoutFirstField = mutableListOf<ClipboardEntryNotificationField>()
            fieldsWithoutFirstField.addAll(fieldsToAdd)
            fieldsWithoutFirstField.remove(field)
            // Add extra actions
            for (fieldToAdd in fieldsWithoutFirstField) {
                builder.addAction(R.drawable.notification_ic_clipboard_key_24dp, fieldToAdd.label,
                        getCopyPendingIntent(fieldToAdd, fieldsToAdd))
            }
        }

        checkNotificationsPermission {
            notificationManager?.notify(notificationId, builder.build())
        }
    }

    private fun copyField(fieldToCopy: ClipboardEntryNotificationField, nextFields: MutableList<ClipboardEntryNotificationField>) {
        mTimerJob?.cancel()

        try {
            var generatedValue = fieldToCopy.getGeneratedValue(mEntryInfo)
            clipboardHelper?.copyToClipboard(
                fieldToCopy.label,
                generatedValue,
                fieldToCopy.isSensitive
            )

            val builder = buildNewNotification()
                    .setSmallIcon(R.drawable.notification_ic_clipboard_key_24dp)
                    .setContentTitle(fieldToCopy.label)

            // New action with next field if click
            if (nextFields.size > 0) {
                val nextField = nextFields[0]
                builder.setContentText(getString(R.string.select_to_copy, nextField.label))
                builder.setContentIntent(getCopyPendingIntent(nextField, nextFields))
            }

            val cleanIntent = Intent(this, ClipboardEntryNotificationService::class.java)
            cleanIntent.action = ACTION_CLEAN_CLIPBOARD
            val cleanPendingIntent = PendingIntent.getService(
                this, 0, cleanIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )
            builder.setDeleteIntent(cleanPendingIntent)

            //Get settings
            val notificationTimeoutMilliSecs = PreferencesUtil.getClipboardTimeout(this)
            if (notificationTimeoutMilliSecs != NEVER) {
                defineTimerJob(builder, notificationTimeoutMilliSecs, {
                    val newGeneratedValue = fieldToCopy.getGeneratedValue(mEntryInfo)
                    // New auto generated value
                    if (generatedValue != newGeneratedValue) {
                        generatedValue = newGeneratedValue
                        clipboardHelper?.copyToClipboard(
                            fieldToCopy.label,
                            generatedValue,
                            fieldToCopy.isSensitive
                        )
                    }
                }) {
                    stopNotificationAndSendLockIfNeeded()
                    // Clean password only if no next field
                    if (nextFields.size <= 0)
                        clipboardHelper?.cleanClipboard()
                }
            } else {
                // No timer
                checkNotificationsPermission {
                    notificationManager?.notify(notificationId, builder.build())
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Clipboard can't be populate", e)
        }
    }

    private fun checkNotificationsPermission(action: () -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED) {
            action.invoke()
        } else {
            showPermissionErrorIfNeeded(this)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        clipboardHelper?.cleanClipboard()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        clipboardHelper?.cleanClipboard()
        super.onDestroy()
    }

    companion object {

        private val TAG = ClipboardEntryNotificationService::class.java.name

        private const val CHANNEL_CLIPBOARD_ID = "com.kunzisoft.keepass.notification.channel.clipboard"

        const val ACTION_NEW_NOTIFICATION = "ACTION_NEW_NOTIFICATION"
        const val EXTRA_ENTRY_INFO = "EXTRA_ENTRY_INFO"
        const val EXTRA_CLIPBOARD_FIELDS = "EXTRA_CLIPBOARD_FIELDS"
        const val ACTION_CLEAN_CLIPBOARD = "ACTION_CLEAN_CLIPBOARD"

        private fun showPermissionErrorIfNeeded(context: Context) {
            if (PreferencesUtil.isClipboardNotificationsEnable(context)) {
                Toast.makeText(context, R.string.warning_copy_permission, Toast.LENGTH_LONG).show()
            }
        }

        fun checkAndLaunchNotification(
            activity: Activity,
            entry: EntryInfo
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED) {
                        launchNotificationIfAllowed(activity, entry)
                } else {
                    showPermissionErrorIfNeeded(activity)
                }
            } else {
                launchNotificationIfAllowed(activity, entry)
            }
        }

        private fun launchNotificationIfAllowed(context: Context, entry: EntryInfo) {

            val containsUsernameToCopy = entry.username.isNotEmpty()
            val containsPasswordToCopy = entry.password.isNotEmpty()
                    && PreferencesUtil.allowCopyProtectedFields(context)
            val containsOTPToCopy = entry.containsCustomField(OTP_TOKEN_FIELD)
            val containsExtraFieldToCopy = entry.customFields.isNotEmpty()
                    && (entry.containsCustomFieldsNotProtected()
                        ||
                        (entry.containsCustomFieldsProtected() && PreferencesUtil.allowCopyProtectedFields(context))
                    )

            var startService = false
            val intent = Intent(context, ClipboardEntryNotificationService::class.java)

            // If notifications enabled in settings
            // Don't if application timeout
            if (PreferencesUtil.isClipboardNotificationsEnable(context)) {
                if (containsUsernameToCopy
                        || containsPasswordToCopy
                        || containsOTPToCopy
                        || containsExtraFieldToCopy) {

                    // username already copied, waiting for user's action before copy password.
                    intent.action = ACTION_NEW_NOTIFICATION
                    intent.putExtra(EXTRA_ENTRY_INFO, entry)
                    // Construct notification fields
                    val notificationFields = mutableListOf<ClipboardEntryNotificationField>()
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
                    // Add OTP
                    if (containsOTPToCopy) {
                        notificationFields.add(
                                ClipboardEntryNotificationField(
                                        ClipboardEntryNotificationField.NotificationFieldId.OTP,
                                        OTP_TOKEN_FIELD))
                    }
                    // Add extra fields
                    if (containsExtraFieldToCopy) {
                        try {
                            var anonymousFieldNumber = 0
                            entry.customFields.forEach { field ->
                                //If value is not protected or allowed
                                if ((!field.protectedValue.isProtected
                                        || PreferencesUtil.allowCopyProtectedFields(context))
                                     && field.name != OTP_TOKEN_FIELD) {
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
                    intent.putParcelableList(EXTRA_CLIPBOARD_FIELDS, notificationFields)
                    context.startService(intent)
                }
            }

            if (!startService)
                context.stopService(intent)
        }

        fun removeNotification(context: Context?) {
            context?.stopService(Intent(context, ClipboardEntryNotificationService::class.java))
        }
    }
}
