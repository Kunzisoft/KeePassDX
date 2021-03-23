/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.app

import androidx.multidex.MultiDexApplication
import com.kunzisoft.keepass.activities.stylish.Stylish
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.utils.UriUtil
import java.util.*

class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

        Stylish.init(this)
        PRNGFixes.apply()
    }

    override fun onTerminate() {
        Database.getInstance().clearAndClose(UriUtil.getBinaryDir(this))
        super.onTerminate()
    }

    companion object {

        // Similar to file storage but much faster
        private val byteArrayList = HashMap<Int, ByteArray>()

        fun getByteArray(key: Int?): KeyByteArray {
            if (key == null) {
                val newItem = KeyByteArray(byteArrayList.size, ByteArray(0))
                byteArrayList[newItem.key] = newItem.data
                return newItem
            }
            if (!byteArrayList.containsKey(key)) {
                val newItem = KeyByteArray(key, ByteArray(0))
                byteArrayList[newItem.key] = newItem.data
                return newItem
            }
            return KeyByteArray(key, byteArrayList[key]!!)
        }

        fun setByteArray(key: Int?, data: ByteArray): KeyByteArray {
            return if (key == null) {
                val keyByteArray = KeyByteArray(byteArrayList.size, data)
                byteArrayList[keyByteArray.key] = keyByteArray.data
                keyByteArray
            } else {
                byteArrayList[key] = data
                KeyByteArray(key, data)
            }
        }

        fun removeByteArray(key: Int?) {
            key?.let {
                byteArrayList.remove(it)
            }
        }
    }

    data class KeyByteArray(val key: Int, val data: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is KeyByteArray) return false

            if (key != other.key) return false

            return true
        }

        override fun hashCode(): Int {
            return key
        }
    }
}
