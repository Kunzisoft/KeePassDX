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