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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "file_database_history")
data class FileDatabaseHistoryEntity(
        @PrimaryKey
        @ColumnInfo(name = "database_uri")
            val databaseUri: String,

        @ColumnInfo(name = "database_alias")
            var databaseAlias: String,

        @ColumnInfo(name = "keyfile_uri")
            var keyFileUri: String?,

        @ColumnInfo(name = "hardware_key")
            var hardwareKey: String?,

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