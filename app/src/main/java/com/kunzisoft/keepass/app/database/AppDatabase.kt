package com.kunzisoft.keepass.app.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(version = 1, entities = [FileDatabaseHistoryEntity::class, CipherDatabaseEntity::class])
abstract class AppDatabase : RoomDatabase() {

    abstract fun fileDatabaseHistoryDao(): FileDatabaseHistoryDao
    abstract fun cipherDatabaseDao(): CipherDatabaseDao

    companion object {
        fun getDatabase(applicationContext: Context): AppDatabase {
            return Room.databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java, "com.kunzisoft.keepass.database"
            ).build()
        }
    }
}