package com.kunzisoft.keepass.app.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context

@Database(entities = [FileDatabaseHistoryEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun databaseFileHistoryDao(): FileDatabaseHistoryDao

    companion object {
        fun getDatabase(applicationContext: Context): AppDatabase {
            return Room.databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java, "com.kunzisoft.keepass.database"
            ).build()
        }
    }
}