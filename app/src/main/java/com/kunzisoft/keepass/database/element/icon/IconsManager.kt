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
import com.kunzisoft.keepass.database.element.binary.BinaryCache
import com.kunzisoft.keepass.database.element.binary.BinaryData
import com.kunzisoft.keepass.database.element.binary.CustomIconPool
import com.kunzisoft.keepass.database.element.icon.IconImageStandard.Companion.KEY_ID
import com.kunzisoft.keepass.icons.IconPack.Companion.NB_ICONS
import java.util.*

class IconsManager(private val binaryCache: BinaryCache) {

    private val standardCache = List(NB_ICONS) {
        IconImageStandard(it)
    }
    private val customCache = CustomIconPool(binaryCache)

    fun getIcon(iconId: Int): IconImageStandard {
        val searchIconId = if (IconImageStandard.isCorrectIconId(iconId)) iconId else KEY_ID
        return standardCache[searchIconId]
    }

    fun doForEachStandardIcon(action: (IconImageStandard) -> Unit) {
        standardCache.forEach { icon ->
            action.invoke(icon)
        }
    }

    /*
     *  Custom
     */

    fun buildNewCustomIcon(key: UUID? = null,
                           result: (IconImageCustom, BinaryData?) -> Unit) {
        // Create a binary file for a brand new custom icon
        addCustomIcon(key, false, result)
    }

    fun addCustomIcon(key: UUID? = null,
                      smallSize: Boolean,
                      result: (IconImageCustom, BinaryData?) -> Unit) {
        val keyBinary = customCache.put(key) { uniqueBinaryId ->
            // Create a byte array for better performance with small data
            binaryCache.getBinaryData(uniqueBinaryId, smallSize)
        }
        result.invoke(IconImageCustom(keyBinary.keys.first()), keyBinary.binary)
    }

    fun getIcon(iconUuid: UUID): IconImageCustom {
        return IconImageCustom(iconUuid)
    }

    fun isCustomIconBinaryDuplicate(binaryData: BinaryData): Boolean {
        return customCache.isBinaryDuplicate(binaryData)
    }

    fun removeCustomIcon(binaryCache: BinaryCache, iconUuid: UUID) {
        val binary = customCache[iconUuid]
        customCache.remove(iconUuid)
        try {
            binary?.clear(binaryCache)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to remove custom icon binary", e)
        }
    }

    fun getBinaryForCustomIcon(iconUuid: UUID): BinaryData? {
        return customCache[iconUuid]
    }

    fun doForEachCustomIcon(action: (IconImageCustom, BinaryData) -> Unit) {
        customCache.doForEachBinary { key, binary ->
            action.invoke(IconImageCustom(key), binary)
        }
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
