/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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

import android.util.SparseArray
import com.kunzisoft.keepass.database.element.security.ProtectedBinary

class BinaryPool {
    private val pool = SparseArray<ProtectedBinary>()

    operator fun get(key: Int): ProtectedBinary? {
        return pool[key]
    }

    fun put(key: Int, value: ProtectedBinary) {
        pool.put(key, value)
    }

    fun doForEachBinary(action: (key: Int, binary: ProtectedBinary) -> Unit) {
        for (i in 0 until pool.size()) {
            action.invoke(i, pool.get(pool.keyAt(i)))
        }
    }

    fun clear() {
        doForEachBinary { _, binary ->
            binary.clear()
        }
        pool.clear()
    }

    fun add(protectedBinary: ProtectedBinary) {
        if (findKey(protectedBinary) == null) {
            pool.put(findUnusedKey(), protectedBinary)
        }
    }

    fun findUnusedKey(): Int {
        var unusedKey = pool.size()
        while (get(unusedKey) != null)
            unusedKey++
        return unusedKey
    }

    fun findKey(pb: ProtectedBinary): Int? {
        for (i in 0 until pool.size()) {
            if (pool.get(pool.keyAt(i)) == pb) return i
        }
        return null
    }
}
