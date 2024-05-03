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
import com.kunzisoft.keepass.database.element.database.DatabaseVersioned
import com.kunzisoft.keepass.database.element.group.GroupKDB
import com.kunzisoft.keepass.database.element.group.GroupKDBX
import com.kunzisoft.keepass.database.element.group.GroupVersionedInterface
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.node.*
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.GroupInfo
import com.kunzisoft.keepass.utils.readBooleanCompat
import com.kunzisoft.keepass.utils.readParcelableCompat
import com.kunzisoft.keepass.utils.writeBooleanCompat
import java.util.*
import kotlin.collections.ArrayList

class Group : Node, GroupVersionedInterface<Group, Entry> {

    var groupKDB: GroupKDB? = null
        private set
    var groupKDBX: GroupKDBX? = null
        private set

    // Virtual group is used to defined a detached database group
    var isVirtual = false

    var numberOfChildEntries: Int = 0
    var recursiveNumberOfChildEntries: Int = 0

    /**
     * Use this constructor to copy a Group
     */
    constructor(group: Group) {
        if (group.groupKDB != null) {
            if (this.groupKDB == null)
                this.groupKDB = GroupKDB()
        }
        if (group.groupKDBX != null) {
            if (this.groupKDBX == null)
                this.groupKDBX = GroupKDBX()
        }
        group.groupKDB?.let {
            this.groupKDB?.updateWith(it)
        }
        group.groupKDBX?.let {
            this.groupKDBX?.updateWith(it)
        }
    }

    constructor(group: GroupKDB) {
        this.groupKDBX = null
        this.groupKDB = group
    }

    constructor(group: GroupKDBX) {
        this.groupKDB = null
        this.groupKDBX = group
    }

    constructor(parcel: Parcel) {
        groupKDB = parcel.readParcelableCompat()
        groupKDBX = parcel.readParcelableCompat()
        isVirtual = parcel.readBooleanCompat()
    }

    enum class ChildFilter {
        META_STREAM, EXPIRED;

        companion object {
            fun getDefaults(showExpiredEntries: Boolean): Array<ChildFilter> {
                return if (showExpiredEntries) {
                    arrayOf(META_STREAM)
                } else {
                    arrayOf(META_STREAM, EXPIRED)
                }
            }
        }
    }

    companion object CREATOR : Parcelable.Creator<Group> {
        override fun createFromParcel(parcel: Parcel): Group {
            return Group(parcel)
        }

        override fun newArray(size: Int): Array<Group?> {
            return arrayOfNulls(size)
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(groupKDB, flags)
        dest.writeParcelable(groupKDBX, flags)
        dest.writeBooleanCompat(isVirtual)
    }

    override val nodeId: NodeId<*>
        get() = groupKDBX?.nodeId ?: groupKDB?.nodeId ?: NodeIdUUID()

    override var title: String
        get() = groupKDB?.title ?: groupKDBX?.title ?: ""
        set(value) {
            groupKDB?.title = value
            groupKDBX?.title = value
        }

    override var icon: IconImage
        get() = groupKDB?.icon ?: groupKDBX?.icon ?: IconImage()
        set(value) {
            groupKDB?.icon = value
            groupKDBX?.icon = value
        }

    var tags: Tags
        get() = groupKDBX?.tags ?: Tags()
        set(value) {
            groupKDBX?.tags = value
        }

    var previousParentGroup: UUID = DatabaseVersioned.UUID_ZERO
        get() = groupKDBX?.previousParentGroup ?: DatabaseVersioned.UUID_ZERO
        private set

    fun setPreviousParentGroup(previousParent: Group?) {
        groupKDBX?.previousParentGroup = previousParent?.groupKDBX?.id ?: DatabaseVersioned.UUID_ZERO
    }

    override val type: Type
        get() = Type.GROUP

    override var parent: Group?
        get() {
            groupKDB?.parent?.let {
                return Group(it)
            }
            groupKDBX?.parent?.let {
                return Group(it)
            }
            return null
        }
        set(value) {
            groupKDB?.parent = value?.groupKDB
            groupKDBX?.parent = value?.groupKDBX
        }

    override fun containsParent(): Boolean {
        return groupKDB?.containsParent() ?: groupKDBX?.containsParent() ?: false
    }

    override fun afterAssignNewParent() {
        groupKDB?.afterAssignNewParent()
        groupKDBX?.afterAssignNewParent()
    }

    fun addChildrenFrom(group: Group) {
        group.groupKDB?.getChildEntries()?.forEach { entryToAdd ->
            groupKDB?.addChildEntry(entryToAdd)
            entryToAdd.parent = groupKDB
        }
        group.groupKDB?.getChildGroups()?.forEach { groupToAdd ->
            groupKDB?.addChildGroup(groupToAdd)
            groupToAdd.parent = groupKDB
        }

        group.groupKDBX?.getChildEntries()?.forEach { entryToAdd ->
            groupKDBX?.addChildEntry(entryToAdd)
            entryToAdd.parent = groupKDBX
        }
        group.groupKDBX?.getChildGroups()?.forEach { groupToAdd ->
            groupKDBX?.addChildGroup(groupToAdd)
            groupToAdd.parent = groupKDBX
        }
    }

    override fun touch(modified: Boolean, touchParents: Boolean) {
        groupKDB?.touch(modified, touchParents)
        groupKDBX?.touch(modified, touchParents)
    }

    override fun isContainedIn(container: Group): Boolean {
        var contained: Boolean? = null
        container.groupKDB?.let {
            contained = groupKDB?.isContainedIn(it)
        }
        container.groupKDBX?.let {
            contained = groupKDBX?.isContainedIn(it)
        }
        return contained ?: false
    }

    override fun nodeIndexInParentForNaturalOrder(): Int {
        return groupKDB?.nodeIndexInParentForNaturalOrder()
                ?: groupKDBX?.nodeIndexInParentForNaturalOrder()
                ?: -1
    }

    override var creationTime: DateInstant
        get() = groupKDB?.creationTime ?: groupKDBX?.creationTime ?: DateInstant()
        set(value) {
            groupKDB?.creationTime = value
            groupKDBX?.creationTime = value
        }

    override var lastModificationTime: DateInstant
        get() = groupKDB?.lastModificationTime ?: groupKDBX?.lastModificationTime ?: DateInstant()
        set(value) {
            groupKDB?.lastModificationTime = value
            groupKDBX?.lastModificationTime = value
        }

    override var lastAccessTime: DateInstant
        get() = groupKDB?.lastAccessTime ?: groupKDBX?.lastAccessTime ?: DateInstant()
        set(value) {
            groupKDB?.lastAccessTime = value
            groupKDBX?.lastAccessTime = value
        }

    override var expiryTime: DateInstant
        get() = groupKDB?.expiryTime ?: groupKDBX?.expiryTime ?: DateInstant()
        set(value) {
            groupKDB?.expiryTime = value
            groupKDBX?.expiryTime = value
        }

    override var expires: Boolean
        get() = groupKDB?.expires ?: groupKDBX?.expires ?: false
        set(value) {
            groupKDB?.expires = value
            groupKDBX?.expires = value
        }

    override val isCurrentlyExpires: Boolean
        get() = groupKDB?.isCurrentlyExpires ?: groupKDBX?.isCurrentlyExpires ?: false

    var notes: String?
        get() = groupKDBX?.notes
        set(value) {
            value?.let {
                groupKDBX?.notes = it
            }
        }

    var customData: CustomData
        get() = groupKDBX?.customData ?: CustomData()
        set(value) {
            groupKDBX?.customData = value
        }

    override fun getChildGroups(): List<Group> {
        return groupKDB?.getChildGroups()?.map {
            Group(it)
        } ?:
        groupKDBX?.getChildGroups()?.map {
            Group(it)
        } ?:
        ArrayList()
    }

    fun getFilteredChildGroups(filters: Array<ChildFilter>): List<Group> {
        return groupKDB?.getChildGroups()?.map {
            Group(it).apply {
                this.refreshNumberOfChildEntries(filters)
            }
        } ?:
        groupKDBX?.getChildGroups()?.map {
            Group(it).apply {
                this.refreshNumberOfChildEntries(filters)
            }
        } ?:
        ArrayList()
    }

    override fun getChildEntries(): List<Entry> {
        return groupKDB?.getChildEntries()?.map {
            Entry(it)
        } ?:
        groupKDBX?.getChildEntries()?.map {
            Entry(it)
        } ?:
        ArrayList()
    }

    fun getChildEntriesInfo(database: Database): List<EntryInfo> {
        val entriesInfo = ArrayList<EntryInfo>()
        getChildEntries().forEach { entry ->
            entriesInfo.add(entry.getEntryInfo(database))
        }
        return entriesInfo
    }

    fun getFilteredChildEntries(filters: Array<ChildFilter>): List<Entry> {
        val withoutMetaStream = filters.contains(ChildFilter.META_STREAM)
        val showExpiredEntries = !filters.contains(ChildFilter.EXPIRED)

        // TODO Change KDB parser to remove meta entries
        return groupKDB?.getChildEntries()?.filter {
            (!withoutMetaStream || (withoutMetaStream && !it.isMetaStream()))
                    && (!it.isCurrentlyExpires or showExpiredEntries)
        }?.map {
            Entry(it)
        } ?:
        groupKDBX?.getChildEntries()?.filter {
            !it.isCurrentlyExpires or showExpiredEntries
        }?.map {
            Entry(it)
        } ?:
        ArrayList()
    }

    fun refreshNumberOfChildEntries(filters: Array<ChildFilter> = emptyArray()) {
        this.numberOfChildEntries = getFilteredChildEntries(filters).size
        this.recursiveNumberOfChildEntries = getFilteredChildEntriesInGroups(filters)
    }

    /**
     * @return the cumulative number of entries in the current group and its children
     */
    private fun getFilteredChildEntriesInGroups(filters: Array<ChildFilter>): Int {
        var counter = 0
        getChildGroups().forEach { childGroup ->
            counter += childGroup.getFilteredChildEntriesInGroups(filters)
        }
        return getFilteredChildEntries(filters).size + counter
    }

    /**
     * Filter entries and return children
     * @return List of direct children (one level below) as NodeVersioned
     */
    fun getChildren(): List<Node> {
        return getChildGroups() + getChildEntries()
    }

    fun getFilteredChildren(filters: Array<ChildFilter>): List<Node> {
        val nodes = getFilteredChildGroups(filters) + getFilteredChildEntries(filters)
        refreshNumberOfChildEntries(filters)
        return nodes
    }

    override fun addChildGroup(group: Group) {
        group.groupKDB?.let {
            groupKDB?.addChildGroup(it)
        }
        group.groupKDBX?.let {
            groupKDBX?.addChildGroup(it)
        }
    }

    override fun addChildEntry(entry: Entry) {
        entry.entryKDB?.let {
            groupKDB?.addChildEntry(it)
        }
        entry.entryKDBX?.let {
            groupKDBX?.addChildEntry(it)
        }
    }

    override fun updateChildGroup(group: Group) {
        group.groupKDB?.let {
            groupKDB?.updateChildGroup(it)
        }
        group.groupKDBX?.let {
            groupKDBX?.updateChildGroup(it)
        }
    }

    override fun updateChildEntry(entry: Entry) {
        entry.entryKDB?.let {
            groupKDB?.updateChildEntry(it)
        }
        entry.entryKDBX?.let {
            groupKDBX?.updateChildEntry(it)
        }
    }

    override fun removeChildGroup(group: Group) {
        group.groupKDB?.let {
            groupKDB?.removeChildGroup(it)
        }
        group.groupKDBX?.let {
            groupKDBX?.removeChildGroup(it)
        }
    }

    override fun removeChildEntry(entry: Entry) {
        entry.entryKDB?.let {
            groupKDB?.removeChildEntry(it)
        }
        entry.entryKDBX?.let {
            groupKDBX?.removeChildEntry(it)
        }
    }

    override fun removeChildren() {
        groupKDB?.removeChildren()
        groupKDBX?.removeChildren()
    }

    val allowAddEntryIfIsRoot: Boolean
        get() = groupKDBX != null

    val allowAddNoteInGroup: Boolean
        get() = groupKDBX != null

    /*
      ------------
      KDB Methods
      ------------
     */

    var nodeIdKDB: NodeId<Int>
        get() = groupKDB?.nodeId ?: NodeIdInt()
        set(value) { groupKDB?.nodeId = value }

    fun setNodeId(id: NodeIdInt) {
        groupKDB?.nodeId = id
    }

    /*
      ------------
      KDBX Methods
      ------------
     */

    var nodeIdKDBX: NodeId<UUID>
        get() = groupKDBX?.nodeId ?: NodeIdUUID()
        set(value) { groupKDBX?.nodeId = value }

    fun setNodeId(id: NodeIdUUID) {
        groupKDBX?.nodeId = id
    }

    var searchable: Boolean?
        get() = groupKDBX?.enableSearching
        set(value) {
            groupKDBX?.enableSearching = value
        }

    fun isSearchable(): Boolean {
        return searchable ?: (parent?.isSearchable() ?: true)
    }

    var enableAutoType: Boolean?
        get() = groupKDBX?.enableAutoType
        set(value) {
            groupKDBX?.enableAutoType = value
        }

    var defaultAutoTypeSequence: String
        get() = groupKDBX?.defaultAutoTypeSequence ?: ""
        set(value) {
            groupKDBX?.defaultAutoTypeSequence = value
        }

    fun setExpanded(expanded: Boolean) {
        groupKDBX?.isExpanded = expanded
    }

    /*
      ------------
      Converter
      ------------
     */

    fun getGroupInfo(): GroupInfo {
        val groupInfo = GroupInfo()
        groupInfo.id = groupKDBX?.nodeId?.id
        groupInfo.title = title
        groupInfo.icon = icon
        groupInfo.creationTime = creationTime
        groupInfo.lastModificationTime = lastModificationTime
        groupInfo.expires = expires
        groupInfo.expiryTime = expiryTime
        groupInfo.notes = notes
        groupInfo.searchable = searchable
        groupInfo.enableAutoType = enableAutoType
        groupInfo.defaultAutoTypeSequence = defaultAutoTypeSequence
        groupInfo.tags = tags
        groupInfo.customData = customData
        return groupInfo
    }

    fun setGroupInfo(groupInfo: GroupInfo) {
        title = groupInfo.title
        icon = groupInfo.icon
        // Update date time, creation time stay as is
        lastModificationTime = DateInstant()
        lastAccessTime = DateInstant()
        expires = groupInfo.expires
        expiryTime = groupInfo.expiryTime
        notes = groupInfo.notes
        searchable = groupInfo.searchable
        enableAutoType = groupInfo.enableAutoType
        defaultAutoTypeSequence = groupInfo.defaultAutoTypeSequence
        tags = groupInfo.tags
        customData = groupInfo.customData
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Group

        if (groupKDB != other.groupKDB) return false
        if (groupKDBX != other.groupKDBX) return false

        return true
    }

    override fun hashCode(): Int {
        var result = groupKDB?.hashCode() ?: 0
        result = 31 * result + (groupKDBX?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return groupKDB?.toString() ?: groupKDBX?.toString() ?: "Undefined"
    }


}
