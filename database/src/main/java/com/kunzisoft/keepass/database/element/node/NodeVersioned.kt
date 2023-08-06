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
 *
 */
package com.kunzisoft.keepass.database.element.node

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.entry.EntryVersionedInterface
import com.kunzisoft.keepass.database.element.group.GroupVersionedInterface
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.utils.readBooleanCompat
import com.kunzisoft.keepass.utils.readParcelableCompat
import com.kunzisoft.keepass.utils.writeBooleanCompat

/**
 * Abstract class who manage Groups and Entries
 */
abstract class NodeVersioned<IdType, Parent : GroupVersionedInterface<Parent, Entry>, Entry : EntryVersionedInterface<Parent>>
    : NodeVersionedInterface<Parent>, NodeTimeInterface, Parcelable {

    var nodeId: NodeId<IdType> = this.initNodeId()

    val id: IdType
        get() = nodeId.id

    var nodeIndexInParentForNaturalOrder = -1

    protected constructor()

    protected constructor(parcel: Parcel) {
        this.nodeId = parcel.readParcelableCompat() ?: nodeId
        this.parent = this.readParentParcelable(parcel)
        this.icon = parcel.readParcelableCompat() ?: icon
        this.creationTime = parcel.readParcelableCompat() ?: creationTime
        this.lastModificationTime = parcel.readParcelableCompat() ?: lastModificationTime
        this.lastAccessTime = parcel.readParcelableCompat() ?: lastAccessTime
        this.expiryTime = parcel.readParcelableCompat() ?: expiryTime
        this.expires = parcel.readBooleanCompat()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(nodeId, flags)
        writeParentParcelable(parent, dest, flags)
        dest.writeParcelable(icon, flags)
        dest.writeParcelable(creationTime, flags)
        dest.writeParcelable(lastModificationTime, flags)
        dest.writeParcelable(lastAccessTime, flags)
        dest.writeParcelable(expiryTime, flags)
        dest.writeBooleanCompat(expires)
    }

    override fun describeContents(): Int {
        return 0
    }

    protected fun updateWith(source: NodeVersioned<IdType, Parent, Entry>,
                             updateParents: Boolean = true) {
        this.nodeId = copyNodeId(source.nodeId)
        if (updateParents) {
            this.parent = source.parent
        }
        this.icon = source.icon
        this.creationTime = DateInstant(source.creationTime)
        this.lastModificationTime = DateInstant(source.lastModificationTime)
        this.lastAccessTime = DateInstant(source.lastAccessTime)
        this.expiryTime = DateInstant(source.expiryTime)
        this.expires = source.expires
    }

    protected abstract fun initNodeId(): NodeId<IdType>
    protected abstract fun copyNodeId(nodeId: NodeId<IdType>): NodeId<IdType>
    protected abstract fun readParentParcelable(parcel: Parcel): Parent?
    protected abstract fun writeParentParcelable(parent: Parent?, parcel: Parcel, flags: Int)

    final override var parent: Parent? = null

    final override var icon: IconImage = IconImage()

    final override var creationTime: DateInstant = DateInstant()

    final override var lastModificationTime: DateInstant = DateInstant()

    final override var lastAccessTime: DateInstant = DateInstant()

    final override var expiryTime: DateInstant = DateInstant.NEVER_EXPIRES

    final override val isCurrentlyExpires: Boolean
        get() = expires && expiryTime.isCurrentlyExpire()

    /**
     * @return true if parent is present (false if not present, can be a root or a detach element)
     */
    override fun containsParent(): Boolean {
        return parent != null
    }

    override fun afterAssignNewParent() {}

    override fun isContainedIn(container: Parent): Boolean {
        if (this == container)
            return true
        var cur = this.parent
        while (cur != null) {
            if (cur == container) {
                return true
            }
            cur = cur.parent
        }
        return false
    }

    override fun touch(modified: Boolean, touchParents: Boolean) {
        val now = DateInstant()
        lastAccessTime = now

        if (modified) {
            lastModificationTime = now
        }

        if (touchParents) {
            parent?.touch(modified, true)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other)
            return true
        if (other == null)
            return false
        if (other !is NodeVersioned<*, *, *>) {
            return false
        }
        return type == other.type && nodeId == other.nodeId
    }

    override fun hashCode(): Int {
        return nodeId.hashCode()
    }

    override fun toString(): String {
        return "$title ($nodeId)"
    }
}
