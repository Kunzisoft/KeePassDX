package com.kunzisoft.keepass.database.element

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.Field
import java.util.*
import kotlin.collections.ArrayList

class EntryVersioned : NodeVersioned, PwEntryInterface<GroupVersioned> {

    var pwEntryV3: PwEntryV3? = null
        private set
    var pwEntryV4: PwEntryV4? = null
        private set

    fun updateWith(entry: EntryVersioned) {
        entry.pwEntryV3?.let {
            this.pwEntryV3?.updateWith(it)
        }
        entry.pwEntryV4?.let {
            this.pwEntryV4?.updateWith(it)
        }
    }

    /**
     * Use this constructor to copy an Entry with exact same values
     */
    constructor(entry: EntryVersioned) {
        if (entry.pwEntryV3 != null) {
            this.pwEntryV3 = PwEntryV3()
        }
        if (entry.pwEntryV4 != null) {
            this.pwEntryV4 = PwEntryV4()
        }
        updateWith(entry)
    }

    constructor(entry: PwEntryV3) {
        this.pwEntryV4 = null
        this.pwEntryV3 = entry
    }

    constructor(entry: PwEntryV4) {
        this.pwEntryV3 = null
        this.pwEntryV4 = entry
    }

    constructor(parcel: Parcel) {
        pwEntryV3 = parcel.readParcelable(PwEntryV3::class.java.classLoader)
        pwEntryV4 = parcel.readParcelable(PwEntryV4::class.java.classLoader)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(pwEntryV3, flags)
        dest.writeParcelable(pwEntryV4, flags)
    }

    var nodeId: PwNodeId<UUID>
        get() = pwEntryV4?.nodeId ?: pwEntryV3?.nodeId ?: PwNodeIdUUID()
        set(value) {
            pwEntryV3?.nodeId = value
            pwEntryV4?.nodeId = value
        }

    override var title: String
        get() = pwEntryV3?.title ?: pwEntryV4?.title ?: ""
        set(value) {
            pwEntryV3?.title = value
            pwEntryV4?.title = value
        }

    override var icon: PwIcon
        get() {
            return pwEntryV3?.icon ?: pwEntryV4?.icon ?: PwIconStandard()
        }
        set(value) {
            pwEntryV3?.icon = value
            pwEntryV4?.icon = value
        }

    override val type: Type
        get() = Type.ENTRY

    override var parent: GroupVersioned?
        get() {
            pwEntryV3?.parent?.let {
                return GroupVersioned(it)
            }
            pwEntryV4?.parent?.let {
                return GroupVersioned(it)
            }
            return null
        }
        set(value) {
            pwEntryV3?.parent = value?.pwGroupV3
            pwEntryV4?.parent = value?.pwGroupV4
        }

    override fun containsParent(): Boolean {
        return pwEntryV3?.containsParent() ?: pwEntryV4?.containsParent() ?: false
    }

    override fun afterAssignNewParent() {
        pwEntryV4?.afterChangeParent()
    }

    override fun touch(modified: Boolean, touchParents: Boolean) {
        pwEntryV3?.touch(modified, touchParents)
        pwEntryV4?.touch(modified, touchParents)
    }

    override fun isContainedIn(container: GroupVersioned): Boolean {
        var contained: Boolean? = false
        container.pwGroupV3?.let {
            contained = pwEntryV3?.isContainedIn(it)
        }
        container.pwGroupV4?.let {
            contained = pwEntryV4?.isContainedIn(it)
        }
        return contained ?: false
    }

    override var creationTime: PwDate
        get() = pwEntryV3?.creationTime ?: pwEntryV4?.creationTime ?: PwDate()
        set(value) {
            pwEntryV3?.creationTime = value
            pwEntryV4?.creationTime = value
        }

    override var lastModificationTime: PwDate
        get() = pwEntryV3?.lastModificationTime ?: pwEntryV4?.lastModificationTime ?: PwDate()
        set(value) {
            pwEntryV3?.lastModificationTime = value
            pwEntryV4?.lastModificationTime = value
        }

    override var lastAccessTime: PwDate
        get() = pwEntryV3?.lastAccessTime ?: pwEntryV4?.lastAccessTime ?: PwDate()
        set(value) {
            pwEntryV3?.lastAccessTime = value
            pwEntryV4?.lastAccessTime = value
        }

    override var expiryTime: PwDate
        get() = pwEntryV3?.expiryTime ?: pwEntryV4?.expiryTime ?: PwDate()
        set(value) {
            pwEntryV3?.expiryTime = value
            pwEntryV4?.expiryTime = value
        }

    override var isExpires: Boolean
        get() =pwEntryV3?.isExpires ?: pwEntryV4?.isExpires ?: false
        set(value) {
            pwEntryV3?.isExpires = value
            pwEntryV4?.isExpires = value
        }

    override var username: String
        get() = pwEntryV3?.username ?: pwEntryV4?.username ?: ""
        set(value) {
            pwEntryV3?.username = value
            pwEntryV4?.username = value
        }

    override var password: String
        get() = pwEntryV3?.password ?: pwEntryV4?.password ?: ""
        set(value) {
            pwEntryV3?.password = value
            pwEntryV4?.password = value
        }

    override var url: String
        get() = pwEntryV3?.url ?: pwEntryV4?.url ?: ""
        set(value) {
            pwEntryV3?.url = value
            pwEntryV4?.url = value
        }

    override var notes: String
        get() = pwEntryV3?.notes ?: pwEntryV4?.notes ?: ""
        set(value) {
            pwEntryV3?.notes = value
            pwEntryV4?.notes = value
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
      V3 Methods
      ------------
     */

    /**
     * If it's a node with only meta information like Meta-info SYSTEM Database Color
     * @return false by default, true if it's a meta stream
     */
    val isMetaStream: Boolean
        get() = pwEntryV3?.isMetaStream ?: false

    /*
      ------------
      V4 Methods
      ------------
     */

    var iconCustom: PwIconCustom
        get() = pwEntryV4?.iconCustom ?: PwIconCustom.UNKNOWN_ICON
        set(value) {
            pwEntryV4?.iconCustom = value
        }

    /**
     * Retrieve extra fields to show, key is the label, value is the value of field
     * @return Map of label/value
     */
    val fields: ExtraFields
        get() = pwEntryV4?.fields ?: ExtraFields()

    /**
     * To redefine if version of entry allow extra field,
     * @return true if entry allows extra field
     */
    fun allowExtraFields(): Boolean {
        return pwEntryV4?.allowExtraFields() ?: false
    }

    /**
     * If entry contains extra fields
     * @return true if there is extra fields
     */
    fun containsCustomFields(): Boolean {
        return pwEntryV4?.containsCustomFields() ?: false
    }

    /**
     * If entry contains extra fields that are protected
     * @return true if there is extra fields protected
     */
    fun containsCustomFieldsProtected(): Boolean {
        return pwEntryV4?.containsCustomFieldsProtected() ?: false
    }

    /**
     * If entry contains extra fields that are not protected
     * @return true if there is extra fields not protected
     */
    fun containsCustomFieldsNotProtected(): Boolean {
        return pwEntryV4?.containsCustomFieldsNotProtected() ?: false
    }

    /**
     * Add an extra field to the list (standard or custom)
     * @param label Label of field, must be unique
     * @param value Value of field
     */
    fun addExtraField(label: String, value: ProtectedString) {
        pwEntryV4?.addExtraField(label, value)
    }

    /**
     * Delete all custom fields
     */
    fun removeAllCustomFields() {
        pwEntryV4?.removeAllCustomFields()
    }

    fun startToManageFieldReferences(db: PwDatabaseV4) {
        pwEntryV4?.startToManageFieldReferences(db)
    }

    fun stopToManageFieldReferences() {
        pwEntryV4?.stopToManageFieldReferences()
    }

    fun addBackupToHistory() {
        pwEntryV4?.let {
            val entryHistory = PwEntryV4()
            entryHistory.updateWith(it)
            it.addEntryToHistory(entryHistory)
        }
    }

    fun removeOldestEntryFromHistory() {
        pwEntryV4?.removeOldestEntryFromHistory()
    }

    fun getHistory(): ArrayList<PwEntryV4> {
        return pwEntryV4?.history ?: ArrayList()
    }

    fun containsCustomData(): Boolean {
        return pwEntryV4?.containsCustomData() ?: false
    }

    /*
      ------------
      Converter
      ------------
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
        if (containsCustomFields()) {
            fields.doActionToAllCustomProtectedField { key, value ->
                        entryInfo.customFields.add(
                                Field(key, value))
                    }
        }
        if (!raw)
            database?.stopManageEntry(this)
        return entryInfo
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EntryVersioned

        if (pwEntryV3 != other.pwEntryV3) return false
        if (pwEntryV4 != other.pwEntryV4) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pwEntryV3?.hashCode() ?: 0
        result = 31 * result + (pwEntryV4?.hashCode() ?: 0)
        return result
    }


    companion object CREATOR : Parcelable.Creator<EntryVersioned> {
        override fun createFromParcel(parcel: Parcel): EntryVersioned {
            return EntryVersioned(parcel)
        }

        override fun newArray(size: Int): Array<EntryVersioned?> {
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
