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

    fun updateWith(entry: EntryVersioned, copyHistory: Boolean = true) {
        entry.pwEntryV3?.let {
            this.pwEntryV3?.updateWith(it)
        }
        entry.pwEntryV4?.let {
            this.pwEntryV4?.updateWith(it, copyHistory)
        }
    }

    /**
     * Use this constructor to copy an Entry with exact same values
     */
    constructor(entry: EntryVersioned, copyHistory: Boolean = true) {
        if (entry.pwEntryV3 != null) {
            this.pwEntryV3 = PwEntryV3()
        }
        if (entry.pwEntryV4 != null) {
            this.pwEntryV4 = PwEntryV4()
        }
        updateWith(entry, copyHistory)
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
     * Retrieve custom fields to show, key is the label, value is the value of field (protected or not)
     * @return Map of label/value
     */
    val customFields: HashMap<String, ProtectedString>
        get() = pwEntryV4?.customFields ?: HashMap()

    /**
     * To redefine if version of entry allow custom field,
     * @return true if entry allows custom field
     */
    fun allowCustomFields(): Boolean {
        return pwEntryV4?.allowCustomFields() ?: false
    }

    /**
     * Add an extra field to the list (standard or custom)
     * @param label Label of field, must be unique
     * @param value Value of field
     */
    fun addExtraField(label: String, value: ProtectedString) {
        pwEntryV4?.addExtraField(label, value)
    }

    fun startToManageFieldReferences(db: PwDatabaseV4) {
        pwEntryV4?.startToManageFieldReferences(db)
    }

    fun stopToManageFieldReferences() {
        pwEntryV4?.stopToManageFieldReferences()
    }

    fun getHistory(): ArrayList<EntryVersioned> {
        val history = ArrayList<EntryVersioned>()
        val entryV4History = pwEntryV4?.history ?: ArrayList()
        for (entryHistory in entryV4History) {
            history.add(EntryVersioned(entryHistory))
        }
        return history
    }

    fun addEntryToHistory(entry: EntryVersioned) {
        entry.pwEntryV4?.let {
            pwEntryV4?.addEntryToHistory(it)
        }
    }

    fun removeAllHistory() {
        pwEntryV4?.removeAllHistory()
    }

    fun removeOldestEntryFromHistory() {
        pwEntryV4?.removeOldestEntryFromHistory()
    }

    fun getSize(): Long {
        return pwEntryV4?.size ?: 0L
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
        for (entry in customFields.entries) {
            entryInfo.customFields.add(
                    Field(entry.key, entry.value))
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
