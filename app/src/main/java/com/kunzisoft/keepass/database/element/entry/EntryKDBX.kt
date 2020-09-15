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
package com.kunzisoft.keepass.database.element.entry

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.database.BinaryPool
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.element.group.GroupKDBX
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.icon.IconImageCustom
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.element.node.NodeKDBXInterface
import com.kunzisoft.keepass.database.element.node.Type
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.utils.ParcelableUtil
import com.kunzisoft.keepass.utils.UnsignedLong
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap

class EntryKDBX : EntryVersioned<UUID, UUID, GroupKDBX, EntryKDBX>, NodeKDBXInterface {

    // To decode each field not parcelable
    @Transient
    private var mDatabase: DatabaseKDBX? = null
    @Transient
    private var mDecodeRef = false

    override var icon: IconImage
        get() {
            return when {
                iconCustom.isUnknown -> super.icon
                else -> iconCustom
            }
        }
        set(value) {
            if (value is IconImageStandard)
                iconCustom = IconImageCustom.UNKNOWN_ICON
            super.icon = value
        }
    var iconCustom = IconImageCustom.UNKNOWN_ICON
    private var customData = LinkedHashMap<String, String>()
    // TODO Private
    var fields = LinkedHashMap<String, ProtectedString>()
    var binaries = LinkedHashMap<String, Int>() // Map<Label, PoolId>
    var foregroundColor = ""
    var backgroundColor = ""
    var overrideURL = ""
    var autoType = AutoType()
    var history = ArrayList<EntryKDBX>()
    var additional = ""
    var tags = ""

    fun getSize(binaryPool: BinaryPool): Long {
        var size = FIXED_LENGTH_SIZE

        for (entry in fields.entries) {
            size += entry.key.length.toLong()
            size += entry.value.length().toLong()
        }

        size += getAttachmentsSize(binaryPool)

        size += autoType.defaultSequence.length.toLong()
        for ((key, value) in autoType.entrySet()) {
            size += key.length.toLong()
            size += value.length.toLong()
        }

        for (entry in history) {
            size += entry.getSize(binaryPool)
        }

        size += overrideURL.length.toLong()
        size += tags.length.toLong()

        return size
    }

    override var expires: Boolean = false

    constructor() : super()

    constructor(parcel: Parcel) : super(parcel) {
        iconCustom = parcel.readParcelable(IconImageCustom::class.java.classLoader) ?: iconCustom
        usageCount = UnsignedLong(parcel.readLong())
        locationChanged = parcel.readParcelable(DateInstant::class.java.classLoader) ?: locationChanged
        customData = ParcelableUtil.readStringParcelableMap(parcel)
        fields = ParcelableUtil.readStringParcelableMap(parcel, ProtectedString::class.java)
        binaries = ParcelableUtil.readStringIntMap(parcel)
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
        dest.writeLong(usageCount.toKotlinLong())
        dest.writeParcelable(locationChanged, flags)
        ParcelableUtil.writeStringParcelableMap(dest, customData)
        ParcelableUtil.writeStringParcelableMap(dest, flags, fields)
        ParcelableUtil.writeStringIntMap(dest, binaries)
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
    fun updateWith(source: EntryKDBX, copyHistory: Boolean = true) {
        super.updateWith(source)
        iconCustom = IconImageCustom(source.iconCustom)
        usageCount = source.usageCount
        locationChanged = DateInstant(source.locationChanged)
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
        if (copyHistory)
            history.addAll(source.history)
        url = source.url
        additional = source.additional
        tags = source.tags
    }

    fun startToManageFieldReferences(database: DatabaseKDBX) {
        this.mDatabase = database
        this.mDecodeRef = true
    }

    fun stopToManageFieldReferences() {
        this.mDatabase = null
        this.mDecodeRef = false
    }

    override fun initNodeId(): NodeId<UUID> {
        return NodeIdUUID()
    }

    override fun copyNodeId(nodeId: NodeId<UUID>): NodeId<UUID> {
        return NodeIdUUID(nodeId.id)
    }

    override fun readParentParcelable(parcel: Parcel): GroupKDBX? {
        return parcel.readParcelable(GroupKDBX::class.java.classLoader)
    }

    override fun writeParentParcelable(parent: GroupKDBX?, parcel: Parcel, flags: Int) {
        parcel.writeParcelable(parent, flags)
    }

    /**
     * Decode a reference key with the FieldReferencesEngine
     * @param decodeRef
     * @param key
     * @return
     */
    private fun decodeRefKey(decodeRef: Boolean, key: String): String {
        return fields[key]?.toString()?.let { text ->
            return if (decodeRef) {
                if (mDatabase == null) text else FieldReferencesEngine().compile(text, this, mDatabase!!)
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

    override var usageCount = UnsignedLong(0)

    override var locationChanged = DateInstant()

    fun afterChangeParent() {
        locationChanged = DateInstant()
    }

    private fun isStandardField(key: String): Boolean {
        return (key == STR_TITLE
                || key == STR_USERNAME
                || key == STR_PASSWORD
                || key == STR_URL
                || key == STR_NOTES)
    }

    var customFields = LinkedHashMap<String, ProtectedString>()
        get() {
            field.clear()
            for ((key, value) in fields) {
                if (!isStandardField(key)) {
                    field[key] = ProtectedString(value.isProtected, decodeRefKey(mDecodeRef, key))
                }
            }
            return field
        }

    fun removeAllFields() {
        fields.clear()
    }

    fun putExtraField(label: String, value: ProtectedString) {
        fields[label] = value
    }

    /**
     * It's a list because history labels can be defined multiple times
     */
    fun getAttachments(binaryPool: BinaryPool, inHistory: Boolean = false): List<Attachment> {
        val entryAttachmentList = ArrayList<Attachment>()
        for ((label, poolId) in binaries) {
            binaryPool[poolId]?.let { binary ->
                entryAttachmentList.add(Attachment(label, binary))
            }
        }
        if (inHistory) {
            history.forEach {
                entryAttachmentList.addAll(it.getAttachments(binaryPool, false))
            }
        }
        return entryAttachmentList
    }

    fun containsAttachment(): Boolean {
        return binaries.isNotEmpty()
    }

    fun putAttachment(attachment: Attachment, binaryPool: BinaryPool) {
        binaries[attachment.name] = binaryPool.put(attachment.binaryAttachment)
    }

    fun removeAttachment(attachment: Attachment) {
        binaries.remove(attachment.name)
    }

    private fun getAttachmentsSize(binaryPool: BinaryPool): Long {
        var size = 0L
        for ((label, poolId) in binaries) {
            size += label.length.toLong()
            size += binaryPool[poolId]?.length() ?: 0
        }
        return size
    }

    // TODO Remove ?
    fun sizeOfHistory(): Int {
        return history.size
    }

    override fun putCustomData(key: String, value: String) {
        customData[key] = value
    }

    override fun containsCustomData(): Boolean {
        return customData.isNotEmpty()
    }

    fun addEntryToHistory(entry: EntryKDBX) {
        history.add(entry)
    }

    fun removeEntryFromHistory(position: Int): EntryKDBX? {
        return history.removeAt(position)
    }

    fun removeOldestEntryFromHistory(): EntryKDBX? {
        var min: Date? = null
        var index = -1

        for (i in history.indices) {
            val entry = history[i]
            val lastMod = entry.lastModificationTime.date
            if (min == null  || lastMod.before(min)) {
                index = i
                min = lastMod
            }
        }

        return if (index != -1) {
            history.removeAt(index)
        } else null
    }

    override fun touch(modified: Boolean, touchParents: Boolean) {
        super.touch(modified, touchParents)
        // TODO unsigned long
        usageCount = UnsignedLong(usageCount.toKotlinLong() + 1)
    }

    companion object {

        const val STR_TITLE = "Title"
        const val STR_USERNAME = "UserName"
        const val STR_PASSWORD = "Password"
        const val STR_URL = "URL"
        const val STR_NOTES = "Notes"

        @JvmField
        val CREATOR: Parcelable.Creator<EntryKDBX> = object : Parcelable.Creator<EntryKDBX> {
            override fun createFromParcel(parcel: Parcel): EntryKDBX {
                return EntryKDBX(parcel)
            }

            override fun newArray(size: Int): Array<EntryKDBX?> {
                return arrayOfNulls(size)
            }
        }

        private const val FIXED_LENGTH_SIZE: Long = 128 // Approximate fixed length size
    }
}
