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
package com.kunzisoft.keepass.database.element

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.utils.readStringParcelableMap
import com.kunzisoft.keepass.utils.writeStringParcelableMap
import java.util.*

class CustomData : Parcelable {

    private val mCustomDataItems = HashMap<String, CustomDataItem>()

    constructor()

    constructor(toCopy: CustomData) {
        mCustomDataItems.clear()
        mCustomDataItems.putAll(toCopy.mCustomDataItems)
    }

    constructor(parcel: Parcel) {
        mCustomDataItems.clear()
        mCustomDataItems.putAll(parcel.readStringParcelableMap())
    }

    fun get(key: String): CustomDataItem? {
        return mCustomDataItems[key]
    }

    fun put(customDataItem: CustomDataItem) {
        mCustomDataItems[customDataItem.key] = customDataItem
    }

    fun containsItemWithValue(value: String): Boolean {
        return mCustomDataItems.any { mapEntry -> mapEntry.value.value.equals(value, true) }
    }

    fun containsItemWithLastModificationTime(): Boolean {
        return mCustomDataItems.any { mapEntry -> mapEntry.value.lastModificationTime != null }
    }

    fun isNotEmpty(): Boolean {
        return mCustomDataItems.isNotEmpty()
    }

    fun doForEachItems(action: (CustomDataItem) -> Unit) {
        for ((_, value) in mCustomDataItems) {
            action.invoke(value)
        }
    }

    override fun toString(): String {
        return mCustomDataItems.toString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeStringParcelableMap(mCustomDataItems, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CustomData> {
        override fun createFromParcel(parcel: Parcel): CustomData {
            return CustomData(parcel)
        }

        override fun newArray(size: Int): Array<CustomData?> {
            return arrayOfNulls(size)
        }
    }
}