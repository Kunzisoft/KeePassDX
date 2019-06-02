/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
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

import com.kunzisoft.keepass.database.element.security.ProtectedBinary
import java.util.HashMap
import kotlin.collections.Map.Entry

class BinaryPool {
    // TODO SparseArray
    private val pool = HashMap<Int, ProtectedBinary>()

    operator fun get(key: Int): ProtectedBinary? {
        return pool[key]
    }

    fun put(key: Int, value: ProtectedBinary) {
        pool[key] = value
    }

    fun entrySet(): Set<Entry<Int, ProtectedBinary>> {
        return pool.entries
    }

    fun clear() {
        for ((_, value) in pool)
            value.clear()
        pool.clear()
    }

    fun binaries(): Collection<ProtectedBinary> {
        return pool.values
    }

    fun add(protectedBinary: ProtectedBinary) {
        if (findKey(protectedBinary) != -1) return
        pool[findUnusedKey()] = protectedBinary
    }

    fun findUnusedKey(): Int {
        var unusedKey = pool.size
        while (get(unusedKey) != null)
            unusedKey++
        return unusedKey
    }

    fun findKey(pb: ProtectedBinary): Int {
        for ((key, value) in pool) {
            if (value == pb) return key
        }
        return -1
    }
}
