package com.kunzisoft.keepass.app.database

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "file_database_history")
data class FileDatabaseHistoryEntity(
        @PrimaryKey
        @ColumnInfo(name = "database_uri")
            val databaseUri: String,

        @ColumnInfo(name = "database_alias")
            val databaseAlias: String,

        @ColumnInfo(name = "keyfile_uri")
            var keyFileUri: String?,

        @ColumnInfo(name = "updated")
            val updated: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileDatabaseHistoryEntity

        if (databaseUri != other.databaseUri) return false

        return true
    }

    override fun hashCode(): Int {
        return databaseUri.hashCode()
    }
}