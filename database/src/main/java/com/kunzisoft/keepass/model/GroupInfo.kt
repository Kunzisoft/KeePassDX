package com.kunzisoft.keepass.model

import android.os.Parcel
import android.os.ParcelUuid
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.Tags
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import com.kunzisoft.keepass.database.element.icon.IconImageStandard.Companion.FOLDER_ID
import com.kunzisoft.keepass.utils.readParcelableCompat
import java.util.*

class GroupInfo : NodeInfo {

    var id: UUID? = null
    var notes: String? = null
    var searchable: Boolean? = null
    var enableAutoType: Boolean? = null
    var defaultAutoTypeSequence: String = ""
    var tags: Tags = Tags()

    init {
        icon.standard = IconImageStandard(FOLDER_ID)
    }

    constructor(): super()

    constructor(parcel: Parcel): super(parcel) {
        id = parcel.readParcelableCompat<ParcelUuid>()?.uuid ?: id
        notes = parcel.readString()
        val isSearchingEnabled = parcel.readInt()
        searchable = if (isSearchingEnabled == -1) null else isSearchingEnabled == 1
        val isAutoTypeEnabled = parcel.readInt()
        enableAutoType = if (isAutoTypeEnabled == -1) null else isAutoTypeEnabled == 1
        defaultAutoTypeSequence = parcel.readString() ?: defaultAutoTypeSequence
        tags = parcel.readParcelableCompat() ?: tags
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        val uuid = if (id != null) ParcelUuid(id) else null
        parcel.writeParcelable(uuid, flags)
        parcel.writeString(notes)
        parcel.writeInt(if (searchable == null) -1 else if (searchable!!) 1 else 0)
        parcel.writeInt(if (enableAutoType == null) -1 else if (enableAutoType!!) 1 else 0)
        parcel.writeString(defaultAutoTypeSequence)
        parcel.writeParcelable(tags, flags)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupInfo) return false
        if (!super.equals(other)) return false

        if (id != other.id) return false
        if (notes != other.notes) return false
        if (searchable != other.searchable) return false
        if (enableAutoType != other.enableAutoType) return false
        if (defaultAutoTypeSequence != other.defaultAutoTypeSequence) return false
        if (tags != other.tags) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + (notes?.hashCode() ?: 0)
        result = 31 * result + searchable.hashCode()
        result = 31 * result + enableAutoType.hashCode()
        result = 31 * result + defaultAutoTypeSequence.hashCode()
        result = 31 * result + tags.hashCode()
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