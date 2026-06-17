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

import androidx.lifecycle.viewModelScope
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.EntryId
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.GroupId
import com.kunzisoft.keepass.database.element.template.Template
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.FieldProtection
import com.kunzisoft.keepass.model.RegisterInfo
import com.kunzisoft.keepass.otp.OtpElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
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
 * ViewModel for editing an entry.
 */
class EntryEditViewModel: NodeEditViewModel() {

    private var mDatabase: ContextualDatabase? = null
    private var mEntryId: EntryId? = null
    private var mParentId: GroupId? = null
    private var mRegisterInfo: RegisterInfo? = null
    private var mTemplates: Templates? = null
    var isTemplate: Boolean = false
        private set
    var allowCustomFields: Boolean = false
        private set
    var allowOTP: Boolean = false
        private set

    var passwordField: Field? = null

    // To show dialog only one time
    var backPressedAlreadyApproved = false

    // Useful to not relaunch a current action
    private var actionLocked: Boolean = false

    private var mInitialEntryInfo: EntryInfo? = null

    private val _entryEditUIState = MutableStateFlow(EntryEditState())
    val entryEditUIState: StateFlow<EntryEditState> = _entryEditUIState.asStateFlow()

    private val _entryEditEvents = MutableSharedFlow<EntryEditEvent>(replay = 0)
    val entryEditEvents: SharedFlow<EntryEditEvent> = _entryEditEvents.asSharedFlow()

    private val _onEntryValidationRequested = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val onEntryValidationRequested: SharedFlow<Unit> = _onEntryValidationRequested.asSharedFlow()

    val entryLoaded: Boolean
        get() = entryEditUIState.value.loaded

    private var initialized = false

    fun onDatabaseLoaded(database: ContextualDatabase) {
        mDatabase = database
        allowCustomFields = database.allowEntryCustomFields() == true
        allowOTP = database.allowOTP == true
        loadTemplateEntry(mEntryId, mParentId, mRegisterInfo)
    }

    fun loadTemplateEntry(
        entryId: EntryId?,
        parentId: GroupId?,
        registerInfo: RegisterInfo?
    ) {
        if (this.mEntryId == entryId
            && this.mParentId == parentId
            && this.mRegisterInfo == registerInfo
            && entryLoaded) {
            return
        }
        this.mEntryId = entryId
        this.mParentId = parentId
        this.mRegisterInfo = registerInfo

        viewModelScope.launch {
            // Just to compensate TemplateView bug
            mTemplates?.let { templates ->
                _entryEditUIState.update {
                    it.copy(
                        loaded = false,
                        templates = templates
                    )
                }
            }
            withContext(Dispatchers.IO) {
                mDatabase?.let { database ->
                    // Update the entry
                    mEntryId?.let {
                        loadEntryInfo(database, database.buildEntryInfoFrom(
                            entryId = it,
                            registerInfo = registerInfo
                        ) {
                            // Action to perform when data is overwritten
                            _entryEditEvents.emit(EntryEditEvent.ShowOverwriteMessage)
                        })
                    }
                    // Create the entry
                    mParentId?.let {
                        loadEntryInfo(database, database.buildNewEntryInfo(
                            parentId = it,
                            registerInfo = registerInfo
                        ))
                    }
                }
            }
        }
    }

    private suspend fun loadEntryInfo(database: ContextualDatabase, entryInfo: EntryInfo?) {
        mInitialEntryInfo = entryInfo?.let { entryInfo ->
            EntryInfo(entryInfo)
        }
        isTemplate = database.entryIsTemplate(entryInfo)
        if (!initialized) {
            initialized = true
            entryInfo?.let {
                withContext(Dispatchers.Main) {
                    _entryEditEvents.emit(EntryEditEvent.EntryLoaded(entryInfo))
                }
            }
        }
        val selectedTemplate = entryInfo?.template ?: Template.STANDARD
        val templates = Templates(
            templates = database.getTemplates(isTemplate),
            defaultTemplate = selectedTemplate
        )
        mTemplates = templates
        withContext(Dispatchers.Main) {
            _entryEditEvents.emit(EntryEditEvent.OnTemplateChanged(selectedTemplate))
            _entryEditUIState.update { entryEdit ->
                entryEdit.copy(
                    loaded = true,
                    entryInfo = entryInfo,
                    templates = templates
                )
            }
        }
    }

    fun changeTemplate(template: Template) {
        viewModelScope.launch {
            _entryEditEvents.emit(EntryEditEvent.OnTemplateChanged(template))
        }
    }

    fun requestEntryValidation() {
        viewModelScope.launch {
            _onEntryValidationRequested.emit(Unit)
        }
    }

    fun scrollTo(viewPosition: Float) {
        viewModelScope.launch {
            // Scroll to the attachment position
            _entryEditEvents.emit(EntryEditEvent.ScrollTo(viewPosition.toInt()))
        }
    }

    fun unlockAction() {
        actionLocked = false
    }

    fun saveEntryInfo(entryInfo: EntryInfo) {
        if (actionLocked.not()) {
            actionLocked = true
            viewModelScope.launch {
                mEntryId?.let {
                    _entryEditEvents.emit(
                        EntryEditEvent.UpdateEntry(entryInfo)
                    )
                } ?: mParentId?.let { parentId ->
                    _entryEditEvents.emit(
                        EntryEditEvent.CreateEntry(
                            parentId = parentId,
                            newEntry = entryInfo
                        )
                    )
                } ?: run {
                    actionLocked = false
                }
            }
        }
    }

    fun requestPasswordSelection(passwordField: Field) {
        this.passwordField = passwordField
        viewModelScope.launch {
            _entryEditEvents.emit(EntryEditEvent.RequestPasswordSelection)
        }
    }

    fun selectPassword(passwordField: Field) {
        viewModelScope.launch {
            _entryEditEvents.emit(EntryEditEvent.OnPasswordSelected(passwordField))
        }
    }

    fun requestChangeFieldProtection(fieldProtection: FieldProtection) {
        viewModelScope.launch {
            _entryEditEvents.emit(EntryEditEvent.OnChangeFieldProtectionRequested(fieldProtection))
        }
    }

    fun requestCustomFieldEdition(customField: Field) {
        viewModelScope.launch {
            _entryEditEvents.emit(EntryEditEvent.RequestCustomFieldEdition(customField))
        }
    }

    fun addCustomField(newField: Field) {
        viewModelScope.launch {
            _entryEditEvents.emit(
                EntryEditEvent.OnCustomFieldEdited(oldField = null, newField = newField)
            )
        }
    }

    fun editCustomField(oldField: Field, newField: Field) {
        viewModelScope.launch {
            _entryEditEvents.emit(
                EntryEditEvent.OnCustomFieldEdited(oldField = oldField, newField = newField)
            )
        }
    }

    fun removeCustomField(oldField: Field) {
        viewModelScope.launch {
            _entryEditEvents.emit(
                EntryEditEvent.OnCustomFieldEdited(oldField = oldField, newField = null)
            )
        }
    }

    fun showCustomFieldEditionError() {
        viewModelScope.launch {
            _entryEditEvents.emit(EntryEditEvent.OnCustomFieldError)
        }
    }

    fun setupOtp() {
        viewModelScope.launch {
            _entryEditEvents.emit(EntryEditEvent.RequestSetupOTP)
        }
    }

    fun createOtp(otpElement: OtpElement) {
        viewModelScope.launch {
            _entryEditEvents.emit(EntryEditEvent.OnOtpCreated(otpElement))
        }
    }

    fun updateFieldProtection(fieldProtection: FieldProtection, isRevealed: Boolean) {
        fieldProtection.isRevealed = isRevealed
        viewModelScope.launch {
            _entryEditEvents.emit(EntryEditEvent.OnFieldProtectionUpdated(fieldProtection))
        }
    }

    fun askToClose(closeType: CloseType) {
        viewModelScope.launch {
            _entryEditEvents.emit(EntryEditEvent.RetrieveEntryInfoForClosing(closeType))
        }
    }

    fun askToCloseEntry(currentEntryInfo: EntryInfo?, closeType: CloseType) {
        if (backPressedAlreadyApproved.not()) {
            if (mInitialEntryInfo != currentEntryInfo) {
                viewModelScope.launch {
                    _entryEditEvents.emit(EntryEditEvent.AskToDiscardChanges(closeType))
                }
            } else {
                viewModelScope.launch {
                    _entryEditEvents.emit(EntryEditEvent.CloseEntry(closeType))
                }
            }
        } else {
            viewModelScope.launch {
                _entryEditEvents.emit(EntryEditEvent.CloseEntry(closeType))
            }
        }
    }

    fun approveDiscardChanges(closeType: CloseType) {
        backPressedAlreadyApproved = true
        viewModelScope.launch {
            _entryEditEvents.emit(EntryEditEvent.CloseEntry(closeType))
        }
    }

    data class EntryEditState(
        val loaded: Boolean = false,
        val entryInfo: EntryInfo? = null,
        val templates: Templates? = null
    )
    data class Templates(
        val templates: List<Template>,
        val defaultTemplate: Template,
    )

    enum class CloseType {
        DATABASE_BACK_PRESSED,
        CANCEL_SPECIAL_MODE
    }

    sealed class EntryEditEvent {
        data class EntryLoaded(
            val entryInfo: EntryInfo,
        ) : EntryEditEvent()

        data class OnTemplateChanged(
            val template: Template,
        ) : EntryEditEvent()

        object RequestPasswordSelection : EntryEditEvent()

        data class OnPasswordSelected(
            val field: Field
        ) : EntryEditEvent()

        data class RequestCustomFieldEdition(
            val field: Field
        ) : EntryEditEvent()

        data class OnCustomFieldEdited(
            val oldField: Field?,
            val newField: Field?
        ) : EntryEditEvent()

        data class ScrollTo(
            val viewPosition: Int
        ) : EntryEditEvent()

        object OnCustomFieldError : EntryEditEvent()

        object RequestSetupOTP : EntryEditEvent()

        data class OnOtpCreated(
            val otpElement: OtpElement
        ) : EntryEditEvent()

        data class CreateEntry(
            val parentId: GroupId,
            val newEntry: EntryInfo
        ) : EntryEditEvent()

        data class UpdateEntry(
            val entry: EntryInfo
        ) : EntryEditEvent()

        data class CloseEntry(
            val closeType: CloseType
        ) : EntryEditEvent()

        data class AskToDiscardChanges(
            val closeType: CloseType
        ) : EntryEditEvent()

        object ShowOverwriteMessage : EntryEditEvent()

        data class OnChangeFieldProtectionRequested(
            val fieldProtection: FieldProtection
        ) : EntryEditEvent()

        data class OnFieldProtectionUpdated(
            val fieldProtection: FieldProtection
        ) : EntryEditEvent()

        data class RetrieveEntryInfoForClosing(
            val closeType: CloseType
        ) : EntryEditEvent()
    }

    companion object {
        private val TAG = EntryEditViewModel::class.java.name
    }
}