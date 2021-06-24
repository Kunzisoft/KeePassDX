package com.kunzisoft.keepass.view

import com.kunzisoft.keepass.database.element.DateInstant

interface GenericDateTimeView {

    var activation: Boolean
    var dateTime: DateInstant
    var isFieldVisible: Boolean
}