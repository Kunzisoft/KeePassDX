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
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.FieldProtection
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.otp.OtpType
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

    private var mDatabase: ContextualDatabase? = null
    var mainEntryId: EntryId? = null
        private set
    var historyPosition: Int = -1
        private set
    var entryIsHistory: Boolean = false
        private set

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

    private val _onTotpProgressUpdated = MutableStateFlow<TotpProgress?>(null)
    val onTotpProgressUpdated: StateFlow<TotpProgress?> = _onTotpProgressUpdated.asStateFlow()

    private val _onFieldProtectionUpdated = MutableSharedFlow<FieldProtection>(replay = 0)
    val onFieldProtectionUpdated: SharedFlow<FieldProtection> = _onFieldProtectionUpdated.asSharedFlow()

    init {
        // Init preferences
        autoSwitchToMagikeyboard = PreferencesUtil.isAutoSwitchToMagikeyboardEnable(application)
        keyboardEntrySelectionEnabled = PreferencesUtil.isKeyboardEntrySelectionEnable(application)
        showEntryColors = PreferencesUtil.showEntryColors(application)
    }

    fun loadDatabase(database: ContextualDatabase?) {
        this.mDatabase = database
        loadEntry(mainEntryId, historyPosition)
    }

    fun loadEntry(
        mainEntryId: EntryId?,
        historyPosition: Int = -1,
    ) {
        this.mainEntryId = mainEntryId
        this.historyPosition = historyPosition

        viewModelScope.launch {
            mDatabase?.let { database ->
                _entryUIState.update { entryState ->
                    entryState.copy(loaded = false)
                }
                if (mainEntryId != null) {
                    withContext(Dispatchers.IO) {
                        database.getEntryById(mainEntryId)?.let { mainEntry ->
                            val isHistory = historyPosition > -1
                            val currentEntry = if (isHistory) {
                                mainEntry.getHistory()[historyPosition]
                            } else {
                                mainEntry
                            }
                            val entryInfo = database.getEntryInfoFrom(currentEntry)
                            this@EntryViewModel.entryIsHistory = isHistory

                            withContext(Dispatchers.Main) {
                                _entryEvents.emit(EntryEvent.EntryLoaded(entryInfo))

                                // Assign colors
                                val background =
                                    if (showEntryColors) entryInfo.backgroundColor else null
                                val foreground =
                                    if (showEntryColors) entryInfo.foregroundColor else null
                                backgroundColor = background
                                foregroundColor = foreground

                                // To show Entry UI
                                _entryUIState.update {
                                    it.copy(
                                        loaded = true,
                                        entryInfo = entryInfo,
                                        isReadOnly = database.isReadOnly,
                                        showFloatingActionButton = !isHistory,
                                        showHistoryView = isHistory,
                                        entryHistory = if (!isHistory)
                                            database.getHistoryEntryInfoFrom(mainEntryId) ?: listOf()
                                        else listOf(),
                                        toolbarColor = background ?: colorSurface,
                                        onToolbarColor = foreground ?: colorOnSurface,
                                        iconColor = foreground ?: colorSecondary,
                                        iconBackgroundColor = background?.let { bg ->
                                            ColorUtils.blendARGB(bg, Color.WHITE, 0.1f)
                                        } ?: colorBackground
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
                        } ?: run {
                            withContext(Dispatchers.Main) {
                                _entryEvents.emit(EntryEvent.Close)
                            }
                        }
                    }
                }
            }
        }
    }

    fun updateProtectionField(fieldProtection: FieldProtection, isRevealed: Boolean) {
        fieldProtection.isRevealed = isRevealed
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
        viewModelScope.launch {
            _entryEvents.emit(EntryEvent.RequestCopyProtectedField(fieldProtection))
        }
    }

    fun onOtpElementUpdated(otpElement: OtpElement?) {
        val totpProgress: TotpProgress? = when (otpElement?.type) {
            // Refresh view if TOTP
            OtpType.TOTP -> {
                val max = 100
                TotpProgress(
                    max = max,
                    progress = max * otpElement.secondsRemaining / otpElement.period
                )
            }
            else -> null
        }
        _onTotpProgressUpdated.value = totpProgress
    }

    fun onAttachmentSelected(attachment: Attachment) {
        viewModelScope.launch {
            _entryEvents.emit(EntryEvent.AttachmentSelected(attachment))
        }
    }

    fun onHistorySelected(item: EntryInfo, position: Int) {
        viewModelScope.launch {
            _entryEvents.emit(EntryEvent.HistorySelected(EntryHistory(item.nodeId, item, position)))
        }
    }

    fun onSectionSelected(section: EntrySection) {
        viewModelScope.launch {
            _entryEvents.emit(EntryEvent.SectionSelected(section))
        }
    }

    fun applyToolbarColors() {
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

    fun entryHistoryActionsAllowed(): Boolean = entryUIState.value.loaded && entryIsHistory && mDatabase?.isReadOnly == false

    fun databaseActionsAllowed(): Boolean = entryUIState.value.loaded
    fun saveDatabaseActionAllowed(): Boolean = !entryIsHistory && mDatabase?.isReadOnly == false
    fun mergeDatabaseActionAllowed(): Boolean = !entryIsHistory && saveDatabaseActionAllowed() && mDatabase?.isMergeDataAllowed() == true
    fun reloadDatabaseActionAllowed(): Boolean = !entryIsHistory

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

        data class SectionSelected(
            val section: EntrySection,
        ) : EntryEvent()

        object Close : EntryEvent()
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
        val isReadOnly: Boolean = true,
        val showFloatingActionButton: Boolean = false,
        val showHistoryView: Boolean = false,
        val toolbarColor: Int = 0,
        val onToolbarColor: Int = 0,
        val iconColor: Int = 0,
        val iconBackgroundColor: Int = 0,
        val entryHistory: List<EntryInfo> = listOf(),
    )

    data class TotpProgress(
        val max: Int,
        val progress: Int
    )

    companion object {
        private val TAG = EntryViewModel::class.java.name
    }
}