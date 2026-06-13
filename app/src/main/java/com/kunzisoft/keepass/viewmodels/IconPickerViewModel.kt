package com.kunzisoft.keepass.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunzisoft.keepass.database.element.icon.IconImageCustom
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for icon picker.
 */
class IconPickerViewModel : ViewModel() {

    private val _standardIconPicked = MutableSharedFlow<IconImageStandard>()
    /**
     * SharedFlow for standard icon picked event.
     */
    val standardIconPicked: SharedFlow<IconImageStandard> = _standardIconPicked.asSharedFlow()

    private val _customIconPicked = MutableSharedFlow<IconImageCustom>()
    /**
     * SharedFlow for custom icon picked event.
     */
    val customIconPicked: SharedFlow<IconImageCustom> = _customIconPicked.asSharedFlow()

    private val _customIconsSelected = MutableStateFlow<List<IconImageCustom>>(emptyList())
    /**
     * StateFlow for list of selected custom icons.
     */
    val customIconsSelected: StateFlow<List<IconImageCustom>> = _customIconsSelected.asStateFlow()

    private val _customIconAdded = MutableSharedFlow<IconCustomState>()
    /**
     * SharedFlow for custom icon added event.
     */
    val customIconAdded: SharedFlow<IconCustomState> = _customIconAdded.asSharedFlow()

    private val _customIconRemoved = MutableSharedFlow<IconCustomState>()
    /**
     * SharedFlow for custom icon removed event.
     */
    val customIconRemoved: SharedFlow<IconCustomState> = _customIconRemoved.asSharedFlow()

    private val _customIconUpdated = MutableSharedFlow<IconCustomState>()
    /**
     * SharedFlow for custom icon updated event.
     */
    val customIconUpdated: SharedFlow<IconCustomState> = _customIconUpdated.asSharedFlow()

    /**
     * Pick a standard icon.
     */
    fun pickStandardIcon(icon: IconImageStandard) {
        viewModelScope.launch {
            _standardIconPicked.emit(icon)
        }
    }

    /**
     * Pick a custom icon.
     */
    fun pickCustomIcon(icon: IconImageCustom) {
        viewModelScope.launch {
            _customIconPicked.emit(icon)
        }
    }

    /**
     * Select a list of custom icons.
     */
    fun selectCustomIcons(icons: List<IconImageCustom>) {
        _customIconsSelected.value = icons
    }

    /**
     * Deselect all custom icons.
     */
    fun deselectAllCustomIcons() {
        _customIconsSelected.value = emptyList()
    }

    /**
     * Add a custom icon.
     */
    fun addCustomIcon(customIcon: IconCustomState) {
        viewModelScope.launch {
            _customIconAdded.emit(customIcon)
        }
    }

    /**
     * Remove a custom icon.
     */
    fun removeCustomIcon(customIcon: IconCustomState) {
        viewModelScope.launch {
            _customIconRemoved.emit(customIcon)
        }
    }

    /**
     * Update a custom icon.
     */
    fun updateCustomIcon(customIcon: IconCustomState) {
        viewModelScope.launch {
            _customIconUpdated.emit(customIcon)
        }
    }

    /**
     * State of custom icon.
     */
    data class IconCustomState(
        var iconCustom: IconImageCustom? = null,
        var error: Boolean = true,
        var errorStringId: Int = -1,
        var errorConsumed: Boolean = false,
    )
}
