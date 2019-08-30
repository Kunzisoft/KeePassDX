package com.kunzisoft.keepass.app.database

import androidx.sqlite.db.SupportSQLiteQuery
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

    /* TODO Replace (Insert not yet supported)
    @Query("REPLACE INTO file_database_history(keyfile_uri) VALUES(null)")
    fun deleteAllKeyFiles()
    */
    @RawQuery
    fun deleteAllKeyFiles(query: SupportSQLiteQuery): Boolean

    @Query("DELETE FROM file_database_history")
    fun deleteAll()
}