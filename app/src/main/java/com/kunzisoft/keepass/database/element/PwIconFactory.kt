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
package com.kunzisoft.keepass.database.element

import org.apache.commons.collections.map.AbstractReferenceMap
import org.apache.commons.collections.map.ReferenceMap

import java.util.UUID

class PwIconFactory {
    /** customIconMap
     * Cache for icon drawable.
     * Keys: Integer, Values: PwIconStandard
     */
    private val cache = ReferenceMap(AbstractReferenceMap.HARD, AbstractReferenceMap.WEAK)

    /** standardIconMap
     * Cache for icon drawable.
     * Keys: UUID, Values: PwIconCustom
     */
    private val customCache = ReferenceMap(AbstractReferenceMap.HARD, AbstractReferenceMap.WEAK)

    val unknownIcon: PwIconStandard
        get() = getIcon(PwIcon.UNKNOWN_ID)

    val keyIcon: PwIconStandard
        get() = getIcon(PwIconStandard.KEY)

    val trashIcon: PwIconStandard
        get() = getIcon(PwIconStandard.TRASH)

    val folderIcon: PwIconStandard
        get() = getIcon(PwIconStandard.FOLDER)

    fun getIcon(iconId: Int): PwIconStandard {
        var icon: PwIconStandard? = cache[iconId] as PwIconStandard?

        if (icon == null) {
            icon = PwIconStandard(iconId)
            cache[iconId] = icon
        }

        return icon
    }

    fun getIcon(iconUuid: UUID): PwIconCustom {
        var icon: PwIconCustom? = customCache[iconUuid] as PwIconCustom?

        if (icon == null) {
            icon = PwIconCustom(iconUuid)
            customCache[iconUuid] = icon
        }

        return icon
    }

    fun put(icon: PwIconCustom) {
        customCache[icon.uuid] = icon
    }
}
