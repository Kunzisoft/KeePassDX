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
package com.kunzisoft.keepass.database.element.binary

class AttachmentPool : BinaryPool<Int>() {

    /**
     * Utility method to find an unused key in the pool
     */
    override fun findUnusedKey(): Int {
        var unusedKey = 0
        while (pool[unusedKey] != null)
            unusedKey++
        return unusedKey
    }

    /**
     * To register a binary with a ref corresponding to an ordered index
     */
    fun getBinaryIndexFromKey(key: Int): Int? {
        val index = orderedBinariesWithoutDuplication().indexOfFirst { it.keys.contains(key) }
        return if (index < 0)
            null
        else
            index
    }
}
