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
package com.kunzisoft.keepass.crypto

object NativeLib {
    private var isLoaded = false
    private var loadSuccess = false

    fun loaded(): Boolean {
        return init()
    }

    fun init(): Boolean {
        if (!isLoaded) {
            try {
                System.loadLibrary("final-key")
                System.loadLibrary("argon2")
            } catch (e: UnsatisfiedLinkError) {
                return false
            }

            isLoaded = true
            loadSuccess = true
        }

        return loadSuccess
    }

}
