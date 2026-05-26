package com.kunzisoft.keepass.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for picking a color.
 */
class ColorPickerViewModel: ViewModel() {

    private val _colorPicked = MutableSharedFlow<Int?>(replay = 0)
    val colorPicked: SharedFlow<Int?> = _colorPicked.asSharedFlow()

    /**
     * Notify that a color has been picked.
     * @param color The picked color.
     */
    fun pickColor(color: Int?) {
        viewModelScope.launch {
            _colorPicked.emit(color)
        }
    }
}