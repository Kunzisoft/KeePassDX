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
package com.kunzisoft.keepass.model

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.GroupId
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import com.kunzisoft.keepass.database.element.icon.IconImageStandard.Companion.FOLDER_ID
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.utils.readParcelableCompat

/**
 * Data class representing information about a group in the database.
 * This class is used for UI representation and data transfer.
 */
open class GroupInfo : NodeInfo {

    override var nodeId: GroupId = NodeIdUUID()
    var notes: String? = null
    var searchable: Boolean? = null
    var enableAutoType: Boolean? = null
    var defaultAutoTypeSequence: String = ""

    /**
     * Default constructor.
     */
    constructor(): super() {
        icon.standard = IconImageStandard(FOLDER_ID)
    }

    /**
     * Copy constructor.
     * @param groupToCopy The group info to copy.
     */
    constructor(groupToCopy: GroupInfo): super(groupToCopy) {
        this.nodeId = groupToCopy.nodeId
        this.notes = groupToCopy.notes
        this.searchable = groupToCopy.searchable
        this.enableAutoType = groupToCopy.enableAutoType
        this.defaultAutoTypeSequence = groupToCopy.defaultAutoTypeSequence
    }

    /**
     * Parcel constructor.
     * @param parcel The parcel to read from.
     */
    constructor(parcel: Parcel): super(parcel) {
        nodeId = parcel.readParcelableCompat<GroupId>() ?: nodeId
        notes = parcel.readString()
        val isSearchingEnabled = parcel.readInt()
        searchable = if (isSearchingEnabled == -1) null else isSearchingEnabled == 1
        val isAutoTypeEnabled = parcel.readInt()
        enableAutoType = if (isAutoTypeEnabled == -1) null else isAutoTypeEnabled == 1
        defaultAutoTypeSequence = parcel.readString() ?: defaultAutoTypeSequence
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeParcelable(nodeId, flags)
        parcel.writeString(notes)
        parcel.writeInt(if (searchable == null) -1 else if (searchable!!) 1 else 0)
        parcel.writeInt(if (enableAutoType == null) -1 else if (enableAutoType!!) 1 else 0)
        parcel.writeString(defaultAutoTypeSequence)
    }

    /**
     * Check if this group is the root group of the database.
     * @param database The database context.
     * @return True if this is the root group, false otherwise.
     */
    fun isRoot(database: Database?): Boolean {
        if (database == null)
            return false
        database.rootGroup?.let {
            return it.nodeId == this.nodeId
        }
        return false
    }

    /**
     * Check if this group is the recycle bin of the database.
     * @param database The database context.
     * @return True if this is the recycle bin, false otherwise.
     */
    fun isRecycleBin(database: Database?): Boolean {
        if (database == null)
            return false
        if (database.isRecycleBinEnabled) {
            database.recycleBin?.let {
                return it.nodeId == this.nodeId
            }
        }
        return false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupInfo) return false
        if (!super.equals(other)) return false

        if (nodeId != other.nodeId) return false
        if (notes != other.notes) return false
        if (searchable != other.searchable) return false
        if (enableAutoType != other.enableAutoType) return false
        if (defaultAutoTypeSequence != other.defaultAutoTypeSequence) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + nodeId.hashCode()
        result = 31 * result + (notes?.hashCode() ?: 0)
        result = 31 * result + searchable.hashCode()
        result = 31 * result + enableAutoType.hashCode()
        result = 31 * result + defaultAutoTypeSequence.hashCode()
        return result
    }

    companion object CREATOR : Parcelable.Creator<GroupInfo> {
        override fun createFromParcel(parcel: Parcel): GroupInfo {
            return GroupInfo(parcel)
        }

        override fun newArray(size: Int): Array<GroupInfo?> {
            return arrayOfNulls(size)
        }
    }
}