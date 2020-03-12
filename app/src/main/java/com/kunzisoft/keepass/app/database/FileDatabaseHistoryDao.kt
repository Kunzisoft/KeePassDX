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

import androidx.room.*

@Dao
interface FileDatabaseHistoryDao {
    @Query("SELECT * FROM file_database_history ORDER BY updated DESC")
    fun getAll(): List<FileDatabaseHistoryEntity>

    @Query("SELECT * FROM file_database_history WHERE database_uri = :databaseUriString")
    fun getByDatabaseUri(databaseUriString: String): FileDatabaseHistoryEntity?

    @Insert
    fun add(vararg fileDatabaseHistory: FileDatabaseHistoryEntity)

    @Update
    fun update(vararg fileDatabaseHistory: FileDatabaseHistoryEntity)

    @Delete
    fun delete(fileDatabaseHistory: FileDatabaseHistoryEntity): Int

    @Query("UPDATE file_database_history SET keyfile_uri=null WHERE database_uri = :databaseUriString")
    fun deleteKeyFileByDatabaseUri(databaseUriString: String)

    @Query("UPDATE file_database_history SET keyfile_uri=null")
    fun deleteAllKeyFiles()

    @Query("DELETE FROM file_database_history")
    fun deleteAll()
}