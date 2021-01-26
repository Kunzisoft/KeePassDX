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
package com.kunzisoft.keepass.database.element

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.database.BinaryPool
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.element.entry.EntryKDB
import com.kunzisoft.keepass.database.element.entry.EntryKDBX
import com.kunzisoft.keepass.database.element.entry.EntryVersionedInterface
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.icon.IconImageCustom
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.element.node.Type
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.Field
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.otp.OtpEntryFields
import java.util.*
import kotlin.collections.ArrayList

class Entry : Node, EntryVersionedInterface<Group> {

    var entryKDB: EntryKDB? = null
        private set
    var entryKDBX: EntryKDBX? = null
        private set

    fun updateWith(entry: Entry, copyHistory: Boolean = true) {
        entry.entryKDB?.let {
            this.entryKDB?.updateWith(it)
        }
        entry.entryKDBX?.let {
            this.entryKDBX?.updateWith(it, copyHistory)
        }
    }

    /**
     * Use this constructor to copy an Entry with exact same values
     */
    constructor(entry: Entry, copyHistory: Boolean = true) {
        if (entry.entryKDB != null) {
            this.entryKDB = EntryKDB()
        }
        if (entry.entryKDBX != null) {
            this.entryKDBX = EntryKDBX()
        }
        updateWith(entry, copyHistory)
    }

    constructor(entry: EntryKDB) {
        this.entryKDBX = null
        this.entryKDB = entry
    }

    constructor(entry: EntryKDBX) {
        this.entryKDB = null
        this.entryKDBX = entry
    }

    constructor(parcel: Parcel) {
        entryKDB = parcel.readParcelable(EntryKDB::class.java.classLoader)
        entryKDBX = parcel.readParcelable(EntryKDBX::class.java.classLoader)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(entryKDB, flags)
        dest.writeParcelable(entryKDBX, flags)
    }

    override var nodeId: NodeId<UUID>
        get() = entryKDBX?.nodeId ?: entryKDB?.nodeId ?: NodeIdUUID()
        set(value) {
            entryKDB?.nodeId = value
            entryKDBX?.nodeId = value
        }

    override var title: String
        get() = entryKDB?.title ?: entryKDBX?.title ?: ""
        set(value) {
            entryKDB?.title = value
            entryKDBX?.title = value
        }

    override var icon: IconImage
        get() {
            return entryKDB?.icon ?: entryKDBX?.icon ?: IconImageStandard()
        }
        set(value) {
            entryKDB?.icon = value
            entryKDBX?.icon = value
        }

    override val type: Type
        get() = Type.ENTRY

    override var parent: Group?
        get() {
            entryKDB?.parent?.let {
                return Group(it)
            }
            entryKDBX?.parent?.let {
                return Group(it)
            }
            return null
        }
        set(value) {
            entryKDB?.parent = value?.groupKDB
            entryKDBX?.parent = value?.groupKDBX
        }

    override fun containsParent(): Boolean {
        return entryKDB?.containsParent() ?: entryKDBX?.containsParent() ?: false
    }

    override fun afterAssignNewParent() {
        entryKDBX?.afterChangeParent()
    }

    override fun touch(modified: Boolean, touchParents: Boolean) {
        entryKDB?.touch(modified, touchParents)
        entryKDBX?.touch(modified, touchParents)
    }

    override fun isContainedIn(container: Group): Boolean {
        var contained: Boolean? = false
        container.groupKDB?.let {
            contained = entryKDB?.isContainedIn(it)
        }
        container.groupKDBX?.let {
            contained = entryKDBX?.isContainedIn(it)
        }
        return contained ?: false
    }

    override fun nodeIndexInParentForNaturalOrder(): Int {
        return entryKDB?.nodeIndexInParentForNaturalOrder()
                ?: entryKDBX?.nodeIndexInParentForNaturalOrder()
                ?: -1
    }

    override var creationTime: DateInstant
        get() = entryKDB?.creationTime ?: entryKDBX?.creationTime ?: DateInstant()
        set(value) {
            entryKDB?.creationTime = value
            entryKDBX?.creationTime = value
        }

    override var lastModificationTime: DateInstant
        get() = entryKDB?.lastModificationTime ?: entryKDBX?.lastModificationTime ?: DateInstant()
        set(value) {
            entryKDB?.lastModificationTime = value
            entryKDBX?.lastModificationTime = value
        }

    override var lastAccessTime: DateInstant
        get() = entryKDB?.lastAccessTime ?: entryKDBX?.lastAccessTime ?: DateInstant()
        set(value) {
            entryKDB?.lastAccessTime = value
            entryKDBX?.lastAccessTime = value
        }

    override var expiryTime: DateInstant
        get() = entryKDB?.expiryTime ?: entryKDBX?.expiryTime ?: DateInstant()
        set(value) {
            entryKDB?.expiryTime = value
            entryKDBX?.expiryTime = value
        }

    override var expires: Boolean
        get() = entryKDB?.expires ?: entryKDBX?.expires ?: false
        set(value) {
            entryKDB?.expires = value
            entryKDBX?.expires = value
        }

    override val isCurrentlyExpires: Boolean
        get() = entryKDB?.isCurrentlyExpires ?: entryKDBX?.isCurrentlyExpires ?: false

    override var username: String
        get() = entryKDB?.username ?: entryKDBX?.username ?: ""
        set(value) {
            entryKDB?.username = value
            entryKDBX?.username = value
        }

    override var password: String
        get() = entryKDB?.password ?: entryKDBX?.password ?: ""
        set(value) {
            entryKDB?.password = value
            entryKDBX?.password = value
        }

    override var url: String
        get() = entryKDB?.url ?: entryKDBX?.url ?: ""
        set(value) {
            entryKDB?.url = value
            entryKDBX?.url = value
        }

    override var notes: String
        get() = entryKDB?.notes ?: entryKDBX?.notes ?: ""
        set(value) {
            entryKDB?.notes = value
            entryKDBX?.notes = value
        }

    private fun isTan(): Boolean {
        return title == PMS_TAN_ENTRY && username.isNotEmpty()
    }

    /**
     * {@inheritDoc}
     * Get the display title from an entry, <br></br>
     * [.startManageEntry] and [.stopManageEntry] must be called
     * before and after [.getVisualTitle]
     */
    fun getVisualTitle(): String {
        return if (isTan()) {
            "$PMS_TAN_ENTRY $username"
        } else {
            if (title.isEmpty())
                if (url.isEmpty())
                    if (username.isEmpty())
                            nodeId.toString()
                    else
                        username
                else
                    url
            else
                title
        }
    }

    /*
      ------------
      KDB Methods
      ------------
     */

    /**
     * If it's a node with only meta information like Meta-info SYSTEM Database Color
     * @return false by default, true if it's a meta stream
     */
    val isMetaStream: Boolean
        get() = entryKDB?.isMetaStream ?: false

    /*
      ------------
      KDBX Methods
      ------------
     */

    var iconCustom: IconImageCustom
        get() = entryKDBX?.iconCustom ?: IconImageCustom.UNKNOWN_ICON
        set(value) {
            entryKDBX?.iconCustom = value
        }

    /**
     * Retrieve extra fields to show, key is the label, value is the value of field (protected or not)
     * @return Map of label/value
     */
    fun getExtraFields(): List<Field> {
        val extraFields = ArrayList<Field>()
        entryKDBX?.let {
            for (field in it.customFields) {
                extraFields.add(Field(field.key, field.value))
            }
        }
        return extraFields
    }

    /**
     * Update or add an extra field to the list (standard or custom)
     */
    fun putExtraField(field: Field) {
        entryKDBX?.putExtraField(field.name, field.protectedValue)
    }

    private fun addExtraFields(fields: List<Field>) {
        fields.forEach {
            putExtraField(it)
        }
    }

    private fun removeAllFields() {
        entryKDBX?.removeAllFields()
    }

    fun getOtpElement(): OtpElement? {
        entryKDBX?.let {
            return OtpEntryFields.parseFields { key ->
                it.customFields[key]?.toString()
            }
        }
        return null
    }

    fun startToManageFieldReferences(database: DatabaseKDBX) {
        entryKDBX?.startToManageFieldReferences(database)
    }

    fun stopToManageFieldReferences() {
        entryKDBX?.stopToManageFieldReferences()
    }

    fun getAttachments(binaryPool: BinaryPool, inHistory: Boolean = false): List<Attachment> {
        val attachments = ArrayList<Attachment>()
        entryKDB?.getAttachment()?.let {
            attachments.add(it)
        }
        entryKDBX?.getAttachments(binaryPool, inHistory)?.let {
            attachments.addAll(it)
        }
        return attachments
    }

    fun containsAttachment(): Boolean {
        return entryKDB?.containsAttachment() == true
                || entryKDBX?.containsAttachment() == true
    }

    private fun addAttachments(binaryPool: BinaryPool, attachments: List<Attachment>) {
        attachments.forEach {
            putAttachment(it, binaryPool)
        }
    }

    private fun removeAttachment(attachment: Attachment) {
        entryKDB?.removeAttachment(attachment)
        entryKDBX?.removeAttachment(attachment)
    }

    private fun removeAllAttachments() {
        entryKDB?.removeAttachment()
        entryKDBX?.removeAttachments()
    }

    private fun putAttachment(attachment: Attachment, binaryPool: BinaryPool) {
        entryKDB?.putAttachment(attachment)
        entryKDBX?.putAttachment(attachment, binaryPool)
    }

    fun getHistory(): ArrayList<Entry> {
        val history = ArrayList<Entry>()
        val entryKDBXHistory = entryKDBX?.history ?: ArrayList()
        for (entryHistory in entryKDBXHistory) {
            history.add(Entry(entryHistory))
        }
        return history
    }

    fun addEntryToHistory(entry: Entry) {
        entry.entryKDBX?.let {
            entryKDBX?.addEntryToHistory(it)
        }
    }

    fun removeEntryFromHistory(position: Int): Entry? {
        entryKDBX?.removeEntryFromHistory(position)?.let {
            return Entry(it)
        }
        return null
    }

    fun removeOldestEntryFromHistory(): Entry? {
         entryKDBX?.removeOldestEntryFromHistory()?.let {
            return Entry(it)
        }
        return null
    }

    fun getSize(binaryPool: BinaryPool): Long {
        return entryKDBX?.getSize(binaryPool) ?: 0L
    }

    fun containsCustomData(): Boolean {
        return entryKDBX?.containsCustomData() ?: false
    }

    /*
      ------------
      Converter
      ------------
     */

    /**
     * Retrieve generated entry info.
     * If are not [raw] data, remove parameter fields and add auto generated elements in auto custom fields
     */
    fun getEntryInfo(database: Database?, raw: Boolean = false): EntryInfo {
        val entryInfo = EntryInfo()
        if (raw)
            database?.stopManageEntry(this)
        else
            database?.startManageEntry(this)

        entryInfo.id = nodeId.toString()
        entryInfo.title = title
        entryInfo.icon = icon
        entryInfo.username = username
        entryInfo.password = password
        entryInfo.creationTime = creationTime
        entryInfo.modificationTime = lastModificationTime
        entryInfo.expires = expires
        entryInfo.expiryTime = expiryTime
        entryInfo.url = url
        entryInfo.notes = notes
        entryInfo.customFields = getExtraFields()
        // Add otpElement to generate token
        entryInfo.otpModel = getOtpElement()?.otpModel
        if (!raw) {
            // Replace parameter fields by generated OTP fields
            entryInfo.customFields = OtpEntryFields.generateAutoFields(entryInfo.customFields)
        }
        database?.binaryPool?.let { binaryPool ->
            entryInfo.attachments = getAttachments(binaryPool)
        }

        if (!raw)
            database?.stopManageEntry(this)
        return entryInfo
    }

    fun setEntryInfo(database: Database?, newEntryInfo: EntryInfo) {
        database?.startManageEntry(this)

        removeAllFields()
        removeAllAttachments()
        // NodeId stay as is
        title = newEntryInfo.title
        icon = newEntryInfo.icon
        username = newEntryInfo.username
        password = newEntryInfo.password
        // Update date time, creation time stay as is
        lastModificationTime = DateInstant()
        lastAccessTime = DateInstant()
        expires = newEntryInfo.expires
        expiryTime = newEntryInfo.expiryTime
        url = newEntryInfo.url
        notes = newEntryInfo.notes
        addExtraFields(newEntryInfo.customFields)
        database?.binaryPool?.let { binaryPool ->
            addAttachments(binaryPool, newEntryInfo.attachments)
        }

        database?.stopManageEntry(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Entry

        if (entryKDB != other.entryKDB) return false
        if (entryKDBX != other.entryKDBX) return false

        return true
    }

    override fun hashCode(): Int {
        var result = entryKDB?.hashCode() ?: 0
        result = 31 * result + (entryKDBX?.hashCode() ?: 0)
        return result
    }


    companion object CREATOR : Parcelable.Creator<Entry> {
        override fun createFromParcel(parcel: Parcel): Entry {
            return Entry(parcel)
        }

        override fun newArray(size: Int): Array<Entry?> {
            return arrayOfNulls(size)
        }

        const val PMS_TAN_ENTRY = "<TAN>"

        /**
         * True if [field] name is not a standard field name
         */
        fun newExtraFieldNameAllowed(field: Field): Boolean {
            return EntryKDBX.newCustomNameAllowed(field.name)
        }
    }
}
