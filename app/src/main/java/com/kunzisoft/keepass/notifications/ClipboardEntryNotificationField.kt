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

import android.content.res.Resources
import android.os.Parcel
import android.os.Parcelable
import android.util.Log

import com.kunzisoft.keepass.R

import java.util.ArrayList

/**
 * Utility class to manage fields in Notifications
 */
open class ClipboardEntryNotificationField : Parcelable {

    private var id: NotificationFieldId = NotificationFieldId.UNKNOWN
    var value: String
    var label: String
    var copyText: String

    val actionKey: String
        get() = getActionKey(id)

    val extraKey: String
        get() = getExtraKey(id)

    constructor(id: NotificationFieldId, value: String, resources: Resources) {
        this.id = id
        this.value = value
        this.label = getLabel(resources)
        this.copyText = getCopyText(resources)
    }

    constructor(id: NotificationFieldId, value: String, label: String, resources: Resources) {
        this.id = id
        this.value = value
        this.label = label
        this.copyText = getCopyText(resources)
    }

    protected constructor(parcel: Parcel) {
        id = NotificationFieldId.values()[parcel.readInt()]
        value = parcel.readString()
        label = parcel.readString()
        copyText = parcel.readString()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id.ordinal)
        dest.writeString(value)
        dest.writeString(label)
        dest.writeString(copyText)
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
        UNKNOWN, USERNAME, PASSWORD, FIELD_A, FIELD_B, FIELD_C;

        companion object {

            val anonymousFieldId: Array<NotificationFieldId>
                get() = arrayOf(FIELD_A, FIELD_B, FIELD_C)
        }
    }

    private fun getLabel(resources: Resources): String {
        return when (id) {
            NotificationFieldId.USERNAME -> resources.getString(R.string.entry_user_name)
            NotificationFieldId.PASSWORD -> resources.getString(R.string.entry_password)
            else -> id.name
        }
    }

    private fun getCopyText(resources: Resources): String {
        return resources.getString(R.string.select_to_copy, label)
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
