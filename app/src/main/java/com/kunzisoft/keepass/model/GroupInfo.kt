package com.kunzisoft.keepass.model

import android.os.Parcel
import android.os.ParcelUuid
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import com.kunzisoft.keepass.database.element.icon.IconImageStandard.Companion.FOLDER_ID
import java.util.*

class GroupInfo : NodeInfo {

    var id: UUID? = null
    var notes: String? = null

    init {
        icon.standard = IconImageStandard(FOLDER_ID)
    }

    constructor(): super()

    constructor(parcel: Parcel): super(parcel) {
        id = parcel.readParcelable<ParcelUuid>(ParcelUuid::class.java.classLoader)?.uuid ?: id
        notes = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        val uuid = if (id != null) ParcelUuid(id) else null
        parcel.writeParcelable(uuid, flags)
        parcel.writeString(notes)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupInfo) return false
        if (!super.equals(other)) return false

        if (id != other.id) return false
        if (notes != other.notes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + (notes?.hashCode() ?: 0)
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