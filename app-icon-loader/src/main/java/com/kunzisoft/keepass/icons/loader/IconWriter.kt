package com.kunzisoft.keepass.icons.loader

import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

/**
 * Write downloaded icon to cache directory.
 */
class IconWriter(
    private val iconsDir: File,
) {

    init {
        iconsDir.deleteRecursively()
        iconsDir.mkdirs()
    }

    fun write(icon: Icon, bitmap: Bitmap) {
        FileOutputStream(getFile(icon)).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    fun getFile(icon: Icon) = File(iconsDir, "${icon.uuid}.png")
}
