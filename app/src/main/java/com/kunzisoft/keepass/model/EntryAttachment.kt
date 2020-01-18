package com.kunzisoft.keepass.model

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.security.BinaryAttachment
import com.kunzisoft.keepass.utils.readEnum
import com.kunzisoft.keepass.utils.writeEnum

data class EntryAttachment(var name: String,
                           var binaryAttachment: BinaryAttachment,
                           var downloadState: AttachmentState = AttachmentState.NULL,
                           var downloadProgression: Int = 0) : Parcelable {

    constructor(parcel: Parcel) : this(
            parcel.readString() ?: "",
            parcel.readParcelable(BinaryAttachment::class.java.classLoader) ?: BinaryAttachment(),
            parcel.readEnum<AttachmentState>() ?: AttachmentState.NULL,
            parcel.readInt())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeParcelable(binaryAttachment, flags)
        parcel.writeEnum(downloadState)
        parcel.writeInt(downloadProgression)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<EntryAttachment> {
        override fun createFromParcel(parcel: Parcel): EntryAttachment {
            return EntryAttachment(parcel)
        }

        override fun newArray(size: Int): Array<EntryAttachment?> {
            return arrayOfNulls(size)
        }
    }
}

enum class AttachmentState {
    NULL, START, IN_PROGRESS, COMPLETE, ERROR
}