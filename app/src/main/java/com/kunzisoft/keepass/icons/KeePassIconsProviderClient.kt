package com.kunzisoft.keepass.icons

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import com.kunzisoft.keepass.database.element.binary.BinaryByte
import com.kunzisoft.keepass.database.element.binary.BinaryCache
import com.kunzisoft.keepass.database.element.binary.BinaryData
import com.kunzisoft.keepass.database.element.icon.IconImageCustom
import com.kunzisoft.keepass.model.IconProviderData
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.util.*

private const val AUTHORITY = "com.kunzisoft.keepass.icons.loader"

class KeePassIconsProviderClient(
    private val contentResolver: ContentResolver,
    val cache: BinaryCache = BinaryCache(),
) {

    fun exists() =
        contentResolver.acquireContentProviderClient(AUTHORITY).let {
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) it?.close() else it?.release()
            it != null
        }

    fun queryIcons(iconProviderData: IconProviderData): List<IconImageCustom> {
        val packageNames = iconProviderData.packageNames
        val hosts = iconProviderData.hosts
        val selectionArgs = packageNames.map { "app:$it" } + hosts.map { "host:$it" }

        return contentResolver.query(
            Uri.parse("content://$AUTHORITY"),
            null,
            null,
            selectionArgs.toSet().toTypedArray(),
            null
        )?.use { cursor ->
            cursor.asSequence().map {
                val uuid = it.getBlob(0).toUUID()
                val name = it.getString(1)
                IconImageCustom(
                    uuid = uuid,
                    name = name,
                )
            }.toList()
        } ?: emptyList()
    }

    fun loadIcon(iconId: UUID): BinaryData? =
        contentResolver.openFileDescriptor(
            Uri.parse("content://$AUTHORITY/$iconId"),
            "r",
        ).use { file ->
            file?.fileDescriptor?.let { fileDescriptor ->
                BinaryByte(iconId.toString()).apply {
                    getOutputDataStream(cache).use { out ->
                        FileInputStream(fileDescriptor).copyTo(out)
                    }
                }
            }
        }

    private fun ByteArray.toUUID(): UUID {
        val buffer = ByteBuffer.wrap(this)
        val firstLong = buffer.long
        val secondLong = buffer.long
        return UUID(firstLong, secondLong)
    }

    private fun Cursor.asSequence(): Sequence<Cursor> =
        generateSequence { takeIf { it.moveToNext() } }
}
