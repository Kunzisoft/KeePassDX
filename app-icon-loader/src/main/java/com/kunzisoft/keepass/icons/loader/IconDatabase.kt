package com.kunzisoft.keepass.icons.loader

import android.database.Cursor
import androidx.room.*
import java.util.*

@Database(version = 1, entities = [Icon::class])
abstract class IconDatabase : RoomDatabase() {
    abstract fun icons(): IconDao
}

@Dao
interface IconDao {

    @Insert
    fun insert(icons: List<Icon>)

    @Query("SELECT sourceKey FROM icon WHERE source = :source")
    fun getSourceKeys(source: IconSource): List<String>

    @Query("SELECT * FROM icon WHERE uuid = :uuid")
    fun get(uuid: UUID): Icon?

    @Query("SELECT uuid, name, source FROM icon WHERE sourceKey IN (:packageNames) OR sourceKey IN (:hosts)")
    fun search(packageNames: Set<String>, hosts: Set<String>): Cursor
}

@Entity(
    indices = [
        Index(value = ["sourceKey", "source"], unique = true),
    ]
)
data class Icon(
    @PrimaryKey
    val uuid: UUID,
    val name: String,
    val sourceKey: String,
    val source: IconSource,
)

enum class IconSource {
    App, DuckDuckGo, Google
}
