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

import com.kunzisoft.keepass.database.element.database.BinaryPool
import com.kunzisoft.keepass.icons.IconPack.Companion.NB_ICONS
import java.io.File
import java.util.*

class IconsManager {

    private val standardCache = List(NB_ICONS) {
        IconImageStandard(it)
    }
    private val customCache = CustomIconPool()

    fun getIcon(iconId: Int): IconImageStandard {
        return standardCache[iconId]
    }

    fun getStandardIconList(): List<IconImage> {
        return standardCache.mapIndexed { _, iconImageStandard -> IconImage(iconImageStandard) }
    }

    /*
     *  Custom
     */

    fun buildNewCustomIcon(cacheDirectory: File, key: UUID? = null): IconImageCustom {
        val keyBinary = customCache.put(cacheDirectory, key)
        return IconImageCustom(keyBinary.keys.first(), keyBinary.binary)
    }

    fun putIcon(icon: IconImageCustom) {
        customCache.put(icon.uuid, icon.binaryFile)
    }

    fun getIcon(iconUuid: UUID): IconImageCustom {
        customCache[iconUuid]?.let {
            return IconImageCustom(iconUuid, it)
        }
        return IconImageCustom(iconUuid)
    }

    fun containsCustomIcons(): Boolean {
        return !customCache.isEmpty()
    }

    fun getCustomIconList(): List<IconImage> {
        val list = ArrayList<IconImage>()
        customCache.doForEachBinary { key, binary ->
            if (binary.length > 0) {
                list.add(IconImage(IconImageCustom(key, binary)))
            }
        }
        return list
    }

    class CustomIconPool : BinaryPool<UUID>() {
        override fun findUnusedKey(): UUID {
            var newUUID = UUID.randomUUID()
            while (pool.containsKey(newUUID)) {
                newUUID = UUID.randomUUID()
            }
            return newUUID
        }
    }

    /**
     * Clear the cache of icons
     */
    fun clearCache() {
        customCache.clear()
    }
}
