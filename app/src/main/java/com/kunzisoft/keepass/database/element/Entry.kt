package com.kunzisoft.keepass.database.element

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.security.ProtectedString
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

    fun getVisualTitle(): String {
        return getVisualTitle(isTan(),
                title,
                username,
                url,
                nodeId.toString())
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
     * Retrieve custom fields to show, key is the label, value is the value of field (protected or not)
     * @return Map of label/value
     */
    val customFields: HashMap<String, ProtectedString>
        get() = entryKDBX?.customFields ?: HashMap()

    /**
     * To redefine if version of entry allow custom field,
     * @return true if entry allows custom field
     */
    fun allowCustomFields(): Boolean {
        return entryKDBX?.allowCustomFields() ?: false
    }

    fun removeAllFields() {
        entryKDBX?.removeAllFields()
    }

    /**
     * Update or add an extra field to the list (standard or custom)
     * @param label Label of field, must be unique
     * @param value Value of field
     */
    fun putExtraField(label: String, value: ProtectedString) {
        entryKDBX?.putExtraField(label, value)
    }

    fun getOtpElement(): OtpElement? {
        return OtpEntryFields.parseFields { key ->
            customFields[key]?.toString()
        }
    }

    fun startToManageFieldReferences(db: DatabaseKDBX) {
        entryKDBX?.startToManageFieldReferences(db)
    }

    fun stopToManageFieldReferences() {
        entryKDBX?.stopToManageFieldReferences()
    }

    fun getHistory(): ArrayList<Entry> {
        val history = ArrayList<Entry>()
        val entryV4History = entryKDBX?.history ?: ArrayList()
        for (entryHistory in entryV4History) {
            history.add(Entry(entryHistory))
        }
        return history
    }

    fun addEntryToHistory(entry: Entry) {
        entry.entryKDBX?.let {
            entryKDBX?.addEntryToHistory(it)
        }
    }

    fun removeAllHistory() {
        entryKDBX?.removeAllHistory()
    }

    fun removeOldestEntryFromHistory() {
        entryKDBX?.removeOldestEntryFromHistory()
    }

    fun getSize(): Long {
        return entryKDBX?.size ?: 0L
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
     * Retrieve generated entry info,
     * Remove parameter fields and add auto generated elements in auto custom fields
     */
    fun getEntryInfo(database: Database?, raw: Boolean = false): EntryInfo {
        val entryInfo = EntryInfo()
        if (raw)
            database?.stopManageEntry(this)
        else
            database?.startManageEntry(this)
        entryInfo.id = nodeId.toString()
        entryInfo.title = title
        entryInfo.username = username
        entryInfo.password = password
        entryInfo.url = url
        entryInfo.notes = notes
        for (entry in customFields.entries) {
            entryInfo.customFields.add(
                    Field(entry.key, entry.value))
        }
        // Add otpElement to generate token
        entryInfo.otpModel = getOtpElement()?.otpModel
        // Replace parameter fields by generated OTP fields
        entryInfo.customFields = OtpEntryFields.generateAutoFields(entryInfo.customFields)
        if (!raw)
            database?.stopManageEntry(this)
        return entryInfo
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
         * {@inheritDoc}
         * Get the display title from an entry, <br></br>
         * [.startManageEntry] and [.stopManageEntry] must be called
         * before and after [.getVisualTitle]
         */
        fun getVisualTitle(isTan: Boolean, title: String, userName: String, url: String, id: String): String {
            return if (isTan) {
                "$PMS_TAN_ENTRY $userName"
            } else {
                if (title.isEmpty())
                    if (userName.isEmpty())
                        if (url.isEmpty())
                            id
                        else
                            url
                    else
                        userName
                else
                    title
            }
        }
    }
}
