/*
 * Copyright 2019 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.database.element.icon

import org.apache.commons.collections.map.AbstractReferenceMap
import org.apache.commons.collections.map.ReferenceMap

import java.util.UUID

class IconImageFactory {
    /** customIconMap
     * Cache for icon drawable.
     * Keys: Integer, Values: IconImageStandard
     */
    private val cache = ReferenceMap(AbstractReferenceMap.HARD, AbstractReferenceMap.WEAK)

    /** standardIconMap
     * Cache for icon drawable.
     * Keys: UUID, Values: IconImageCustom
     */
    private val customCache = ReferenceMap(AbstractReferenceMap.HARD, AbstractReferenceMap.WEAK)

    val unknownIcon: IconImageStandard
        get() = getIcon(IconImage.UNKNOWN_ID)

    val keyIcon: IconImageStandard
        get() = getIcon(IconImageStandard.KEY)

    val trashIcon: IconImageStandard
        get() = getIcon(IconImageStandard.TRASH)

    val folderIcon: IconImageStandard
        get() = getIcon(IconImageStandard.FOLDER)

    fun getIcon(iconId: Int): IconImageStandard {
        var icon: IconImageStandard? = cache[iconId] as IconImageStandard?

        if (icon == null) {
            icon = IconImageStandard(iconId)
            cache[iconId] = icon
        }

        return icon
    }

    fun getIcon(iconUuid: UUID): IconImageCustom {
        var icon: IconImageCustom? = customCache[iconUuid] as IconImageCustom?

        if (icon == null) {
            icon = IconImageCustom(iconUuid)
            customCache[iconUuid] = icon
        }

        return icon
    }

    fun put(icon: IconImageCustom) {
        customCache[icon.uuid] = icon
    }
}
