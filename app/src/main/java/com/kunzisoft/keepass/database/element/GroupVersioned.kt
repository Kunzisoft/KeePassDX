package com.kunzisoft.keepass.database.element

import android.os.Parcel
import android.os.Parcelable
import java.util.*
import kotlin.collections.ArrayList

class GroupVersioned : NodeVersioned, PwGroupInterface<GroupVersioned, EntryVersioned> {

    var pwGroupV3: PwGroupV3? = null
        private set
    var pwGroupV4: PwGroupV4? = null
        private set

    fun updateWith(group: GroupVersioned) {
        group.pwGroupV3?.let {
            this.pwGroupV3?.updateWith(it)
        }
        group.pwGroupV4?.let {
            this.pwGroupV4?.updateWith(it)
        }
    }

    /**
     * Use this constructor to copy a Group
     */
    constructor(group: GroupVersioned) {
        if (group.pwGroupV3 != null) {
            if (this.pwGroupV3 == null)
                this.pwGroupV3 = PwGroupV3()
        }
        if (group.pwGroupV4 != null) {
            if (this.pwGroupV4 == null)
                this.pwGroupV4 = PwGroupV4()
        }
        updateWith(group)
    }

    constructor(group: PwGroupV3) {
        this.pwGroupV4 = null
        this.pwGroupV3 = group
    }

    constructor(group: PwGroupV4) {
        this.pwGroupV3 = null
        this.pwGroupV4 = group
    }

    constructor(parcel: Parcel) {
        pwGroupV3 = parcel.readParcelable(PwGroupV3::class.java.classLoader)
        pwGroupV4 = parcel.readParcelable(PwGroupV4::class.java.classLoader)
    }

    companion object CREATOR : Parcelable.Creator<GroupVersioned> {
        override fun createFromParcel(parcel: Parcel): GroupVersioned {
            return GroupVersioned(parcel)
        }

        override fun newArray(size: Int): Array<GroupVersioned?> {
            return arrayOfNulls(size)
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(pwGroupV3, flags)
        dest.writeParcelable(pwGroupV4, flags)
    }

    val nodeId: PwNodeId<*>?
        get() = pwGroupV4?.nodeId ?: pwGroupV3?.nodeId

    override var title: String
        get() = pwGroupV3?.title ?: pwGroupV4?.title ?: ""
        set(value) {
            pwGroupV3?.title = value
            pwGroupV4?.title = value
        }

    override var icon: PwIcon
        get() = pwGroupV3?.icon ?: pwGroupV4?.icon ?: PwIconStandard()
        set(value) {
            pwGroupV3?.icon = value
            pwGroupV4?.icon = value
        }

    override val type: Type
        get() = Type.GROUP

    override var parent: GroupVersioned?
        get() {
            pwGroupV3?.parent?.let {
                return GroupVersioned(it)
            }
            pwGroupV4?.parent?.let {
                return GroupVersioned(it)
            }
            return null
        }
        set(value) {
            pwGroupV3?.parent = value?.pwGroupV3
            pwGroupV4?.parent = value?.pwGroupV4
        }

    override fun containsParent(): Boolean {
        return pwGroupV3?.containsParent() ?: pwGroupV4?.containsParent() ?: false
    }

    override fun afterAssignNewParent() {
        pwGroupV3?.afterAssignNewParent()
        pwGroupV4?.afterAssignNewParent()
    }

    override fun touch(modified: Boolean, touchParents: Boolean) {
        pwGroupV3?.touch(modified, touchParents)
        pwGroupV4?.touch(modified, touchParents)
    }

    override fun isContainedIn(container: GroupVersioned): Boolean {
        var contained: Boolean? = null
        container.pwGroupV3?.let {
            contained = pwGroupV3?.isContainedIn(it)
        }
        container.pwGroupV4?.let {
            contained = pwGroupV4?.isContainedIn(it)
        }
        return contained ?: false
    }

    override val isSearchingEnabled: Boolean
        get() = pwGroupV3?.isSearchingEnabled ?: pwGroupV4?.isSearchingEnabled ?: false

    override var creationTime: PwDate
        get() = pwGroupV3?.creationTime ?: pwGroupV4?.creationTime ?: PwDate()
        set(value) {
            pwGroupV3?.creationTime = value
            pwGroupV4?.creationTime = value
        }

    override var lastModificationTime: PwDate
        get() = pwGroupV3?.lastModificationTime ?: pwGroupV4?.lastModificationTime ?: PwDate()
        set(value) {
            pwGroupV3?.lastModificationTime = value
            pwGroupV4?.lastModificationTime = value
        }

    override var lastAccessTime: PwDate
        get() = pwGroupV3?.lastAccessTime ?: pwGroupV4?.lastAccessTime ?: PwDate()
        set(value) {
            pwGroupV3?.lastAccessTime = value
            pwGroupV4?.lastAccessTime = value
        }

    override var expiryTime: PwDate
        get() = pwGroupV3?.expiryTime ?: pwGroupV4?.expiryTime ?: PwDate()
        set(value) {
            pwGroupV3?.expiryTime = value
            pwGroupV4?.expiryTime = value
        }

    override var isExpires: Boolean
        get() = pwGroupV3?.isExpires ?: pwGroupV4?.isExpires ?: false
        set(value) {
            pwGroupV3?.isExpires = value
            pwGroupV4?.isExpires = value
        }

    override fun getChildGroups(): MutableList<GroupVersioned> {
        val children = ArrayList<GroupVersioned>()

        pwGroupV3?.getChildGroups()?.forEach {
            children.add(GroupVersioned(it))
        }
        pwGroupV4?.getChildGroups()?.forEach {
            children.add(GroupVersioned(it))
        }

        return children
    }

    override fun getChildEntries(): MutableList<EntryVersioned> {
        val children = ArrayList<EntryVersioned>()

        pwGroupV3?.getChildEntries()?.forEach {
            children.add(EntryVersioned(it))
        }
        pwGroupV4?.getChildEntries()?.forEach {
            children.add(EntryVersioned(it))
        }

        return children
    }

    /**
     * Filter MetaStream entries and return children
     * @return List of direct children (one level below) as PwNode
     */
    fun getChildrenWithoutMetaStream(): List<NodeVersioned> {
        val children = ArrayList<NodeVersioned>()
        children.addAll(getChildGroups())

        pwGroupV3?.let {
            children.addAll(getChildEntries().filter { !it.isMetaStream })
        }
        pwGroupV4?.let {
            // No MetasStream in V4
            children.addAll(getChildEntries())
        }

        return children
    }

    override fun addChildGroup(group: GroupVersioned) {
        group.pwGroupV3?.let {
            pwGroupV3?.addChildGroup(it)
        }
        group.pwGroupV4?.let {
            pwGroupV4?.addChildGroup(it)
        }
    }

    override fun addChildEntry(entry: EntryVersioned) {
        entry.pwEntryV3?.let {
            pwGroupV3?.addChildEntry(it)
        }
        entry.pwEntryV4?.let {
            pwGroupV4?.addChildEntry(it)
        }
    }

    override fun removeChildGroup(group: GroupVersioned) {
        group.pwGroupV3?.let {
            pwGroupV3?.removeChildGroup(it)
        }
        group.pwGroupV4?.let {
            pwGroupV4?.removeChildGroup(it)
        }
    }

    override fun removeChildEntry(entry: EntryVersioned) {
        entry.pwEntryV3?.let {
            pwGroupV3?.removeChildEntry(it)
        }
        entry.pwEntryV4?.let {
            pwGroupV4?.removeChildEntry(it)
        }
    }

    override fun allowAddEntryIfIsRoot(): Boolean {
        return pwGroupV3?.allowAddEntryIfIsRoot() ?: pwGroupV4?.allowAddEntryIfIsRoot() ?: false
    }

    /*
      ------------
      V3 Methods
      ------------
     */

    var nodeIdV3: PwNodeId<Int>
        get() = pwGroupV3?.nodeId ?: PwNodeIdInt()
        set(value) { pwGroupV3?.nodeId = value }

    fun setNodeId(id: PwNodeIdInt) {
        pwGroupV3?.nodeId = id
    }

    fun getLevel(): Int {
        return pwGroupV3?.level ?: -1
    }

    fun setLevel(level: Int) {
        pwGroupV3?.level = level
    }

    /*
      ------------
      V4 Methods
      ------------
     */

    var nodeIdV4: PwNodeId<UUID>
        get() = pwGroupV4?.nodeId ?: PwNodeIdUUID()
        set(value) { pwGroupV4?.nodeId = value }

    fun setNodeId(id: PwNodeIdUUID) {
        pwGroupV4?.nodeId = id
    }

    fun setEnableAutoType(enableAutoType: Boolean?) {
        pwGroupV4?.enableAutoType = enableAutoType
    }

    fun setEnableSearching(enableSearching: Boolean?) {
        pwGroupV4?.enableSearching = enableSearching
    }

    fun setExpanded(expanded: Boolean) {
        pwGroupV4?.isExpanded = expanded
    }

    fun containsCustomData(): Boolean {
        return pwGroupV4?.containsCustomData() ?: false
    }
}
