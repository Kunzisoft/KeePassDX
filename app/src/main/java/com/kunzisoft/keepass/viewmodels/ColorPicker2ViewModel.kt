package com.kunzisoft.keepass.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ColorPicker2ViewModel: ViewModel() {

    val colorPicked : LiveData<Array<Int?>> get() = _colorsPicked
    private val _colorsPicked = MutableLiveData<Array<Int?>>()

    fun pickColors(colors: Array<Int?>) {
        _colorsPicked.value = colors
    }
}