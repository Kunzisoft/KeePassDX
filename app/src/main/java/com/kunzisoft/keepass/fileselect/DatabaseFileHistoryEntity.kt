package com.kunzisoft.keepass.fileselect

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "database_file_history")
data class DatabaseFileHistoryEntity(
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

        other as DatabaseFileHistoryEntity

        if (databaseUri != other.databaseUri) return false

        return true
    }

    override fun hashCode(): Int {
        return databaseUri.hashCode()
    }
}