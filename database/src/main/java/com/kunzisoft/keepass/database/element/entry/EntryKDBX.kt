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
import android.os.ParcelUuid
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.CustomData
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.Tags
import com.kunzisoft.keepass.database.element.binary.AttachmentPool
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.element.database.DatabaseVersioned
import com.kunzisoft.keepass.database.element.group.GroupKDBX
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.element.node.NodeKDBXInterface
import com.kunzisoft.keepass.database.element.node.Type
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.utils.readParcelableCompat
import com.kunzisoft.keepass.utils.readStringIntMap
import com.kunzisoft.keepass.utils.readStringParcelableMap
import com.kunzisoft.keepass.utils.writeStringIntMap
import com.kunzisoft.keepass.utils.writeStringParcelableMap
import com.kunzisoft.keepass.utils.UnsignedLong
import java.util.Date
import java.util.UUID

class EntryKDBX : EntryVersioned<UUID, UUID, GroupKDBX, EntryKDBX>, NodeKDBXInterface {

    // To decode each field not parcelable
    @Transient
    private var mDatabase: DatabaseKDBX? = null
    @Transient
    private var mDecodeRef = false

    override var usageCount = UnsignedLong(0)
    override var locationChanged = DateInstant()
    override var customData = CustomData()
    private var fields = LinkedHashMap<String, ProtectedString>()
    var binaries = LinkedHashMap<String, Int>() // Map<Label, PoolId>
    var foregroundColor = ""
    var backgroundColor = ""
    var overrideURL = ""
    override var tags = Tags()
    override var previousParentGroup: UUID = DatabaseVersioned.UUID_ZERO
    var qualityCheck = true
    var autoType = AutoType()
    var history = ArrayList<EntryKDBX>()
    var additional = ""

    override var expires: Boolean = false

    constructor() : super()

    constructor(parcel: Parcel) : super(parcel) {
        usageCount = UnsignedLong(parcel.readLong())
        locationChanged = parcel.readParcelableCompat() ?: locationChanged
        customData = parcel.readParcelableCompat() ?: CustomData()
        fields = parcel.readStringParcelableMap()
        binaries = parcel.readStringIntMap()
        foregroundColor = parcel.readString() ?: foregroundColor
        backgroundColor = parcel.readString() ?: backgroundColor
        overrideURL = parcel.readString() ?: overrideURL
        tags = parcel.readParcelableCompat() ?: tags
        previousParentGroup = parcel.readParcelableCompat<ParcelUuid>()?.uuid ?: DatabaseVersioned.UUID_ZERO
        autoType = parcel.readParcelableCompat() ?: autoType
        parcel.readTypedList(history, CREATOR)
        additional = parcel.readString() ?: additional
    }

    override fun readParentParcelable(parcel: Parcel): GroupKDBX? {
        return parcel.readParcelableCompat()
    }

    override fun writeParentParcelable(parent: GroupKDBX?, parcel: Parcel, flags: Int) {
        parcel.writeParcelable(parent, flags)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeLong(usageCount.toKotlinLong())
        dest.writeParcelable(locationChanged, flags)
        dest.writeParcelable(customData, flags)
        dest.writeStringParcelableMap(fields, flags)
        dest.writeStringIntMap(binaries)
        dest.writeString(foregroundColor)
        dest.writeString(backgroundColor)
        dest.writeString(overrideURL)
        dest.writeParcelable(tags, flags)
        dest.writeParcelable(ParcelUuid(previousParentGroup), flags)
        dest.writeParcelable(autoType, flags)
        dest.writeTypedList(history)
        dest.writeString(additional)
    }

    /**
     * Update with deep copy of each entry element
     * @param source
     */
    fun updateWith(source: EntryKDBX,
                   copyHistory: Boolean = true,
                   updateParents: Boolean = true) {
        super.updateWith(source, updateParents)
        usageCount = source.usageCount
        locationChanged = DateInstant(source.locationChanged)
        customData = CustomData(source.customData)
        fields.clear()
        fields.putAll(source.fields)
        binaries.clear()
        binaries.putAll(source.binaries)
        foregroundColor = source.foregroundColor
        backgroundColor = source.backgroundColor
        overrideURL = source.overrideURL
        tags = source.tags
        previousParentGroup = source.previousParentGroup
        autoType = AutoType(source.autoType)
        history.clear()
        if (copyHistory)
            history.addAll(source.history)
        additional = source.additional
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

    override val type: Type
        get() = Type.ENTRY

    /**
     * Decode a reference key with the FieldReferencesEngine
     * @param decodeRef
     * @param key
     * @return
     */
    private fun decodeRefKey(decodeRef: Boolean, key: String, recursionLevel: Int): String {
        return fields[key]?.toString()?.let { text ->
            return if (decodeRef) {
                mDatabase?.getFieldReferenceValue(text, recursionLevel) ?: text
            } else text
        } ?: ""
    }

    fun decodeTitleKey(recursionLevel: Int): String {
        return decodeRefKey(mDecodeRef, STR_TITLE, recursionLevel)
    }

    override var title: String
        get() = decodeTitleKey(0)
        set(value) {
            val protect = mDatabase != null && mDatabase!!.memoryProtection.protectTitle
            fields[STR_TITLE] = ProtectedString(protect, value)
        }

    fun decodeUsernameKey(recursionLevel: Int): String {
        return decodeRefKey(mDecodeRef, STR_USERNAME, recursionLevel)
    }

    override var username: String
        get() = decodeUsernameKey(0)
        set(value) {
            val protect = mDatabase != null && mDatabase!!.memoryProtection.protectUserName
            fields[STR_USERNAME] = ProtectedString(protect, value)
        }

    fun decodePasswordKey(recursionLevel: Int): String {
        return decodeRefKey(mDecodeRef, STR_PASSWORD, recursionLevel)
    }

    override var password: String
        get() = decodePasswordKey(0)
        set(value) {
            val protect = mDatabase != null && mDatabase!!.memoryProtection.protectPassword
            fields[STR_PASSWORD] = ProtectedString(protect, value)
        }

    fun decodeUrlKey(recursionLevel: Int): String {
        return decodeRefKey(mDecodeRef, STR_URL, recursionLevel)
    }

    override var url
        get() = decodeUrlKey(0)
        set(value) {
            val protect = mDatabase != null && mDatabase!!.memoryProtection.protectUrl
            fields[STR_URL] = ProtectedString(protect, value)
        }

    fun decodeNotesKey(recursionLevel: Int): String {
        return decodeRefKey(mDecodeRef, STR_NOTES, recursionLevel)
    }

    override var notes: String
        get() = decodeNotesKey(0)
        set(value) {
            val protect = mDatabase != null && mDatabase!!.memoryProtection.protectNotes
            fields[STR_NOTES] = ProtectedString(protect, value)
        }

    fun getCustomFieldValue(label: String): String {
        return decodeRefKey(mDecodeRef, label, 0)
    }

    fun getSize(attachmentPool: AttachmentPool): Long {
        var size = FIXED_LENGTH_SIZE

        for (entry in fields.entries) {
            size += entry.key.length.toLong()
            size += entry.value.length().toLong()
        }

        size += getAttachmentsSize(attachmentPool)

        size += autoType.defaultSequence.length.toLong()
        autoType.doForEachAutoTypeItem { key, value ->
            size += key.length.toLong()
            size += value.length.toLong()
        }

        for (entry in history) {
            size += entry.getSize(attachmentPool)
        }

        size += overrideURL.length.toLong()
        size += tags.toString().length

        return size
    }

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

    fun doForEachDecodedCustomField(action: (field: Field) -> Unit) {
        val iterator = fields.entries.iterator()
        while (iterator.hasNext()) {
            val mapEntry = iterator.next()
            if (!isStandardField(mapEntry.key)) {
                action.invoke(Field(mapEntry.key,
                        ProtectedString(mapEntry.value.isProtected,
                                decodeRefKey(mDecodeRef, mapEntry.key, 0)
                        )
                    )
                )
            }
        }
    }

    fun getFieldValue(label: String): ProtectedString? {
        return fields[label]
    }

    fun getFields(): List<Field> {
        return fields.map { Field(it.key, it.value) }
    }

    fun putField(field: Field) {
        putField(field.name, field.protectedValue)
    }

    fun putField(label: String, value: ProtectedString) {
        fields[label] = value
    }

    fun removeField(name: String) {
        fields.remove(name)
    }

    fun removeAllFields() {
        fields.clear()
    }

    /**
     * It's a list because history labels can be defined multiple times
     */
    fun getAttachments(attachmentPool: AttachmentPool, inHistory: Boolean = false): List<Attachment> {
        val entryAttachmentList = ArrayList<Attachment>()
        for ((label, poolId) in binaries) {
            attachmentPool[poolId]?.let { binary ->
                entryAttachmentList.add(Attachment(label, binary))
            }
        }
        if (inHistory) {
            history.forEach {
                entryAttachmentList.addAll(it.getAttachments(attachmentPool, false))
            }
        }
        return entryAttachmentList
    }

    fun containsAttachment(): Boolean {
        return binaries.isNotEmpty()
    }

    fun putAttachment(attachment: Attachment, attachmentPool: AttachmentPool) {
        binaries[attachment.name] = attachmentPool.put(attachment.binaryData)
    }

    fun removeAttachment(attachment: Attachment) {
        binaries.remove(attachment.name)
    }

    fun removeAttachments() {
        binaries.clear()
    }

    private fun getAttachmentsSize(attachmentPool: AttachmentPool): Long {
        var size = 0L
        for ((label, poolId) in binaries) {
            size += label.length.toLong()
            size += attachmentPool[poolId]?.getSize() ?: 0
        }
        return size
    }

    fun addEntryToHistory(entry: EntryKDBX) {
        history.add(entry)
    }

    fun removeEntryFromHistory(position: Int): EntryKDBX {
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
        usageCount.plusOne()
    }

    companion object {

        const val STR_TITLE = "Title"
        const val STR_USERNAME = "UserName"
        const val STR_PASSWORD = "Password"
        const val STR_URL = "URL"
        const val STR_NOTES = "Notes"

        private const val FIXED_LENGTH_SIZE: Long = 128 // Approximate fixed length size

        fun newCustomNameAllowed(name: String): Boolean {
            return !(name.equals(STR_TITLE, true)
                    || name.equals(STR_USERNAME, true)
                    || name.equals(STR_PASSWORD, true)
                    || name.equals(STR_URL, true)
                    || name.equals(STR_NOTES, true))
        }

        @JvmField
        val CREATOR: Parcelable.Creator<EntryKDBX> = object : Parcelable.Creator<EntryKDBX> {
            override fun createFromParcel(parcel: Parcel): EntryKDBX {
                return EntryKDBX(parcel)
            }

            override fun newArray(size: Int): Array<EntryKDBX?> {
                return arrayOfNulls(size)
            }
        }
    }
}
