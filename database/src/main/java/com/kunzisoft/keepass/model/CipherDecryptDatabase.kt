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

class CipherDecryptDatabase(): Parcelable {

    var databaseUri: Uri? = null
    var credentialStorage: CredentialStorage = CredentialStorage.DEFAULT
    var decryptedValue: ByteArray = byteArrayOf()

    constructor(parcel: Parcel): this() {
        databaseUri = parcel.readParcelableCompat()
        credentialStorage = parcel.readEnum<CredentialStorage>() ?: credentialStorage
        decryptedValue = ByteArray(parcel.readInt())
        parcel.readByteArray(decryptedValue)
    }

    fun replaceContent(copy: CipherDecryptDatabase) {
        this.decryptedValue = copy.decryptedValue
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(databaseUri, flags)
        parcel.writeEnum(credentialStorage)
        parcel.writeInt(decryptedValue.size)
        parcel.writeByteArray(decryptedValue)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CipherDecryptDatabase> {
        override fun createFromParcel(parcel: Parcel): CipherDecryptDatabase {
            return CipherDecryptDatabase(parcel)
        }

        override fun newArray(size: Int): Array<CipherDecryptDatabase?> {
            return arrayOfNulls(size)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CipherDecryptDatabase

        if (databaseUri != other.databaseUri) return false

        return true
    }

    override fun hashCode(): Int {
        return databaseUri.hashCode()
    }
}
