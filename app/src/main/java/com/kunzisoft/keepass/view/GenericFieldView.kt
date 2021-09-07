package com.kunzisoft.keepass.view

interface GenericFieldView {
    var label: String
    var value: String
    var default: String
    var isFieldVisible: Boolean
}