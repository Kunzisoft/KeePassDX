/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.element.security

class MemoryProtectionConfig {

    var protectTitle = DEFAULT_PROTECT_TITLE
    var protectUserName = DEFAULT_PROTECT_USERNAME
    var protectPassword = DEFAULT_PROTECT_PASSWORD
    var protectUrl = DEFAULT_PROTECT_URL
    var protectNotes = DEFAULT_PROTECT_NOTES

    var autoEnableVisualHiding = DEFAULT_AUTO_ENABLE_VISUAL_HIDING

    fun isProtected(field: String): Boolean {
        if (field.equals(TITLE_FIELD, ignoreCase = true)) return protectTitle
        if (field.equals(USERNAME_FIELD, ignoreCase = true)) return protectUserName
        if (field.equals(PASSWORD_FIELD, ignoreCase = true)) return protectPassword
        if (field.equals(URL_FIELD, ignoreCase = true)) return protectUrl
        if (field.equals(NOTES_FIELD, ignoreCase = true)) return protectNotes
        return false
    }

    companion object ProtectDefinition {
        const val TITLE_FIELD = "Title"
        const val USERNAME_FIELD = "UserName"
        const val PASSWORD_FIELD = "Password"
        const val URL_FIELD = "URL"
        const val NOTES_FIELD = "Notes"

        const val DEFAULT_PROTECT_TITLE = false
        const val DEFAULT_PROTECT_USERNAME = false
        const val DEFAULT_PROTECT_PASSWORD = true
        const val DEFAULT_PROTECT_URL = false
        const val DEFAULT_PROTECT_NOTES = true
        const val DEFAULT_AUTO_ENABLE_VISUAL_HIDING = false
    }
}
