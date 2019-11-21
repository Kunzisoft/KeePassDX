/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */
package com.kunzisoft.keepass.database.element

import android.os.Parcel
import android.os.Parcelable
import org.joda.time.LocalDateTime

/**
 * Abstract class who manage Groups and Entries
 */
abstract class PwNode<IdType, Parent : PwGroupInterface<Parent, Entry>, Entry : PwEntryInterface<Parent>>
    : PwNodeInterface<Parent>, NodeTimeInterface, Parcelable {

    var nodeId: PwNodeId<IdType> = this.initNodeId()

    val id: IdType
        get() = nodeId.id

    protected constructor()

    protected constructor(parcel: Parcel) {
        this.nodeId = parcel.readParcelable(PwNodeId::class.java.classLoader) ?: nodeId
        this.parent = this.readParentParcelable(parcel)
        this.icon = parcel.readParcelable(PwIcon::class.java.classLoader) ?: icon
        this.creationTime = parcel.readParcelable(PwDate::class.java.classLoader) ?: creationTime
        this.lastModificationTime = parcel.readParcelable(PwDate::class.java.classLoader) ?: lastModificationTime
        this.lastAccessTime = parcel.readParcelable(PwDate::class.java.classLoader) ?: lastAccessTime
        this.expiryTime = parcel.readParcelable(PwDate::class.java.classLoader) ?: expiryTime
        this.expires = parcel.readByte().toInt() != 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(nodeId, flags)
        writeParentParcelable(parent, dest, flags)
        dest.writeParcelable(icon, flags)
        dest.writeParcelable(creationTime, flags)
        dest.writeParcelable(lastModificationTime, flags)
        dest.writeParcelable(lastAccessTime, flags)
        dest.writeParcelable(expiryTime, flags)
        dest.writeByte((if (expires) 1 else 0).toByte())
    }

    override fun describeContents(): Int {
        return 0
    }

    protected fun updateWith(source: PwNode<IdType, Parent, Entry>) {
        this.nodeId = copyNodeId(source.nodeId)
        this.parent = source.parent
        this.icon = source.icon
        this.creationTime = PwDate(source.creationTime)
        this.lastModificationTime = PwDate(source.lastModificationTime)
        this.lastAccessTime = PwDate(source.lastAccessTime)
        this.expiryTime = PwDate(source.expiryTime)
        this.expires = source.expires
    }

    protected abstract fun initNodeId(): PwNodeId<IdType>
    protected abstract fun copyNodeId(nodeId: PwNodeId<IdType>): PwNodeId<IdType>
    protected abstract fun readParentParcelable(parcel: Parcel): Parent?
    protected abstract fun writeParentParcelable(parent: Parent?, parcel: Parcel, flags: Int)

    final override var parent: Parent? = null

    override var icon: PwIcon = PwIconStandard()

    final override var creationTime: PwDate = PwDate()

    final override var lastModificationTime: PwDate = PwDate()

    final override var lastAccessTime: PwDate = PwDate()

    final override var expiryTime: PwDate = PwDate.NEVER_EXPIRE

    final override val isCurrentlyExpires: Boolean
        get() = expires
                && LocalDateTime.fromDateFields(expiryTime.date).isBefore(LocalDateTime.now())

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
        val now = PwDate()
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
        if (other !is PwNode<*, *, *>) {
            return false
        }
        return type == other.type && nodeId == other.nodeId
    }

    override fun hashCode(): Int {
        return nodeId.hashCode()
    }
}
