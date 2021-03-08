/*
 * Copyright 2021 Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.database.element.icon

import android.util.Log
import com.kunzisoft.keepass.database.element.database.BinaryFile
import com.kunzisoft.keepass.database.element.database.CustomIconPool
import com.kunzisoft.keepass.icons.IconPack.Companion.NB_ICONS
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class IconsManager {

    private val standardCache = List(NB_ICONS) {
        IconImageStandard(it)
    }
    private val customCache = CustomIconPool()

    fun getIcon(iconId: Int): IconImageStandard {
        return standardCache[iconId]
    }

    fun getStandardIconList(): List<IconImageStandard> {
        return standardCache
    }

    /*
     *  Custom
     */

    fun buildNewCustomIcon(cacheDirectory: File, key: UUID? = null): IconImageCustom {
        val keyBinary = customCache.put(cacheDirectory, key)
        return IconImageCustom(keyBinary.keys.first(), keyBinary.binary)
    }

    fun getIcon(iconUuid: UUID): IconImageCustom {
        customCache[iconUuid]?.let {
            return IconImageCustom(iconUuid, it)
        }
        return IconImageCustom(iconUuid)
    }

    fun getIconsWithBinary(binaryFile: BinaryFile): List<IconImageCustom>{
        val searchBinaryMD5 = binaryFile.md5()
        val listIcons = ArrayList<IconImageCustom>()
        customCache.doForEachBinary { key, binary ->
            if (binary.md5() == searchBinaryMD5) {
                listIcons.add(IconImageCustom(key, binary))
            }
        }
        return listIcons
    }

    fun containsAnyCustomIcon(): Boolean {
        return !customCache.isEmpty()
    }

    fun removeCustomIcon(iconUuid: UUID) {
        val binary = customCache[iconUuid]
        customCache.remove(iconUuid)
        try {
            binary?.clear()
        } catch (e: Exception) {
            Log.w(TAG, "Unable to remove custom icon binary", e)
        }
    }

    fun getCustomIconList(): List<IconImageCustom> {
        val list = ArrayList<IconImageCustom>()
        customCache.doForEachBinary { key, binary ->
            list.add(IconImageCustom(key, binary))
        }
        return list
    }

    /**
     * Clear the cache of icons
     */
    fun clearCache() {
        try {
            customCache.clear()
        } catch(e: Exception) {
            Log.e(TAG, "Unable to clear cache", e)
        }
    }

    companion object {
        private val TAG = IconsManager::class.java.name
    }
}
