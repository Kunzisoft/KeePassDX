/*
 * Copyright 2026 Jeremy Jamet / Kunzisoft.
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

/**
 * Data model for an entry with sorting information.
 * Extends [EntryInfo] and implements [SortedNodeInfo] to include path and index within its parent.
 */
class SortedEntryInfo : EntryInfo, SortedNodeInfo {

    /**
     * The index of this entry within its parent group.
     */
    override var indexInParent: Int = -1

    /**
     * The full path of the entry in the database hierarchy.
     */
    override var path: String? = null

    /**
     * Primary constructor.
     * @param entryToCopy The [EntryInfo] to copy.
     * @param indexInParent The position of the entry in its parent.
     * @param path The path of the entry.
     */
    constructor(
        entryToCopy: EntryInfo,
        indexInParent: Int = -1,
        path: String? = null,
    ): super(entryToCopy) {
        this.indexInParent = indexInParent
        this.path = path
    }

    /**
     * Parcelable constructor.
     * @param parcel The parcel to read from.
     */
    constructor(parcel: Parcel): super(parcel) {
        indexInParent = parcel.readInt()
        path = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeInt(indexInParent)
        parcel.writeString(path)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SortedEntryInfo) return false
        if (!super.equals(other)) return false

        if (indexInParent != other.indexInParent) return false
        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + indexInParent.hashCode()
        result = 31 * result + path.hashCode()
        return result
    }

    companion object CREATOR : Parcelable.Creator<SortedEntryInfo> {
        override fun createFromParcel(parcel: Parcel): SortedEntryInfo {
            return SortedEntryInfo(parcel)
        }

        override fun newArray(size: Int): Array<SortedEntryInfo?> {
            return arrayOfNulls(size)
        }
    }
}