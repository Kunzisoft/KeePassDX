package com.kunzisoft.keepass.icons.loader

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.room.Room
import com.kunzisoft.keepass.icons.loader.app.AppIconDownloader
import com.kunzisoft.keepass.icons.loader.web.DuckDuckGoIconDownloader
import com.kunzisoft.keepass.icons.loader.web.GoogleWebIconDownloader
import java.io.File
import java.util.*

/**
 * Request icons for KeePassDX via this [ContentProvider].
 */
class KeePassIconsProvider : ContentProvider() {

    private val pm by lazy {
        requireContext(this).packageManager
    }

    private val db by lazy {
        Room.inMemoryDatabaseBuilder(requireContext(this), IconDatabase::class.java).build()
    }

    private val icons by lazy {
        db.icons()
    }

    private val appIconDownloader by lazy {
        AppIconDownloader(pm, icons, writer)
    }

    private val duckDuckGoIconDownloader by lazy {
        DuckDuckGoIconDownloader(icons, writer)
    }

    private val googleWebIconDownloader by lazy {
        GoogleWebIconDownloader(icons, writer)
    }

    private val writer by lazy {
        IconWriter(iconsDir = File(requireContext(this).cacheDir, "/icons/"))
    }

    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String? = null

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): Cursor {
        val args = selectionArgs?.asSequence().orEmpty()
        val packageNames = args.filter(prefix = "app:").toSet()
        val hosts = args.filter(prefix = "host:").toSet()

        // Download all icons
        val appIcons = appIconDownloader.download(packageNames)
        val duckDuckGoIcons = duckDuckGoIconDownloader.download(hosts)
        val googleUrlIcons = googleWebIconDownloader.download(hosts)

        // Update icon database
        icons.insert(icons = appIcons + duckDuckGoIcons + googleUrlIcons)

        // Query database
        return icons.search(
            packageNames = packageNames,
            hosts = hosts,
        )
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?,
    ): Int = 0

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? =
        icons.get(
            uuid = UUID.fromString(uri.pathSegments.last())
        )?.let { icon ->
            ParcelFileDescriptor.open(writer.getFile(icon), ParcelFileDescriptor.MODE_READ_ONLY)
        }

    private fun Sequence<String>.filter(prefix: String) = this
        .filter { it.startsWith(prefix) }
        .map { it.substring(prefix.length) }
        .filter(String::isNotBlank)
}
