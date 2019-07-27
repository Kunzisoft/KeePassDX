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
package com.kunzisoft.keepass.database.exception

import android.net.Uri
import java.io.FileNotFoundException

class ContentFileNotFoundException : FileNotFoundException() {
    companion object {
        fun getInstance(uri: Uri?): FileNotFoundException {
            if (uri == null) {
                return FileNotFoundException()
            }

            val scheme = uri.scheme
            return if (scheme != null
                    && scheme.isNotEmpty()
                    && scheme.equals("content", ignoreCase = true)) {
                ContentFileNotFoundException()
            } else FileNotFoundException()
        }
    }
}
