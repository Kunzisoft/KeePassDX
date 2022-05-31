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

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.otp.OtpEntryFields.OTP_TOKEN_FIELD
import java.util.*

/**
 * Utility class to manage fields in Notifications
 */
class ClipboardEntryNotificationField : Parcelable {

    private var id: NotificationFieldId = NotificationFieldId.UNKNOWN
    var label: String = ""
    val isSensitive: Boolean
        get() {
            return id == NotificationFieldId.PASSWORD
        }

    val actionKey: String
        get() = getActionKey(id)

    val extraKey: String
        get() = getExtraKey(id)

    constructor(id: NotificationFieldId, label: String) {
        this.id = id
        this.label = label
    }

    constructor(parcel: Parcel) {
        id = NotificationFieldId.values()[parcel.readInt()]
        label = parcel.readString() ?: label
    }

    fun getGeneratedValue(entryInfo: EntryInfo?): String {
        return when (id) {
            NotificationFieldId.UNKNOWN -> ""
            NotificationFieldId.USERNAME -> entryInfo?.username ?: ""
            NotificationFieldId.PASSWORD -> entryInfo?.password ?: ""
            NotificationFieldId.OTP -> entryInfo?.getGeneratedFieldValue(OTP_TOKEN_FIELD) ?: ""
            NotificationFieldId.FIELD_A,
            NotificationFieldId.FIELD_B,
            NotificationFieldId.FIELD_C -> entryInfo?.getGeneratedFieldValue(label) ?: ""
        }
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id.ordinal)
        dest.writeString(label)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val field = other as ClipboardEntryNotificationField
        return id == field.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    enum class NotificationFieldId {
        UNKNOWN, USERNAME, PASSWORD, OTP, FIELD_A, FIELD_B, FIELD_C;

        companion object {
            val anonymousFieldId: Array<NotificationFieldId>
                get() = arrayOf(FIELD_A, FIELD_B, FIELD_C)
        }
    }

    companion object {

        private val TAG = ClipboardEntryNotificationField::class.java.name

        @JvmField
        val CREATOR: Parcelable.Creator<ClipboardEntryNotificationField> = object : Parcelable.Creator<ClipboardEntryNotificationField> {
            override fun createFromParcel(`in`: Parcel): ClipboardEntryNotificationField {
                return ClipboardEntryNotificationField(`in`)
            }

            override fun newArray(size: Int): Array<ClipboardEntryNotificationField?> {
                return arrayOfNulls(size)
            }
        }

        private const val ACTION_COPY_PREFIX = "ACTION_COPY_"
        private const val EXTRA_KEY_PREFIX = "EXTRA_"

        /**
         * Return EXTRA_KEY link to ACTION_KEY, or null if ACTION_KEY is unknown
         */
        fun getExtraKeyLinkToActionKey(actionKey: String): String? {
            try {
                if (actionKey.startsWith(ACTION_COPY_PREFIX)) {
                    val idName = actionKey.substring(ACTION_COPY_PREFIX.length, actionKey.length)
                    return getExtraKey(NotificationFieldId.valueOf(idName))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Can't get Extra Key from Action Key", e)
            }

            return null
        }

        private fun getActionKey(id: NotificationFieldId): String {
            return ACTION_COPY_PREFIX + id.name
        }

        private fun getExtraKey(id: NotificationFieldId): String {
            return EXTRA_KEY_PREFIX + id.name
        }

        val allActionKeys: List<String>
            get() {
                val actionKeys = ArrayList<String>()
                for (id in NotificationFieldId.values()) {
                    actionKeys.add(getActionKey(id))
                }
                return actionKeys
            }
    }
}
