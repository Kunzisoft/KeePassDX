package com.kunzisoft.keepass.app.database

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
) {
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