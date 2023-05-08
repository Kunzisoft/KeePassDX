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
package com.kunzisoft.keepass.app.database

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cipher_database")
data class CipherDatabaseEntity(
        @PrimaryKey
        @ColumnInfo(name = "database_uri")
        val databaseUri: String,

        @ColumnInfo(name = "encrypted_value")
        var encryptedValue: String,

        @ColumnInfo(name = "specs_parameters")
        var specParameters: String
): Parcelable {

    constructor(parcel: Parcel) : this(
            parcel.readString()!!,
            parcel.readString()!!,
            parcel.readString()!!)

    fun replaceContent(copy: CipherDatabaseEntity) {
        this.encryptedValue = copy.encryptedValue
        this.specParameters = copy.specParameters
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(databaseUri)
        parcel.writeString(encryptedValue)
        parcel.writeString(specParameters)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CipherDatabaseEntity> {
        override fun createFromParcel(parcel: Parcel): CipherDatabaseEntity {
            return CipherDatabaseEntity(parcel)
        }

        override fun newArray(size: Int): Array<CipherDatabaseEntity?> {
            return arrayOfNulls(size)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CipherDatabaseEntity

        if (databaseUri != other.databaseUri) return false

        return true
    }

    override fun hashCode(): Int {
        return databaseUri.hashCode()
    }
}