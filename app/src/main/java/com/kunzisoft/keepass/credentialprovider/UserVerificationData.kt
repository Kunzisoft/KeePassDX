/*
 * Copyright 2025 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.credentialprovider

import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.EntryId
import com.kunzisoft.keepass.model.FieldProtection

data class UserVerificationData(
    val actionType: UserVerificationActionType,
    val database: ContextualDatabase? = null,
    val entryId: EntryId? = null,
    val fieldProtection: FieldProtection? = null,
    val preferenceKey: String? = null,
    val originName: String? = null
)

enum class UserVerificationActionType {
    LAUNCH_AUTHENTICATION_CEREMONY,
    SHOW_PROTECTED_FIELD,
    COPY_PROTECTED_FIELD,
    SHARE_PROTECTED_FIELD,
    EDIT_ENTRY,
    EDIT_DATABASE_SETTING,
    MERGE_FROM_DATABASE,
    SAVE_DATABASE_COPY_TO
}