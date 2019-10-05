package com.kunzisoft.keepass.database.element

import org.joda.time.LocalDateTime

interface PwNodeV3Interface : NodeTimeInterface {

    override var expires: Boolean
        // If expireDate is before NEVER_EXPIRE date less 1 month (to be sure)
        // it is not expires
        get() = LocalDateTime(expiryTime.date)
                .isBefore(LocalDateTime.fromDateFields(PwDate.NEVER_EXPIRE.date)
                        .minusMonths(1))
        set(value) {
            if (!value)
                expiryTime = PwDate.NEVER_EXPIRE
        }
}

