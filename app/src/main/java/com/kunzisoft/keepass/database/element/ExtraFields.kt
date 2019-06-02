/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.element

import android.os.Parcel
import android.os.Parcelable

import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.utils.MemUtil

import java.util.HashMap

import com.kunzisoft.keepass.database.element.PwEntryV4.Companion.STR_TITLE
import com.kunzisoft.keepass.database.element.PwEntryV4.Companion.STR_USERNAME
import com.kunzisoft.keepass.database.element.PwEntryV4.Companion.STR_PASSWORD
import com.kunzisoft.keepass.database.element.PwEntryV4.Companion.STR_URL
import com.kunzisoft.keepass.database.element.PwEntryV4.Companion.STR_NOTES

class ExtraFields : Parcelable {

    private var fields: MutableMap<String, ProtectedString> = HashMap()

    /**
     * @return list of standard and customized fields
     */
    val listOfAllFields: Map<String, ProtectedString>
        get() = fields

    private val customProtectedFields: Map<String, ProtectedString>
        get() {
            val protectedFields = HashMap<String, ProtectedString>()
            if (fields.isNotEmpty()) {
                for ((key, value) in fields) {
                    if (isNotStandardField(key)) {
                        protectedFields[key] = value
                    }
                }
            }
            return protectedFields
        }

    constructor()

    constructor(extraFields: ExtraFields) : this() {
        for ((key, value) in extraFields.fields) {
            fields[key] = ProtectedString(value)
        }
    }

    constructor(parcel: Parcel) {
        fields = MemUtil.readStringParcelableMap(parcel, ProtectedString::class.java)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        MemUtil.writeStringParcelableMap(dest, flags, fields)
    }

    fun containsCustomFields(): Boolean {
        return customProtectedFields.keys.isNotEmpty()
    }

    fun containsCustomFieldsProtected(): Boolean {
        for ((_, value) in customProtectedFields) {
            if (value.isProtected)
                return true
        }
        return false
    }

    fun containsCustomFieldsNotProtected(): Boolean {
        for ((_, value) in customProtectedFields) {
            if (!value.isProtected)
                return true
        }
        return false
    }

    fun getProtectedStringValue(key: String): String {
        val value = fields[key] ?: return ""
        return value.toString()
    }

    fun putProtectedString(key: String, protectedString: ProtectedString) {
        fields[key] = protectedString
    }

    fun putProtectedString(key: String, value: String, protect: Boolean) {
        val ps = ProtectedString(protect, value)
        fields[key] = ps
    }

    fun doActionToAllCustomProtectedField(actionProtected: (key: String, value: ProtectedString)-> Unit) {
        for ((key, value) in customProtectedFields) {
            actionProtected.invoke(key, value)
        }
    }

    interface ActionProtected {
        fun doAction(key: String, value: ProtectedString)
    }

    fun removeAllCustomFields() {
        val iterator = fields.entries.iterator()
        while (iterator.hasNext()) {
            val pair = iterator.next()
            if (isNotStandardField(pair.key)) {
                iterator.remove()
            }
        }
    }

    companion object {

        @JvmField
        val CREATOR: Parcelable.Creator<ExtraFields> = object : Parcelable.Creator<ExtraFields> {
            override fun createFromParcel(parcel: Parcel): ExtraFields {
                return ExtraFields(parcel)
            }

            override fun newArray(size: Int): Array<ExtraFields?> {
                return arrayOfNulls(size)
            }
        }

        private fun isNotStandardField(key: String): Boolean {
            return (key != STR_TITLE && key != STR_USERNAME
                    && key != STR_PASSWORD && key != STR_URL
                    && key != STR_NOTES)
        }
    }
}
