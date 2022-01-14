package com.kunzisoft.keepass.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ColorPickerViewModel: ViewModel() {

    val colorPicked : LiveData<Int?> get() = _colorPicked
    private val _colorPicked = MutableLiveData<Int?>()

    fun pickColor(color: Int?) {
        _colorPicked.value = color
    }
}