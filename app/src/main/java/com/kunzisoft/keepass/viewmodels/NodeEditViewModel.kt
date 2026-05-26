package com.kunzisoft.keepass.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.model.DataDate
import com.kunzisoft.keepass.model.DataTime
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Abstract ViewModel for node editing common properties (icon, color, date).
 */
abstract class NodeEditViewModel : ViewModel() {

    private val _requestIconSelection = MutableSharedFlow<IconImage>(replay = 0)
    val requestIconSelection: SharedFlow<IconImage> = _requestIconSelection.asSharedFlow()

    private val _onIconSelected = MutableSharedFlow<IconImage>(replay = 0)
    val onIconSelected: SharedFlow<IconImage> = _onIconSelected.asSharedFlow()

    private var mColorRequest: ColorRequest = ColorRequest.BACKGROUND
    private val _requestColorSelection = MutableSharedFlow<Int?>(replay = 0)
    val requestColorSelection: SharedFlow<Int?> = _requestColorSelection.asSharedFlow()

    private val _onBackgroundColorSelected = MutableSharedFlow<Int?>(replay = 0)
    val onBackgroundColorSelected: SharedFlow<Int?> = _onBackgroundColorSelected.asSharedFlow()

    private val _onForegroundColorSelected = MutableSharedFlow<Int?>(replay = 0)
    val onForegroundColorSelected: SharedFlow<Int?> = _onForegroundColorSelected.asSharedFlow()

    private val _requestDateTimeSelection = MutableSharedFlow<DateInstant>(replay = 0)
    val requestDateTimeSelection: SharedFlow<DateInstant> = _requestDateTimeSelection.asSharedFlow()

    private val _onDateSelected = MutableSharedFlow<DataDate>(replay = 0)
    val onDateSelected: SharedFlow<DataDate> = _onDateSelected.asSharedFlow()

    private val _onTimeSelected = MutableSharedFlow<DataTime>(replay = 0)
    val onTimeSelected: SharedFlow<DataTime> = _onTimeSelected.asSharedFlow()

    fun requestIconSelection(oldIconImage: IconImage) {
        viewModelScope.launch {
            _requestIconSelection.emit(oldIconImage)
        }
    }

    fun selectIcon(iconImage: IconImage) {
        viewModelScope.launch {
            _onIconSelected.emit(iconImage)
        }
    }

    fun requestBackgroundColorSelection(initialColor: Int?) {
        mColorRequest = ColorRequest.BACKGROUND
        viewModelScope.launch {
            _requestColorSelection.emit(initialColor)
        }
    }

    fun requestForegroundColorSelection(initialColor: Int?) {
        mColorRequest = ColorRequest.FOREGROUND
        viewModelScope.launch {
            _requestColorSelection.emit(initialColor)
        }
    }

    fun selectColor(color: Int?) {
        viewModelScope.launch {
            when (mColorRequest) {
                ColorRequest.BACKGROUND -> _onBackgroundColorSelected.emit(color)
                ColorRequest.FOREGROUND -> _onForegroundColorSelected.emit(color)
            }
        }
    }

    fun requestDateTimeSelection(dateInstant: DateInstant) {
        viewModelScope.launch {
            _requestDateTimeSelection.emit(dateInstant)
        }
    }

    fun selectDate(date: DataDate) {
        viewModelScope.launch {
            _onDateSelected.emit(date)
        }
    }

    fun selectTime(dataTime: DataTime) {
        viewModelScope.launch {
            _onTimeSelected.emit(dataTime)
        }
    }

    private enum class ColorRequest {
        BACKGROUND, FOREGROUND
    }
}