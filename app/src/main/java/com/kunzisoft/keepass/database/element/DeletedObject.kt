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
package com.kunzisoft.keepass.database.element

import com.kunzisoft.keepass.database.element.database.DatabaseVersioned
import java.util.Date
import java.util.UUID

class DeletedObject {

    var uuid: UUID = DatabaseVersioned.UUID_ZERO
    private var mDeletionTime: Date? = null

    fun getDeletionTime(): Date {
        if (mDeletionTime == null) {
            mDeletionTime = Date(System.currentTimeMillis())
        }
        return mDeletionTime!!
    }

    fun setDeletionTime(deletionTime: Date) {
        this.mDeletionTime = deletionTime
    }

    constructor()

    @JvmOverloads
    constructor(uuid: UUID, deletionTime: Date = Date()) {
        this.uuid = uuid
        this.mDeletionTime = deletionTime
    }

    override fun equals(other: Any?): Boolean {
        if (this === other)
            return true
        if (other == null)
            return false
        if (other !is DeletedObject)
            return false
        return uuid == other.uuid
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }
}
