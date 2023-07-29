/*
 * Copyright 2021 Jeremy Jamet / Kunzisoft.
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

import android.content.Context
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.text.format.Formatter
import com.kunzisoft.keepass.utils.readParcelableCompat
import com.kunzisoft.keepass.viewmodels.FileDatabaseInfo
import java.text.DateFormat
import java.util.Date

/**
 * Utility data class to get FileDatabaseInfo at a `t` time
 */
data class SnapFileDatabaseInfo(var fileUri: Uri?,
                                var exists: Boolean,
                                var lastModification: Long?,
                                var size: Long?): Parcelable {

    constructor(parcel: Parcel) : this(
            parcel.readParcelableCompat(),
            parcel.readByte() != 0.toByte(),
            parcel.readValue(Long::class.java.classLoader) as? Long,
            parcel.readValue(Long::class.java.classLoader) as? Long) {
    }

    fun toString(context: Context): String {
        val lastModificationString = DateFormat.getDateTimeInstance()
                .format(Date(lastModification ?: 0))
        return "$lastModificationString, " +
                Formatter.formatFileSize(context, size ?: 0)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(fileUri, flags)
        parcel.writeByte(if (exists) 1 else 0)
        parcel.writeValue(lastModification)
        parcel.writeValue(size)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SnapFileDatabaseInfo) return false

        if (fileUri != other.fileUri) return false
        if (exists != other.exists) return false
        if (lastModification != other.lastModification) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileUri?.hashCode() ?: 0
        result = 31 * result + exists.hashCode()
        result = 31 * result + (lastModification?.hashCode() ?: 0)
        result = 31 * result + (size?.hashCode() ?: 0)
        return result
    }

    companion object CREATOR : Parcelable.Creator<SnapFileDatabaseInfo> {
        override fun createFromParcel(parcel: Parcel): SnapFileDatabaseInfo {
            return SnapFileDatabaseInfo(parcel)
        }

        override fun newArray(size: Int): Array<SnapFileDatabaseInfo?> {
            return arrayOfNulls(size)
        }

        fun fromFileDatabaseInfo(fileDatabaseInfo: FileDatabaseInfo): SnapFileDatabaseInfo {
            return SnapFileDatabaseInfo(
                fileDatabaseInfo.fileUri,
                fileDatabaseInfo.exists,
                fileDatabaseInfo.getLastModification(),
                fileDatabaseInfo.getSize())
        }
    }
}
