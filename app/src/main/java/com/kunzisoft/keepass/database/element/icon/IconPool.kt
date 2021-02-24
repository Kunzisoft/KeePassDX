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

import java.util.UUID

class IconPool {

    private val standardCache = HashMap<Int, IconImageStandard?>()
    private val customCache = HashMap<UUID, IconImageCustom?>()

    fun getIcon(iconId: Int): IconImageStandard {
        var icon: IconImageStandard? = standardCache[iconId]

        if (icon == null) {
            icon = IconImageStandard(iconId)
            standardCache[iconId] = icon
        }

        return icon
    }

    fun getIcon(iconUuid: UUID): IconImageCustom {
        var icon: IconImageCustom? = customCache[iconUuid]

        if (icon == null) {
            icon = IconImageCustom(iconUuid)
            customCache[iconUuid] = icon
        }

        return icon
    }

    fun putIcon(icon: IconImageCustom) {
        customCache[icon.uuid] = icon
    }

    fun containsCustomIcons(): Boolean {
        return customCache.isNotEmpty()
    }

    fun doForEachCustomIcon(action: (customIcon: IconImageCustom) -> Unit) {
        for ((_, customIcon) in customCache) {
            action.invoke(customIcon!!)
        }
    }

    /**
     * Clear the cache of icons
     */
    fun clearCache() {
        standardCache.clear()
        customCache.clear()
    }
}
