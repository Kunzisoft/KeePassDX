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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kunzisoft.keepass.credentialprovider.magikeyboard.MagikeyboardService
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.template.TemplateField
import com.kunzisoft.keepass.database.helper.getLocalizedName
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.FieldProtection
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.ClipboardHelper
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
import java.util.UUID


/**
 * ViewModel for managing entry data and state.
 */
class EntryViewModel(application: Application): AndroidViewModel(application) {

    var mainEntryId: NodeId<UUID>? = null
        private set
    var entryInfo: EntryInfo? = null
        private set
    var historyPosition: Int = -1
        private set
    var entryIsHistory: Boolean = false
        private set
    var entryLoaded = false
        private set

    private var autoSwitchToMagikeyboard: Boolean = false
    private var keyboardEntrySelectionEnabled: Boolean = false

    private val _clipboardHelper: ClipboardHelper = ClipboardHelper(application)


    private val _entryUIState = MutableStateFlow<EntryState>(EntryState())
    val entryUIState: StateFlow<EntryState> = _entryUIState.asStateFlow()

    private val _entryHistoryState = MutableStateFlow(EntryHistoryState())
    val entryHistoryState: StateFlow<EntryHistoryState> = _entryHistoryState.asStateFlow()

    private val _sectionSelected = MutableStateFlow(EntrySection.MAIN)
    val sectionSelected: StateFlow<EntrySection> = _sectionSelected.asStateFlow()

    private val _onEntryLoaded = MutableSharedFlow<OnEntryLoaded>(replay = 0)
    val onEntryLoaded: SharedFlow<OnEntryLoaded> = _onEntryLoaded.asSharedFlow()

    private val _onOtpElementUpdated = MutableSharedFlow<OtpElement?>(replay = 0)
    val onOtpElementUpdated: SharedFlow<OtpElement?> = _onOtpElementUpdated.asSharedFlow()

    private val _attachmentSelected = MutableSharedFlow<Attachment>(replay = 0)
    val attachmentSelected: SharedFlow<Attachment> = _attachmentSelected.asSharedFlow()

    private val _onAttachmentAction = MutableSharedFlow<EntryAttachmentState?>(replay = 0)
    val onAttachmentAction: SharedFlow<EntryAttachmentState?> = _onAttachmentAction.asSharedFlow()

    private val _historySelected = MutableSharedFlow<EntryHistory>(replay = 0)
    val historySelected: SharedFlow<EntryHistory> = _historySelected.asSharedFlow()

    private val _requestCopyProtectedField = MutableSharedFlow<FieldProtection>(replay = 0)
    val requestCopyProtectedField: SharedFlow<FieldProtection> = _requestCopyProtectedField.asSharedFlow()

    private val _onChangeFieldProtectionRequested = MutableSharedFlow<FieldProtection>(replay = 0)
    val onChangeFieldProtectionRequested: SharedFlow<FieldProtection> = _onChangeFieldProtectionRequested.asSharedFlow()

    private val _onFieldProtectionUpdated = MutableSharedFlow<FieldProtection>(replay = 0)
    val onFieldProtectionUpdated: SharedFlow<FieldProtection> = _onFieldProtectionUpdated.asSharedFlow()


    init {
        // Init preferences
        autoSwitchToMagikeyboard = PreferencesUtil.isAutoSwitchToMagikeyboardEnable(application)
        keyboardEntrySelectionEnabled = PreferencesUtil.isKeyboardEntrySelectionEnable(application)
    }

    fun loadDatabase(database: ContextualDatabase?) {
        loadEntry(database, mainEntryId, historyPosition)
    }

    fun loadEntry(
        database: ContextualDatabase?,
        mainEntryId: NodeId<UUID>?,
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
                        this@EntryViewModel.entryInfo = entryInfo
                        this@EntryViewModel.entryIsHistory = isHistory
                        this@EntryViewModel.entryLoaded = true

                        _onEntryLoaded.emit(OnEntryLoaded(
                            entryInfo = entryInfo
                        ))
                        // To show Entry UI
                        _entryUIState.update {
                            it.copy(
                                loading = false,
                                entryInfo = entryInfo,
                                showFloatingActionButton = !isHistory,
                                showHistoryView = isHistory
                            )
                        }

                        // To build entry history
                        val entryHistory: List<EntryInfo> = if (!isHistory)
                            database.getHistoryEntryInfoFrom(mainEntryId) ?: listOf()
                        else listOf()
                        _entryHistoryState.update { currentState ->
                            currentState.copy(
                                entryHistory = entryHistory
                            )
                        }

                        withContext(Dispatchers.Main) {
                            // Manage entry to populate Magikeyboard and launch keyboard notification if allowed
                            if (keyboardEntrySelectionEnabled) {
                                MagikeyboardService.addEntry(
                                    context = getApplication(),
                                    entry = entryInfo,
                                    autoSwitchKeyboard = autoSwitchToMagikeyboard
                                )
                            }
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
            _onChangeFieldProtectionRequested.emit(fieldProtection)
        }
    }

    fun requestCopyField(fieldProtection: FieldProtection) {
        // Only request the User Verification if the field is protected and not shown
        val field = fieldProtection.field
        if (field.protectedValue.isProtected && fieldProtection.isCurrentlyProtected) {
            viewModelScope.launch {
                _requestCopyProtectedField.emit(fieldProtection)
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
            _attachmentSelected.emit(attachment)
        }
    }

    fun onAttachmentAction(entryAttachmentState: EntryAttachmentState?) {
        viewModelScope.launch {
            _onAttachmentAction.emit(entryAttachmentState)
        }
    }

    fun onHistorySelected(item: EntryInfo, position: Int) {
        viewModelScope.launch {
            _historySelected.emit(EntryHistory(item.nodeId, item, position))
        }
    }

    fun selectSection(section: EntrySection) {
        _sectionSelected.value = section
    }

    fun copyToClipboard(field: Field) {
        _clipboardHelper.timeoutCopyToClipboard(
            TemplateField.getLocalizedName(getApplication(), field.name),
            field.protectedValue.toString(),
            field.protectedValue.isProtected
        )
    }

    fun copyToClipboard(text: String) {
        _clipboardHelper.timeoutCopyToClipboard(text, text)
    }

    /**
     * Custom data class to manage entry history.
     */
    data class EntryHistory(
        var nodeId: NodeId<UUID>,
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

    data class OnEntryLoaded(
        val entryInfo: EntryInfo
    )

    data class EntryState(
        val loading: Boolean = true,
        val entryInfo: EntryInfo? = null,
        val showFloatingActionButton: Boolean = false,
        val showHistoryView: Boolean = false
    )

    data class EntryHistoryState(
        val entryHistory: List<EntryInfo> = listOf()
    )

    companion object {
        private val TAG = EntryViewModel::class.java.name
    }
}