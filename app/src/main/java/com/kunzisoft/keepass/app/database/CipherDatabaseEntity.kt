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