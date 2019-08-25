package com.kunzisoft.keepass.fileselect

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context

@Database(entities = [DatabaseFileHistoryEntity::class], version = 1)
abstract class DatabaseFileHistoryDatabase : RoomDatabase() {
    abstract fun databaseFileHistoryDao(): DatabaseFileHistoryDao

    companion object {
        fun getDatabase(applicationContext: Context): DatabaseFileHistoryDatabase {
            return Room.databaseBuilder(
                    applicationContext,
                    DatabaseFileHistoryDatabase::class.java, "database-name"
            ).build()
        }
    }
}