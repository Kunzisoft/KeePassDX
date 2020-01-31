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

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.group.GroupKDB
import com.kunzisoft.keepass.database.element.group.GroupKDBX
import com.kunzisoft.keepass.database.element.group.GroupVersionedInterface
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import com.kunzisoft.keepass.database.element.node.*
import com.kunzisoft.keepass.settings.PreferencesUtil
import java.util.*
import kotlin.collections.ArrayList

class Group : Node, GroupVersionedInterface<Group, Entry> {

    var groupKDB: GroupKDB? = null
        private set
    var groupKDBX: GroupKDBX? = null
        private set

    fun updateWith(group: Group) {
        group.groupKDB?.let {
            this.groupKDB?.updateWith(it)
        }
        group.groupKDBX?.let {
            this.groupKDBX?.updateWith(it)
        }
    }

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
        updateWith(group)
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
        groupKDB = parcel.readParcelable(GroupKDB::class.java.classLoader)
        groupKDBX = parcel.readParcelable(GroupKDBX::class.java.classLoader)
    }

    enum class ChildFilter {
        META_STREAM, EXPIRED;

        companion object {
            fun getDefaults(context: Context): Array<ChildFilter> {
                return if (PreferencesUtil.showExpiredEntries(context)) {
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
    }

    override val nodeId: NodeId<*>?
        get() = groupKDBX?.nodeId ?: groupKDB?.nodeId

    override var title: String
        get() = groupKDB?.title ?: groupKDBX?.title ?: ""
        set(value) {
            groupKDB?.title = value
            groupKDBX?.title = value
        }

    override var icon: IconImage
        get() = groupKDB?.icon ?: groupKDBX?.icon ?: IconImageStandard()
        set(value) {
            groupKDB?.icon = value
            groupKDBX?.icon = value
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
        }
        group.groupKDB?.getChildGroups()?.forEach { groupToAdd ->
            groupKDB?.addChildGroup(groupToAdd)
        }

        group.groupKDBX?.getChildEntries()?.forEach { entryToAdd ->
            groupKDBX?.addChildEntry(entryToAdd)
        }
        group.groupKDBX?.getChildGroups()?.forEach { groupToAdd ->
            groupKDBX?.addChildGroup(groupToAdd)
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

    override fun getChildGroups(): MutableList<Group> {
        val children = ArrayList<Group>()

        groupKDB?.getChildGroups()?.forEach {
            children.add(Group(it))
        }
        groupKDBX?.getChildGroups()?.forEach {
            children.add(Group(it))
        }

        return children
    }

    override fun getChildEntries(): MutableList<Entry> {
        // To cal function with vararg
        return getChildEntries(*emptyArray<ChildFilter>())
    }

    fun getChildEntries(vararg filter: ChildFilter): MutableList<Entry> {
        val children = ArrayList<Entry>()

        val withoutMetaStream = filter.contains(ChildFilter.META_STREAM)
        val showExpiredEntries = !filter.contains(ChildFilter.EXPIRED)

        groupKDB?.getChildEntries()?.forEach {
            val entryToAddAsChild = Entry(it)
            if ((!withoutMetaStream || (withoutMetaStream && !entryToAddAsChild.isMetaStream))
                    && (!entryToAddAsChild.isCurrentlyExpires or showExpiredEntries))
                children.add(entryToAddAsChild)
        }
        groupKDBX?.getChildEntries()?.forEach {
            val entryToAddAsChild = Entry(it)
            if (!entryToAddAsChild.isCurrentlyExpires or showExpiredEntries)
                children.add(entryToAddAsChild)
        }

        return children
    }

    /**
     * Filter entries and return children
     * @return List of direct children (one level below) as NodeVersioned
     */
    fun getChildren(vararg filter: ChildFilter): List<Node> {
        val children = ArrayList<Node>()
        children.addAll(getChildGroups())

        groupKDB?.let {
            children.addAll(getChildEntries(*filter))
        }
        groupKDBX?.let {
            // No MetasStream in V4
            children.addAll(getChildEntries(*filter))
        }

        return children
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

    override fun allowAddEntryIfIsRoot(): Boolean {
        return groupKDB?.allowAddEntryIfIsRoot() ?: groupKDBX?.allowAddEntryIfIsRoot() ?: false
    }

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

    fun getLevel(): Int {
        return groupKDB?.level ?: -1
    }

    fun setLevel(level: Int) {
        groupKDB?.level = level
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

    fun setEnableAutoType(enableAutoType: Boolean?) {
        groupKDBX?.enableAutoType = enableAutoType
    }

    fun setEnableSearching(enableSearching: Boolean?) {
        groupKDBX?.enableSearching = enableSearching
    }

    fun setExpanded(expanded: Boolean) {
        groupKDBX?.isExpanded = expanded
    }

    fun containsCustomData(): Boolean {
        return groupKDBX?.containsCustomData() ?: false
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
}
