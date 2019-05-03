package com.kunzisoft.keepass.database.element

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.database.ExtraFields
import com.kunzisoft.keepass.database.security.ProtectedString
import java.util.*

class EntryVersioned : NodeVersioned, PwEntryInterface<GroupVersioned> {

    var pwEntryV3: PwEntryV3? = null
        private set
    var pwEntryV4: PwEntryV4? = null
        private set

    fun updateWith(entry: EntryVersioned) {
        this.pwEntryV3?.updateWith(entry.pwEntryV3)
        this.pwEntryV4?.updateWith(entry.pwEntryV4)
    }

    constructor()

    /**
     * Use this constructor to copy an Entry
     */
    constructor(entry: EntryVersioned) {
        if (entry.pwEntryV3 != null) {
            if (this.pwEntryV3 != null)
                this.pwEntryV3 = PwEntryV3()
        }
        if (entry.pwEntryV4 != null) {
            if (this.pwEntryV4 != null)
                this.pwEntryV4 = PwEntryV4()
        }
        updateWith(entry)
    }

    constructor(entry: PwEntryV3) {
        this.pwEntryV4 = null
        if (this.pwEntryV3 != null)
            this.pwEntryV3 = PwEntryV3()
        this.pwEntryV3?.updateWith(entry)
    }

    constructor(entry: PwEntryV4) {
        this.pwEntryV3 = null
        if (this.pwEntryV4 != null)
            this.pwEntryV4 = PwEntryV4()
        this.pwEntryV4?.updateWith(entry)
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

    var nodeId: PwNodeId<UUID>?
        get() = pwEntryV4?.nodeId ?: pwEntryV3?.nodeId
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
        get() = pwEntryV3?.icon ?: pwEntryV4?.icon ?: PwIconStandard()
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

    override fun touch(modified: Boolean, touchParents: Boolean) {
        pwEntryV3?.touch(modified, touchParents)
        pwEntryV4?.touch(modified, touchParents)
    }

    override fun isContainedIn(container: GroupVersioned): Boolean {
        return pwEntryV3?.isContainedIn(container.pwGroupV3) ?: pwEntryV4?.isContainedIn(container.pwGroupV4) ?: false
    }

    override val isSearchingEnabled: Boolean
        get() = pwEntryV3?.isSearchingEnabled ?: pwEntryV4?.isSearchingEnabled ?: false

    override fun getLastModificationTime(): PwDate? {
        return pwEntryV3?.lastModificationTime ?: pwEntryV4?.lastModificationTime
    }

    override fun setLastModificationTime(date: PwDate) {
        pwEntryV3?.lastModificationTime = date
        pwEntryV4?.lastModificationTime = date
    }

    override fun getCreationTime(): PwDate? {
        return pwEntryV3?.creationTime ?: pwEntryV4?.creationTime
    }

    override fun setCreationTime(date: PwDate) {
        pwEntryV3?.creationTime = date
        pwEntryV4?.creationTime = date
    }

    override fun getLastAccessTime(): PwDate? {
        return pwEntryV3?.lastAccessTime ?: pwEntryV4?.lastAccessTime
    }

    override fun setLastAccessTime(date: PwDate) {
        pwEntryV3?.lastAccessTime = date
        pwEntryV4?.lastAccessTime = date
    }

    override fun getExpiryTime(): PwDate? {
        return pwEntryV3?.expiryTime ?: pwEntryV4?.expiryTime
    }

    override fun setExpiryTime(date: PwDate) {
        pwEntryV3?.expiryTime = date
        pwEntryV4?.expiryTime = date
    }

    override fun isExpires(): Boolean {
        return pwEntryV3?.isExpires ?: pwEntryV4?.isExpires ?: false
    }

    override fun setExpires(exp: Boolean) {
        pwEntryV3?.isExpires = exp
        pwEntryV4?.isExpires = exp
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

    override fun touchLocation() {
        pwEntryV3?.touchLocation()
        pwEntryV4?.touchLocation()
    }

    fun isTan(): Boolean {
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

    fun createBackup(db: PwDatabaseV4?) {
        pwEntryV4?.createBackup(db)
    }

    fun containsCustomData(): Boolean {
        return pwEntryV4?.containsCustomData() ?: false
    }

    /*
      ------------
      Class methods
      ------------
     */

    companion object CREATOR : Parcelable.Creator<EntryVersioned> {
        override fun createFromParcel(parcel: Parcel): EntryVersioned {
            return EntryVersioned(parcel)
        }

        override fun newArray(size: Int): Array<EntryVersioned?> {
            return arrayOfNulls(size)
        }

        val PMS_TAN_ENTRY = "<TAN>"

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
