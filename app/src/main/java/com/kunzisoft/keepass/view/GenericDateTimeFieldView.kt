package com.kunzisoft.keepass.view

import com.kunzisoft.keepass.database.element.DateInstant

interface GenericDateTimeFieldView: GenericFieldView {
    var activation: Boolean
    var dateTime: DateInstant
}