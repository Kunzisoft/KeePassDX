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
 *
 */
package com.kunzisoft.keepass.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.utils.readParcelableCompat
import com.kunzisoft.keepass.utils.readEnum
import com.kunzisoft.keepass.utils.writeEnum

class CipherEncryptDatabase(): Parcelable {

    var databaseUri: Uri? = null
    var credentialStorage: CredentialStorage = CredentialStorage.DEFAULT
    var encryptedValue: ByteArray = byteArrayOf()
    var specParameters: ByteArray = byteArrayOf()

    constructor(parcel: Parcel): this() {
        databaseUri = parcel.readParcelableCompat()
        credentialStorage = parcel.readEnum<CredentialStorage>() ?: credentialStorage
        encryptedValue = ByteArray(parcel.readInt())
        parcel.readByteArray(encryptedValue)
        specParameters = ByteArray(parcel.readInt())
        parcel.readByteArray(specParameters)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(databaseUri, flags)
        parcel.writeEnum(credentialStorage)
        parcel.writeInt(encryptedValue.size)
        parcel.writeByteArray(encryptedValue)
        parcel.writeInt(specParameters.size)
        parcel.writeByteArray(specParameters)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CipherEncryptDatabase> {
        override fun createFromParcel(parcel: Parcel): CipherEncryptDatabase {
            return CipherEncryptDatabase(parcel)
        }

        override fun newArray(size: Int): Array<CipherEncryptDatabase?> {
            return arrayOfNulls(size)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CipherEncryptDatabase

        if (databaseUri != other.databaseUri) return false

        return true
    }

    override fun hashCode(): Int {
        return databaseUri.hashCode()
    }
}
