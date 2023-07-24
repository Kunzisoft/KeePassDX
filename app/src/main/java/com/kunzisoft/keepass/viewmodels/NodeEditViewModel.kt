package com.kunzisoft.keepass.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.view.DataTime

abstract class NodeEditViewModel : ViewModel() {

    val requestIconSelection : LiveData<IconImage> get() = _requestIconSelection
    private val _requestIconSelection = SingleLiveEvent<IconImage>()
    val onIconSelected : LiveData<IconImage> get() = _onIconSelected
    private val _onIconSelected = SingleLiveEvent<IconImage>()

    private var mColorRequest: ColorRequest = ColorRequest.BACKGROUND
    val requestColorSelection : LiveData<Int?> get() = _requestColorSelection
    private val _requestColorSelection = SingleLiveEvent<Int?>()
    val onBackgroundColorSelected : LiveData<Int?> get() = _onBackgroundColorSelected
    private val _onBackgroundColorSelected = SingleLiveEvent<Int?>()
    val onForegroundColorSelected : LiveData<Int?> get() = _onForegroundColorSelected
    private val _onForegroundColorSelected = SingleLiveEvent<Int?>()

    val requestDateTimeSelection : LiveData<DateInstant> get() = _requestDateTimeSelection
    private val _requestDateTimeSelection = SingleLiveEvent<DateInstant>()
    val onDateSelected : LiveData<Long> get() = _onDateSelected
    private val _onDateSelected = SingleLiveEvent<Long>()
    val onTimeSelected : LiveData<DataTime> get() = _onTimeSelected
    private val _onTimeSelected = SingleLiveEvent<DataTime>()

    fun requestIconSelection(oldIconImage: IconImage) {
        _requestIconSelection.value = oldIconImage
    }

    fun selectIcon(iconImage: IconImage) {
        _onIconSelected.value = iconImage
    }

    fun requestBackgroundColorSelection(initialColor: Int?) {
        mColorRequest = ColorRequest.BACKGROUND
        _requestColorSelection.value = initialColor
    }

    fun requestForegroundColorSelection(initialColor: Int?) {
        mColorRequest = ColorRequest.FOREGROUND
        _requestColorSelection.value = initialColor
    }

    fun selectColor(color: Int?) {
        when (mColorRequest) {
            ColorRequest.BACKGROUND -> _onBackgroundColorSelected.value = color
            ColorRequest.FOREGROUND -> _onForegroundColorSelected.value = color
        }
    }

    fun requestDateTimeSelection(dateInstant: DateInstant) {
        _requestDateTimeSelection.value = dateInstant
    }

    fun selectDate(dateMilliseconds: Long) {
        _onDateSelected.value = dateMilliseconds
    }

    fun selectTime(hours: Int, minutes: Int) {
        _onTimeSelected.value = DataTime(hours, minutes)
    }

    private enum class ColorRequest {
        BACKGROUND, FOREGROUND
    }
}