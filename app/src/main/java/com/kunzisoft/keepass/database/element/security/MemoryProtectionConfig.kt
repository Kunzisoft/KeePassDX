/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.element.security

class MemoryProtectionConfig {

    var protectTitle = false
    var protectUserName = false
    var protectPassword = false
    var protectUrl = false
    var protectNotes = false

    var autoEnableVisualHiding = false

    fun isProtected(field: String): Boolean {
        if (field.equals(ProtectDefinition.TITLE_FIELD, ignoreCase = true)) return protectTitle
        if (field.equals(ProtectDefinition.USERNAME_FIELD, ignoreCase = true)) return protectUserName
        if (field.equals(ProtectDefinition.PASSWORD_FIELD, ignoreCase = true)) return protectPassword
        if (field.equals(ProtectDefinition.URL_FIELD, ignoreCase = true)) return protectUrl
        return if (field.equals(ProtectDefinition.NOTES_FIELD, ignoreCase = true)) protectNotes else false

    }

    object ProtectDefinition {
        const val TITLE_FIELD = "Title"
        const val USERNAME_FIELD = "UserName"
        const val PASSWORD_FIELD = "Password"
        const val URL_FIELD = "URL"
        const val NOTES_FIELD = "Notes"
    }
}
