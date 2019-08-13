package com.kunzisoft.keepass.notifications

import android.content.Context
import android.content.Intent
import android.util.Log
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.EntryVersioned
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.settings.PreferencesUtil
import java.util.*


object NotificationEntryCopyManager {

    fun launchNotificationIfAllowed(context: Context, firstLaunch: Boolean, entry: EntryVersioned) {
        // Start to manage field reference to copy a value from ref
        val database = Database.getInstance()
        database.startManageEntry(entry)

        val containsUsernameToCopy = entry.username.isNotEmpty()
        val containsPasswordToCopy = entry.password.isNotEmpty()
                && PreferencesUtil.allowCopyPasswordAndProtectedFields(context)
        val containsExtraFieldToCopy = entry.allowExtraFields()
                && (entry.containsCustomFields()
                && entry.containsCustomFieldsNotProtected()
                || (entry.containsCustomFields()
                && entry.containsCustomFieldsProtected()
                && PreferencesUtil.allowCopyPasswordAndProtectedFields(context))
                )

        // If notifications enabled in settings
        // Don't if application timeout
        if (firstLaunch
                && PreferencesUtil.isClipboardNotificationsEnable(context.applicationContext)) {
            if (containsUsernameToCopy || containsPasswordToCopy || containsExtraFieldToCopy) {

                // username already copied, waiting for user's action before copy password.
                val intent = Intent(context, CopyingEntryNotificationService::class.java)
                intent.action = CopyingEntryNotificationService.ACTION_NEW_NOTIFICATION
                intent.putExtra(CopyingEntryNotificationService.EXTRA_ENTRY_TITLE, entry.title)
                // Construct notification fields
                val notificationFields = ArrayList<NotificationCopyingField>()
                // Add username if exists to notifications
                if (containsUsernameToCopy)
                    notificationFields.add(
                            NotificationCopyingField(
                                    NotificationCopyingField.NotificationFieldId.USERNAME,
                                    entry.username,
                                    context.resources))
                // Add password to notifications
                if (containsPasswordToCopy) {
                    notificationFields.add(
                            NotificationCopyingField(
                                    NotificationCopyingField.NotificationFieldId.PASSWORD,
                                    entry.password,
                                    context.resources))
                }
                // Add extra fields
                if (containsExtraFieldToCopy) {
                    try {
                        entry.fields.doActionToAllCustomProtectedField(object : (String, ProtectedString) -> Unit {
                            private var anonymousFieldNumber = 0
                            override fun invoke(key: String, value: ProtectedString) {
                                //If value is not protected or allowed
                                if (!value.isProtected
                                        || PreferencesUtil.allowCopyPasswordAndProtectedFields(context)) {
                                    notificationFields.add(
                                            NotificationCopyingField(
                                                    NotificationCopyingField.NotificationFieldId.anonymousFieldId[anonymousFieldNumber],
                                                    value.toString(),
                                                    key,
                                                    context.resources))
                                    anonymousFieldNumber++
                                }
                            }
                        })
                    } catch (e: ArrayIndexOutOfBoundsException) {
                        Log.w("NotificationEntryCopyMg", "Only " + NotificationCopyingField.NotificationFieldId.anonymousFieldId.size +
                                " anonymous notifications are available")
                    }

                }
                // Add notifications
                intent.putParcelableArrayListExtra(CopyingEntryNotificationService.EXTRA_FIELDS, notificationFields)
                context.startService(intent)
            }
        }
        database.stopManageEntry(entry)
    }
}