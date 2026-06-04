/*
 * Copyright 2021 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.viewmodels

import android.app.Application
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.EntryId
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.template.TemplateField
import com.kunzisoft.keepass.database.helper.getLocalizedName
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.FieldProtection
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.settings.PreferencesUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * ViewModel for managing entry data and state.
 */
class EntryViewModel(application: Application): AndroidViewModel(application) {

    var mainEntryId: EntryId? = null
        private set
    var historyPosition: Int = -1
        private set
    var entryIsHistory: Boolean = false
        private set
    val entryLoaded: Boolean
        get() = entryUIState.value.loaded

    @ColorInt
    var colorSecondary: Int = 0
    @ColorInt
    var colorSurface: Int = 0
    @ColorInt
    var colorOnSurface: Int = 0
    @ColorInt
    var colorBackground: Int = 0
    @ColorInt
    private var backgroundColor: Int? = null
    @ColorInt
    private var foregroundColor: Int? = null

    private var autoSwitchToMagikeyboard: Boolean = false
    private var keyboardEntrySelectionEnabled: Boolean = false
    private var showEntryColors: Boolean = false

    private val _entryUIState = MutableStateFlow(EntryViewState())
    val entryUIState: StateFlow<EntryViewState> = _entryUIState.asStateFlow()

    private val _entryEvents = MutableSharedFlow<EntryEvent>(replay = 0)
    val entryEvents: SharedFlow<EntryEvent> = _entryEvents.asSharedFlow()

    private val _onOtpElementUpdated = MutableSharedFlow<OtpElement?>(replay = 0)
    val onOtpElementUpdated: SharedFlow<OtpElement?> = _onOtpElementUpdated.asSharedFlow()

    private val _onAttachmentAction = MutableSharedFlow<EntryAttachmentState?>(replay = 0)
    val onAttachmentAction: SharedFlow<EntryAttachmentState?> = _onAttachmentAction.asSharedFlow()

    private val _onFieldProtectionUpdated = MutableSharedFlow<FieldProtection>(replay = 0)
    val onFieldProtectionUpdated: SharedFlow<FieldProtection> = _onFieldProtectionUpdated.asSharedFlow()


    init {
        // Init preferences
        autoSwitchToMagikeyboard = PreferencesUtil.isAutoSwitchToMagikeyboardEnable(application)
        keyboardEntrySelectionEnabled = PreferencesUtil.isKeyboardEntrySelectionEnable(application)
        showEntryColors = PreferencesUtil.showEntryColors(application)
    }

    fun loadDatabase(database: ContextualDatabase?) {
        loadEntry(database, mainEntryId, historyPosition)
    }

    fun loadEntry(
        database: ContextualDatabase?,
        mainEntryId: EntryId?,
        historyPosition: Int = -1,
    ) {
        this.mainEntryId = mainEntryId
        this.historyPosition = historyPosition

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (database != null && mainEntryId != null) {
                    database.getEntryById(mainEntryId)?.let { mainEntry ->
                        val isHistory = historyPosition > -1
                        val currentEntry = if (isHistory) {
                            mainEntry.getHistory()[historyPosition]
                        } else {
                            mainEntry
                        }
                        val entryInfo = database.getEntryInfoFrom(currentEntry)
                        this@EntryViewModel.entryIsHistory = isHistory

                        _entryEvents.emit(EntryEvent.EntryLoaded(entryInfo))

                        // Assign colors
                        backgroundColor = if (showEntryColors) entryInfo.backgroundColor else null
                        foregroundColor = if (showEntryColors) entryInfo.foregroundColor else null

                        // To show Entry UI
                        _entryUIState.update {
                            it.copy(
                                loaded = true,
                                entryInfo = entryInfo,
                                showFloatingActionButton = !isHistory,
                                showHistoryView = isHistory,
                                entryHistory = if (!isHistory)
                                    database.getHistoryEntryInfoFrom(mainEntryId) ?: listOf()
                                else listOf()
                            )
                        }

                        // Manage entry to populate Magikeyboard and launch keyboard notification if allowed
                        if (keyboardEntrySelectionEnabled) {
                            _entryEvents.emit(
                                EntryEvent.AddToMagikeyboard(
                                    entryInfo = entryInfo,
                                    autoSwitch = autoSwitchToMagikeyboard
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun updateProtectionField(fieldProtection: FieldProtection, value: Boolean) {
        fieldProtection.isCurrentlyProtected = value
        viewModelScope.launch {
            _onFieldProtectionUpdated.emit(fieldProtection)
        }
    }

    fun requestChangeFieldProtection(fieldProtection: FieldProtection) {
        viewModelScope.launch {
            _entryEvents.emit(EntryEvent.ChangeFieldProtectionRequested(fieldProtection))
        }
    }

    fun requestCopyField(fieldProtection: FieldProtection) {
        // Only request the User Verification if the field is protected and not shown
        val field = fieldProtection.field
        if (field.protectedValue.isProtected && fieldProtection.isCurrentlyProtected) {
            viewModelScope.launch {
                _entryEvents.emit(EntryEvent.RequestCopyProtectedField(fieldProtection))
            }
        } else {
            copyToClipboard(field)
        }
    }

    fun onOtpElementUpdated(optElement: OtpElement?) {
        viewModelScope.launch {
            _onOtpElementUpdated.emit(optElement)
        }
    }

    fun onAttachmentSelected(attachment: Attachment) {
        viewModelScope.launch {
            _entryEvents.emit(EntryEvent.AttachmentSelected(attachment))
        }
    }

    fun onAttachmentAction(entryAttachmentState: EntryAttachmentState?) {
        viewModelScope.launch {
            _onAttachmentAction.emit(entryAttachmentState?.copy())
        }
    }

    fun onHistorySelected(item: EntryInfo, position: Int) {
        viewModelScope.launch {
            _entryEvents.emit(EntryEvent.HistorySelected(EntryHistory(item.nodeId, item, position)))
        }
    }

    fun selectSection(section: EntrySection) {
        _entryUIState.update { it.copy(sectionSelected = section) }
    }

    fun copyToClipboard(field: Field) {
        viewModelScope.launch {
            _entryEvents.emit(
                EntryEvent.CopyToClipboard(
                    label = TemplateField.getLocalizedName(getApplication(), field.name),
                    content = field.protectedValue.toString(),
                    isProtected = field.protectedValue.isProtected
                )
            )
        }
    }

    fun copyToClipboard(text: String) {
        viewModelScope.launch {
            _entryEvents.emit(
                EntryEvent.CopyToClipboard(
                    label = text,
                    content = text,
                    isProtected = false
                )
            )
        }
    }

    fun applyToolbarColors() {
        viewModelScope.launch {
            _entryUIState.update {
                it.copy(
                    toolbarColor = backgroundColor ?: colorSurface,
                    onToolbarColor = foregroundColor ?: colorOnSurface,
                    iconColor = foregroundColor ?: colorSecondary,
                    iconBackgroundColor = backgroundColor?.let { background ->
                        ColorUtils.blendARGB(background, Color.WHITE, 0.1f)
                    } ?: colorBackground
                )
            }
        }
    }

    /**
     * Sealed class for entry events.
     */
    sealed class EntryEvent {
        data class EntryLoaded(
            val entryInfo: EntryInfo,
        ) : EntryEvent()

        data class AddToMagikeyboard(
            val entryInfo: EntryInfo,
            val autoSwitch: Boolean,
        ) : EntryEvent()

        data class CopyToClipboard(
            val label: String,
            val content: String,
            val isProtected: Boolean,
        ) : EntryEvent()

        data class RequestCopyProtectedField(
            val fieldProtection: FieldProtection,
        ) : EntryEvent()

        data class ChangeFieldProtectionRequested(
            val fieldProtection: FieldProtection,
        ) : EntryEvent()

        data class AttachmentSelected(
            val attachment: Attachment,
        ) : EntryEvent()

        data class HistorySelected(
            val entryHistory: EntryHistory,
        ) : EntryEvent()
    }

    /**
     * Custom data class to manage entry history.
     */
    data class EntryHistory(
        var nodeId: EntryId,
        var entryInfo: EntryInfo,
        var historyPosition: Int = -1
    )

    /**
     * Enum for entry sections.
     */
    enum class EntrySection(var position: Int) {
        MAIN(0), ADVANCED(1);

        companion object {
            fun getEntrySectionByPosition(position: Int): EntrySection {
                return if (position == 1) ADVANCED else MAIN
            }
        }
    }

    /**
     * Data class for entry view state.
     */
    data class EntryViewState(
        val loaded: Boolean = false,
        val entryInfo: EntryInfo? = null,
        val showFloatingActionButton: Boolean = false,
        val showHistoryView: Boolean = false,
        val toolbarColor: Int = 0,
        val onToolbarColor: Int = 0,
        val iconColor: Int = 0,
        val iconBackgroundColor: Int = 0,
        val entryHistory: List<EntryInfo> = listOf(),
        val sectionSelected: EntrySection = EntrySection.MAIN
    )

    companion object {
        private val TAG = EntryViewModel::class.java.name
    }
}