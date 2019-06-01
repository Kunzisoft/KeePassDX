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
import org.joda.time.LocalDate

/**
 * Abstract class who manage Groups and Entries
 */
abstract class PwNode<IdType, Parent : PwGroupInterface<Parent, Entry>, Entry : PwEntryInterface<Parent>> : PwNodeInterface<Parent>, Parcelable {

    var nodeId: PwNodeId<IdType> = this.initNodeId()

    private var mParent: Parent? = null
    private var mIcon: PwIcon = PwIconStandard()
    private var mCreationDate = PwDate()
    private var mLastModificationDate = PwDate()
    private var mLastAccessDate = PwDate()
    private var mExpireDate = PwDate.PW_NEVER_EXPIRE

    protected abstract fun initNodeId(): PwNodeId<IdType>
    protected abstract fun copyNodeId(nodeId: PwNodeId<IdType>): PwNodeId<IdType>
    protected abstract fun readParentParcelable(parcel: Parcel): Parent

    protected constructor()

    protected constructor(parcel: Parcel) {
        this.nodeId = parcel.readParcelable(PwNodeId::class.java.classLoader)
        this.mParent = this.readParentParcelable(parcel)
        this.mIcon = parcel.readParcelable(PwIconStandard::class.java.classLoader)
        this.mCreationDate = parcel.readParcelable(PwDate::class.java.classLoader)
        this.mLastModificationDate = parcel.readParcelable(PwDate::class.java.classLoader)
        this.mLastAccessDate = parcel.readParcelable(PwDate::class.java.classLoader)
        this.mExpireDate = parcel.readParcelable(PwDate::class.java.classLoader)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(nodeId, flags)
        dest.writeParcelable(mParent, flags)
        dest.writeParcelable(mIcon, flags)
        dest.writeParcelable(mCreationDate, flags)
        dest.writeParcelable(mLastModificationDate, flags)
        dest.writeParcelable(mLastAccessDate, flags)
        dest.writeParcelable(mExpireDate, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    protected fun updateWith(source: PwNode<IdType, Parent, Entry>) {
        this.nodeId = copyNodeId(source.nodeId)
        this.mParent = source.parent
        this.mIcon = source.icon
        this.mCreationDate = PwDate(source.mCreationDate)
        this.mLastModificationDate = PwDate(source.mLastModificationDate)
        this.mLastAccessDate = PwDate(source.mLastAccessDate)
        this.mExpireDate = PwDate(source.mExpireDate)
    }

    val id: IdType
        get() = nodeId.id

    override var parent: Parent?
        get() = mParent
        set(value) { mParent = value }

    override var icon: PwIcon
        get() = mIcon
        set(value) { mIcon = value }

    override var creationTime: PwDate
        get() = mCreationDate
        set(value) { mCreationDate = value }

    override var lastModificationTime: PwDate
        get() = mLastModificationDate
        set(value) { mLastModificationDate = value }

    override var lastAccessTime: PwDate
        get() = mLastAccessDate
        set(value) { mLastAccessDate = value }

    override var expiryTime: PwDate
        get() = mExpireDate
        set(value) { mExpireDate = value }

    override var isExpires: Boolean
        // If expireDate is before NEVER_EXPIRE date less 1 month (to be sure)
        get() = mExpireDate.date.before(LocalDate.fromDateFields(PwDate.NEVER_EXPIRE).minusMonths(1).toDate())
        set(value) {
            if (!value) {
                mExpireDate = PwDate.PW_NEVER_EXPIRE
            }
        }

    /**
     * @return true if parent is present (false if not present, can be a root or a detach element)
     */
    override fun containsParent(): Boolean {
        return parent != null
    }

    override fun isContainedIn(container: Parent): Boolean {
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
