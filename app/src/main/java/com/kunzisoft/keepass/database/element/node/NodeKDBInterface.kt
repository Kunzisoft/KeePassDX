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
package com.kunzisoft.keepass.database.element.node

import com.kunzisoft.keepass.database.element.DateInstant
import org.joda.time.LocalDateTime

interface NodeKDBInterface : NodeTimeInterface {

    override var expires: Boolean
        // If expireDate is before NEVER_EXPIRE date less 1 month (to be sure)
        // it is not expires
        get() = LocalDateTime(expiryTime.date)
                .isBefore(LocalDateTime.fromDateFields(DateInstant.NEVER_EXPIRE.date)
                        .minusMonths(1))
        set(value) {
            if (!value)
                expiryTime = DateInstant.NEVER_EXPIRE
        }
}

