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
package com.kunzisoft.keepass.database.element

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.security.ProtectedBinary
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.utils.MemoryUtil
import java.util.*

class PwEntryV4 : PwEntry<PwGroupV4, PwEntryV4>, PwNodeV4Interface {

    // To decode each field not parcelable
    @Transient
    private var mDatabase: PwDatabaseV4? = null
    @Transient
    private var mDecodeRef = false

    override var icon: PwIcon
        get() {
            return when {
                iconCustom.isUnknown -> super.icon
                else -> iconCustom
            }
        }
        set(value) {
            if (value is PwIconStandard)
                iconCustom = PwIconCustom.UNKNOWN_ICON
            super.icon = value
        }
    var iconCustom = PwIconCustom.UNKNOWN_ICON
    private var customData = HashMap<String, String>()
    var fields = HashMap<String, ProtectedString>()
    val binaries = HashMap<String, ProtectedBinary>()
    var foregroundColor = ""
    var backgroundColor = ""
    var overrideURL = ""
    var autoType = AutoType()
    var history = ArrayList<PwEntryV4>()
    var additional = ""
    var tags = ""

    val size: Long
        get() {
            var size = FIXED_LENGTH_SIZE

            for (entry in fields.entries) {
                size += entry.key.length.toLong()
                size += entry.value.length().toLong()
            }

            for ((key, value) in binaries) {
                size += key.length.toLong()
                size += value.length()
            }

            size += autoType.defaultSequence.length.toLong()
            for ((key, value) in autoType.entrySet()) {
                size += key.length.toLong()
                size += value.length.toLong()
            }

            for (entry in history) {
                size += entry.size
            }

            size += overrideURL.length.toLong()
            size += tags.length.toLong()

            return size
        }

    constructor() : super()

    constructor(parcel: Parcel) : super(parcel) {
        iconCustom = parcel.readParcelable(PwIconCustom::class.java.classLoader) ?: iconCustom
        usageCount = parcel.readLong()
        locationChanged = parcel.readParcelable(PwDate::class.java.classLoader) ?: locationChanged
        customData = MemoryUtil.readStringParcelableMap(parcel)
        fields = MemoryUtil.readStringParcelableMap(parcel, ProtectedString::class.java)
        // TODO binaries = MemoryUtil.readStringParcelableMap(parcel, ProtectedBinary.class);
        foregroundColor = parcel.readString() ?: foregroundColor
        backgroundColor = parcel.readString() ?: backgroundColor
        overrideURL = parcel.readString() ?: overrideURL
        autoType = parcel.readParcelable(AutoType::class.java.classLoader) ?: autoType
        parcel.readTypedList(history, CREATOR)
        url = parcel.readString() ?: url
        additional = parcel.readString() ?: additional
        tags = parcel.readString() ?: tags
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeParcelable(iconCustom, flags)
        dest.writeLong(usageCount)
        dest.writeParcelable(locationChanged, flags)
        MemoryUtil.writeStringParcelableMap(dest, customData)
        MemoryUtil.writeStringParcelableMap(dest, flags, fields)
        // TODO MemoryUtil.writeStringParcelableMap(dest, flags, binaries);
        dest.writeString(foregroundColor)
        dest.writeString(backgroundColor)
        dest.writeString(overrideURL)
        dest.writeParcelable(autoType, flags)
        dest.writeTypedList(history)
        dest.writeString(url)
        dest.writeString(additional)
        dest.writeString(tags)
    }

    /**
     * Update with deep copy of each entry element
     * @param source
     */
    fun updateWith(source: PwEntryV4) {
        super.updateWith(source)
        iconCustom = PwIconCustom(source.iconCustom)
        usageCount = source.usageCount
        locationChanged = PwDate(source.locationChanged)
        // Add all custom elements in map
        customData.clear()
        customData.putAll(source.customData)
        fields.clear()
        fields.putAll(source.fields)
        binaries.clear()
        binaries.putAll(source.binaries)
        foregroundColor = source.foregroundColor
        backgroundColor = source.backgroundColor
        overrideURL = source.overrideURL
        autoType = AutoType(source.autoType)
        history.clear()
        history.addAll(source.history)
        url = source.url
        additional = source.additional
        tags = source.tags
    }

    fun startToManageFieldReferences(db: PwDatabaseV4) {
        this.mDatabase = db
        this.mDecodeRef = true
    }

    fun stopToManageFieldReferences() {
        this.mDatabase = null
        this.mDecodeRef = false
    }

    override fun initNodeId(): PwNodeId<UUID> {
        return PwNodeIdUUID()
    }

    override fun copyNodeId(nodeId: PwNodeId<UUID>): PwNodeId<UUID> {
        return PwNodeIdUUID(nodeId.id)
    }

    override fun readParentParcelable(parcel: Parcel): PwGroupV4? {
        return parcel.readParcelable(PwGroupV4::class.java.classLoader)
    }

    override fun writeParentParcelable(parent: PwGroupV4?, parcel: Parcel, flags: Int) {
        parcel.writeParcelable(parent, flags)
    }

    /**
     * Decode a reference key with the SprEngineV4
     * @param decodeRef
     * @param key
     * @return
     */
    private fun decodeRefKey(decodeRef: Boolean, key: String): String {
        return fields[key]?.toString()?.let { text ->
            return if (decodeRef) {
                if (mDatabase == null) text else SprEngineV4().compile(text, this, mDatabase!!)
            } else text
        } ?: ""
    }

    override var title: String
        get() = decodeRefKey(mDecodeRef, STR_TITLE)
        set(value) {
            val protect = mDatabase != null && mDatabase!!.memoryProtection.protectTitle
            fields[STR_TITLE] = ProtectedString(protect, value)
        }

    override val type: Type
        get() = Type.ENTRY

    override var username: String
        get() = decodeRefKey(mDecodeRef, STR_USERNAME)
        set(value) {
            val protect = mDatabase != null && mDatabase!!.memoryProtection.protectUserName
            fields[STR_USERNAME] = ProtectedString(protect, value)
        }

    override var password: String
        get() = decodeRefKey(mDecodeRef, STR_PASSWORD)
        set(value) {
            val protect = mDatabase != null && mDatabase!!.memoryProtection.protectPassword
            fields[STR_PASSWORD] = ProtectedString(protect, value)
        }

    override var url
        get() = decodeRefKey(mDecodeRef, STR_URL)
        set(value) {
            val protect = mDatabase != null && mDatabase!!.memoryProtection.protectUrl
            fields[STR_URL] = ProtectedString(protect, value)
        }

    override var notes: String
        get() = decodeRefKey(mDecodeRef, STR_NOTES)
        set(value) {
            val protect = mDatabase != null && mDatabase!!.memoryProtection.protectNotes
            fields[STR_NOTES] = ProtectedString(protect, value)
        }

    override var usageCount: Long = 0

    override var locationChanged = PwDate()

    fun afterChangeParent() {
        locationChanged = PwDate()
    }

    private fun isStandardField(key: String): Boolean {
        return (key == STR_TITLE
                || key == STR_USERNAME
                || key == STR_PASSWORD
                || key == STR_URL
                || key == STR_NOTES)
    }

    var customFields = HashMap<String, ProtectedString>()
        get() {
            field.clear()
            for (entry in fields.entries) {
                val key = entry.key
                val value = entry.value
                if (!isStandardField(entry.key)) {
                    field[key] = ProtectedString(value.isProtected, decodeRefKey(mDecodeRef, key))
                }
            }
            return field
        }

    fun allowCustomFields(): Boolean {
        return true
    }

    fun addExtraField(label: String, value: ProtectedString) {
        fields[label] = value
    }

    fun putProtectedBinary(key: String, value: ProtectedBinary) {
        binaries[key] = value
    }

    fun sizeOfHistory(): Int {
        return history.size
    }

    override fun putCustomData(key: String, value: String) {
        customData[key] = value
    }

    override fun containsCustomData(): Boolean {
        return customData.isNotEmpty()
    }

    fun addEntryToHistory(entry: PwEntryV4) {
        history.add(entry)
    }

    fun removeOldestEntryFromHistory() {
        var min: Date? = null
        var index = -1

        for (i in history.indices) {
            val entry = history[i]
            val lastMod = entry.lastModificationTime.date
            if (min == null || lastMod == null || lastMod.before(min)) {
                index = i
                min = lastMod
            }
        }

        if (index != -1) {
            history.removeAt(index)
        }
    }

    override fun touch(modified: Boolean, touchParents: Boolean) {
        super.touch(modified, touchParents)
        ++usageCount
    }

    companion object {

        const val STR_TITLE = "Title"
        const val STR_USERNAME = "UserName"
        const val STR_PASSWORD = "Password"
        const val STR_URL = "URL"
        const val STR_NOTES = "Notes"

        @JvmField
        val CREATOR: Parcelable.Creator<PwEntryV4> = object : Parcelable.Creator<PwEntryV4> {
            override fun createFromParcel(parcel: Parcel): PwEntryV4 {
                return PwEntryV4(parcel)
            }

            override fun newArray(size: Int): Array<PwEntryV4?> {
                return arrayOfNulls(size)
            }
        }

        private const val FIXED_LENGTH_SIZE: Long = 128 // Approximate fixed length size
    }
}
