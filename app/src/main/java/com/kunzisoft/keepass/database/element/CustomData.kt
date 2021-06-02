package com.kunzisoft.keepass.database.element

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.utils.ParcelableUtil
import java.util.*

class CustomData : Parcelable {

    private val mCustomDataItems = HashMap<String, CustomDataItem>()

    constructor()

    constructor(toCopy: CustomData) {
        mCustomDataItems.clear()
        mCustomDataItems.putAll(toCopy.mCustomDataItems)
    }

    constructor(parcel: Parcel) {
        ParcelableUtil.readStringParcelableMap(parcel, CustomDataItem::class.java)
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

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        ParcelableUtil.writeStringParcelableMap(parcel, flags, mCustomDataItems)
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