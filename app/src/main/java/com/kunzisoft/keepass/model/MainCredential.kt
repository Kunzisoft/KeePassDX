/*
 * Copyright 2022 Jeremy Jamet / Kunzisoft.
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
 */
package com.kunzisoft.keepass.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

data class MainCredential(var masterPassword: String? = null,
                          var keyFileUri: Uri? = null,
                          var hardwareKeyData: ByteArray? = null): Parcelable {

    constructor(parcel: Parcel) : this() {
        masterPassword = parcel.readString()
        keyFileUri = parcel.readParcelable(Uri::class.java.classLoader)
        val hardwareKeyDataLength = parcel.readInt()
        if (hardwareKeyDataLength >= 0) {
            hardwareKeyData = ByteArray(hardwareKeyDataLength)
            parcel.readByteArray(hardwareKeyData!!)
        } else {
            hardwareKeyData = null
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(masterPassword)
        parcel.writeParcelable(keyFileUri, flags)
        if (hardwareKeyData != null) {
            parcel.writeInt(hardwareKeyData!!.size)
            parcel.writeByteArray(hardwareKeyData)
        } else {
            parcel.writeInt(-1)
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MainCredential

        if (masterPassword != other.masterPassword) return false
        if (keyFileUri != other.keyFileUri) return false
        if (hardwareKeyData != null) {
            if (other.hardwareKeyData == null) return false
            if (!hardwareKeyData.contentEquals(other.hardwareKeyData)) return false
        } else if (other.hardwareKeyData != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = masterPassword?.hashCode() ?: 0
        result = 31 * result + (keyFileUri?.hashCode() ?: 0)
        result = 31 * result + (hardwareKeyData?.contentHashCode() ?: 0)
        return result
    }

    companion object CREATOR : Parcelable.Creator<MainCredential> {
        override fun createFromParcel(parcel: Parcel): MainCredential {
            return MainCredential(parcel)
        }

        override fun newArray(size: Int): Array<MainCredential?> {
            return arrayOfNulls(size)
        }
    }
}
