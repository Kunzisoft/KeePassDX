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

