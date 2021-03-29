/*
 * Copyright 2020 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.utils

class UnsignedLong(value: Long) {

    private var unsignedValue: Long = value

    /**
     * Convert an unsigned Long to Kotlin Long
     */
    fun toKotlinLong(): Long {
        return unsignedValue
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UnsignedLong

        if (unsignedValue != other.unsignedValue) return false

        return true
    }

    override fun hashCode(): Int {
        return unsignedValue.hashCode()
    }

    fun plusOne() {
        if (unsignedValue >= 0L)
            unsignedValue++
        else
            unsignedValue--
    }

    companion object {
        private const val MAX_VALUE: Long = -1
        val MAX_BYTES = longTo8Bytes(MAX_VALUE)
    }
}