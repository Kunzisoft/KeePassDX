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

    private val _nodeEditEvents = MutableSharedFlow<NodeEditEvent>(replay = 0)
    val nodeEditEvents: SharedFlow<NodeEditEvent> = _nodeEditEvents.asSharedFlow()

    private var mColorRequest: ColorRequest = ColorRequest.BACKGROUND

    fun requestIconSelection(oldIconImage: IconImage) {
        viewModelScope.launch {
            _nodeEditEvents.emit(NodeEditEvent.RequestIconSelection(oldIconImage))
        }
    }

    fun selectIcon(iconImage: IconImage) {
        viewModelScope.launch {
            _nodeEditEvents.emit(NodeEditEvent.OnIconSelected(iconImage))
        }
    }

    fun requestBackgroundColorSelection(initialColor: Int?) {
        mColorRequest = ColorRequest.BACKGROUND
        viewModelScope.launch {
            _nodeEditEvents.emit(NodeEditEvent.RequestColorSelection(initialColor))
        }
    }

    fun requestForegroundColorSelection(initialColor: Int?) {
        mColorRequest = ColorRequest.FOREGROUND
        viewModelScope.launch {
            _nodeEditEvents.emit(NodeEditEvent.RequestColorSelection(initialColor))
        }
    }

    fun selectColor(color: Int?) {
        viewModelScope.launch {
            when (mColorRequest) {
                ColorRequest.BACKGROUND -> _nodeEditEvents.emit(NodeEditEvent.OnBackgroundColorSelected(color))
                ColorRequest.FOREGROUND -> _nodeEditEvents.emit(NodeEditEvent.OnForegroundColorSelected(color))
            }
        }
    }

    fun requestDateTimeSelection(dateInstant: DateInstant) {
        viewModelScope.launch {
            _nodeEditEvents.emit(NodeEditEvent.RequestDateTimeSelection(dateInstant))
        }
    }

    fun selectDate(date: DataDate) {
        viewModelScope.launch {
            _nodeEditEvents.emit(NodeEditEvent.OnDateSelected(date))
        }
    }

    fun selectTime(dataTime: DataTime) {
        viewModelScope.launch {
            _nodeEditEvents.emit(NodeEditEvent.OnTimeSelected(dataTime))
        }
    }

    private enum class ColorRequest {
        BACKGROUND, FOREGROUND
    }

    sealed class NodeEditEvent {
        data class RequestIconSelection(
            val icon: IconImage,
        ) : NodeEditEvent()

        data class OnIconSelected(
            val icon: IconImage
        ) : NodeEditEvent()

        data class RequestColorSelection(
            val color: Int?
        ) : NodeEditEvent()

        data class OnBackgroundColorSelected(
            val color: Int?
        ) : NodeEditEvent()

        data class OnForegroundColorSelected(
            val color: Int?
        ) : NodeEditEvent()

        data class RequestDateTimeSelection(
            val dateInstant: DateInstant
        ) : NodeEditEvent()

        data class OnDateSelected(
            val date: DataDate
        ) : NodeEditEvent()

        data class OnTimeSelected(
            val time: DataTime
        ) : NodeEditEvent()
    }
}