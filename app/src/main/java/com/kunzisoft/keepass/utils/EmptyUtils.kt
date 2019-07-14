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
package com.kunzisoft.keepass.utils

import android.net.Uri

import com.kunzisoft.keepass.database.element.PwDate

object EmptyUtils {
    fun isNullOrEmpty(str: String?): Boolean {
        return str == null || str.isEmpty()
    }

    fun isNullOrEmpty(buf: ByteArray?): Boolean {
        return buf == null || buf.isEmpty()
    }

    fun isNullOrEmpty(date: PwDate?): Boolean {
        return date == null || date == PwDate.DEFAULT_PWDATE
    }

    fun isNullOrEmpty(uri: Uri?): Boolean {
        return uri == null || uri.toString().isEmpty()
    }
}
