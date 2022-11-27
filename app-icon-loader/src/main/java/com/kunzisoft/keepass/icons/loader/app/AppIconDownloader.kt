package com.kunzisoft.keepass.icons.loader.app

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import com.kunzisoft.keepass.icons.loader.*
import java.util.*

/**
 * Download app icon from [PackageManager].
 */
class AppIconDownloader(
    private val pm: PackageManager,
    private val db: IconDao,
    private val writer: IconWriter,
) : IconsDownloader<String>, IconDownloader<PackageInfo> {

    private val source = IconSource.App

    override fun download(items: Set<String>): List<Icon> {
        val existingIcons = db.getSourceKeys(source)
        return pm.getInstalledPackages(0)
            .filter { items.contains(it.packageName) }
            .filterNot { existingIcons.contains(it.packageName) }
            .map { packageInfo -> download(packageInfo) }
    }

    override fun download(item: PackageInfo): Icon =
        Icon(
            uuid = UUID.randomUUID(),
            name = item.applicationInfo?.loadLabel(pm)?.toString() ?: item.packageName,
            sourceKey = item.packageName,
            source = source,
        ).also { icon ->
            val appIcon = pm.getApplicationIcon(item.packageName)
            writer.write(icon, appIcon.toBitmap(config = Bitmap.Config.ARGB_8888))
        }
}
