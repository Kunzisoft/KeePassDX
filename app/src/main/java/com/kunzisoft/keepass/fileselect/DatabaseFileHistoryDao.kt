package com.kunzisoft.keepass.fileselect

import android.arch.persistence.db.SupportSQLiteQuery
import android.arch.persistence.room.*

@Dao
interface DatabaseFileHistoryDao {
    @Query("SELECT * FROM database_file_history ORDER BY updated DESC")
    fun getAll(): List<DatabaseFileHistoryEntity>

    @Query("SELECT * FROM database_file_history WHERE database_uri = :databaseUriString")
    fun getByDatabaseUri(databaseUriString: String): DatabaseFileHistoryEntity?

    @Insert
    fun add(vararg databaseFileHistory: DatabaseFileHistoryEntity)

    @Update
    fun update(vararg databaseFileHistory: DatabaseFileHistoryEntity)

    @Delete
    fun delete(databaseFileHistory: DatabaseFileHistoryEntity): Int

    /* TODO Replace (Insert not yet supported)
    @Query("REPLACE INTO database_file_history(keyfile_uri) VALUES(null)")
    fun deleteAllKeyFiles()
    */
    @RawQuery
    fun deleteAllKeyFiles(query: SupportSQLiteQuery): Boolean

    @Query("DELETE FROM database_file_history")
    fun deleteAll()
}