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
import com.kunzisoft.keepass.utils.readListCompat
import com.kunzisoft.keepass.utils.writeListCompat

class SearchGroupInfo : GroupInfo {

    private val entriesResult = mutableListOf<EntryInfo>()

    constructor(): super()

    constructor(groupToCopy: SearchGroupInfo): super(groupToCopy) {
        this.entriesResult.clear()
        this.entriesResult.addAll(groupToCopy.entriesResult)
    }

    constructor(parcel: Parcel): super(parcel) {
        parcel.readListCompat(entriesResult)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeListCompat(entriesResult)
    }

    fun addSearchResult(result: EntryInfo) {
        this.entriesResult.add(result)
    }

    fun clearSearchResults() {
        this.entriesResult.clear()
    }

    fun getSearchResults(): List<EntryInfo> = this.entriesResult

    fun numberOfSearchResults(): Int = this.entriesResult.size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as SearchGroupInfo

        return entriesResult == other.entriesResult
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + entriesResult.hashCode()
        return result
    }

    companion object CREATOR : Parcelable.Creator<SearchGroupInfo> {
        override fun createFromParcel(parcel: Parcel): SearchGroupInfo {
            return SearchGroupInfo(parcel)
        }

        override fun newArray(size: Int): Array<SearchGroupInfo?> {
            return arrayOfNulls(size)
        }
    }
}