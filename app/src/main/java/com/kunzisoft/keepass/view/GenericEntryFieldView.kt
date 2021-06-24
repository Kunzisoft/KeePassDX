package com.kunzisoft.keepass.view

interface GenericEntryFieldView {

    var value: String
    fun applyFontVisibility(fontInVisibility: Boolean)
    var isFieldVisible: Boolean
}