/*
 * Copyright 2020 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.search

import android.os.Parcel
import android.os.Parcelable

/**
 * Parameters for searching strings in the database.
 */
class SearchParameters() : Parcelable{
    var searchQuery: String = ""
    var caseSensitive = false
    var isRegex = false

    var searchInTitles = true
    var searchInUsernames = true
    var searchInPasswords = false
    var searchInUrls = true
    var searchInExpired = false
    var searchInNotes = true
    var searchInOTP = false
    var searchInOther = true
    var searchInUUIDs = false
    var searchInTags = false

    var searchInCurrentGroup = false
    var searchInSearchableGroup = true
    var searchInRecycleBin = false
    var searchInTemplates = false

    constructor(parcel: Parcel) : this() {
        searchQuery = parcel.readString() ?: searchQuery
        caseSensitive = parcel.readByte() != 0.toByte()
        isRegex = parcel.readByte() != 0.toByte()
        searchInTitles = parcel.readByte() != 0.toByte()
        searchInUsernames = parcel.readByte() != 0.toByte()
        searchInPasswords = parcel.readByte() != 0.toByte()
        searchInUrls = parcel.readByte() != 0.toByte()
        searchInExpired = parcel.readByte() != 0.toByte()
        searchInNotes = parcel.readByte() != 0.toByte()
        searchInOTP = parcel.readByte() != 0.toByte()
        searchInOther = parcel.readByte() != 0.toByte()
        searchInUUIDs = parcel.readByte() != 0.toByte()
        searchInTags = parcel.readByte() != 0.toByte()
        searchInCurrentGroup = parcel.readByte() != 0.toByte()
        searchInSearchableGroup = parcel.readByte() != 0.toByte()
        searchInRecycleBin = parcel.readByte() != 0.toByte()
        searchInTemplates = parcel.readByte() != 0.toByte()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(searchQuery)
        parcel.writeByte(if (caseSensitive) 1 else 0)
        parcel.writeByte(if (isRegex) 1 else 0)
        parcel.writeByte(if (searchInTitles) 1 else 0)
        parcel.writeByte(if (searchInUsernames) 1 else 0)
        parcel.writeByte(if (searchInPasswords) 1 else 0)
        parcel.writeByte(if (searchInUrls) 1 else 0)
        parcel.writeByte(if (searchInExpired) 1 else 0)
        parcel.writeByte(if (searchInNotes) 1 else 0)
        parcel.writeByte(if (searchInOTP) 1 else 0)
        parcel.writeByte(if (searchInOther) 1 else 0)
        parcel.writeByte(if (searchInUUIDs) 1 else 0)
        parcel.writeByte(if (searchInTags) 1 else 0)
        parcel.writeByte(if (searchInCurrentGroup) 1 else 0)
        parcel.writeByte(if (searchInSearchableGroup) 1 else 0)
        parcel.writeByte(if (searchInRecycleBin) 1 else 0)
        parcel.writeByte(if (searchInTemplates) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SearchParameters> {
        override fun createFromParcel(parcel: Parcel): SearchParameters {
            return SearchParameters(parcel)
        }

        override fun newArray(size: Int): Array<SearchParameters?> {
            return arrayOfNulls(size)
        }
    }
}
