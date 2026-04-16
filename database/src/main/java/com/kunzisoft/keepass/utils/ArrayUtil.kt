/*
 * Copyright 2026 Jeremy Jamet / Kunzisoft.
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
 */

package com.kunzisoft.keepass.utils

const val charNull = '\u0000'

/**
 * Extension function to clear a CharArray by filling it with null characters.
 */
fun CharArray.clear() {
    this.fill(charNull)
}

/**
 * Extension function to clear a StringBuilder by filling it with null characters.
 */
fun StringBuilder.clear() {
    for (i in indices) {
        setCharAt(i, charNull)
    }
}

/**
 * Extension function to clear a ByteArray by filling it with zeros.
 */
fun ByteArray.clear() {
    this.fill(0)
}
