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

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.template.Template
import com.kunzisoft.keepass.model.EntryAttachmentState
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
import java.util.UUID


/**
 * ViewModel for editing an entry.
 */
class EntryEditViewModel: NodeEditViewModel() {

    private var mDatabase: ContextualDatabase? = null
    private var mEntryId: NodeId<UUID>? = null
    private var mParentId: NodeId<*>? = null
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

    private val _onEntryLoaded = MutableSharedFlow<EntryInfo>(replay = 0)
    val onEntryLoaded: SharedFlow<EntryInfo> = _onEntryLoaded.asSharedFlow()

    private val _onTemplatesInitialized = MutableSharedFlow<Templates>(replay = 0)
    val onTemplatesInitialized: SharedFlow<Templates> = _onTemplatesInitialized.asSharedFlow()

    private val _onTemplateChanged = MutableSharedFlow<Template>(replay = 0)
    val onTemplateChanged: SharedFlow<Template> = _onTemplateChanged.asSharedFlow()

    private val _onEntryValidationRequested = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val onEntryValidationRequested: SharedFlow<Unit> = _onEntryValidationRequested.asSharedFlow()

    private val _requestPasswordSelection = MutableSharedFlow<Unit>(replay = 0)
    val requestPasswordSelection: SharedFlow<Unit> = _requestPasswordSelection.asSharedFlow()

    private val _onPasswordSelected = MutableSharedFlow<Field>(replay = 0)
    val onPasswordSelected: SharedFlow<Field> = _onPasswordSelected.asSharedFlow()

    private val _requestCustomFieldEdition = MutableSharedFlow<Field>(replay = 0)
    val requestCustomFieldEdition: SharedFlow<Field> = _requestCustomFieldEdition.asSharedFlow()

    private val _onCustomFieldEdited = MutableSharedFlow<FieldEdition>(replay = 0)
    val onCustomFieldEdited: SharedFlow<FieldEdition> = _onCustomFieldEdited.asSharedFlow()

    private val _onCustomFieldError = MutableSharedFlow<Unit>(replay = 0)
    val onCustomFieldError: SharedFlow<Unit> = _onCustomFieldError.asSharedFlow()

    private val _requestSetupOtp = MutableSharedFlow<Unit>(replay = 0)
    val requestSetupOtp: SharedFlow<Unit> = _requestSetupOtp.asSharedFlow()

    private val _onOtpCreated = MutableSharedFlow<OtpElement>(replay = 0)
    val onOtpCreated: SharedFlow<OtpElement> = _onOtpCreated.asSharedFlow()

    private val _onBuildNewAttachment = MutableSharedFlow<AttachmentBuild>(replay = 0)
    val onBuildNewAttachment: SharedFlow<AttachmentBuild> = _onBuildNewAttachment.asSharedFlow()

    private val _onStartUploadAttachment = MutableSharedFlow<AttachmentUpload>(replay = 0)
    val onStartUploadAttachment: SharedFlow<AttachmentUpload> = _onStartUploadAttachment.asSharedFlow()

    private val _attachmentDeleted = MutableSharedFlow<Attachment>(replay = 0)
    val attachmentDeleted: SharedFlow<Attachment> = _attachmentDeleted.asSharedFlow()

    private val _onAttachmentAction = MutableStateFlow<EntryAttachmentState?>(null)
    val onAttachmentAction: StateFlow<EntryAttachmentState?> = _onAttachmentAction.asStateFlow()

    private val _onBinaryPreviewLoaded = MutableSharedFlow<AttachmentPosition>(replay = 0)
    val onBinaryPreviewLoaded: SharedFlow<AttachmentPosition> = _onBinaryPreviewLoaded.asSharedFlow()

    private val _createEntry = MutableSharedFlow<CreateEntryAction>(replay = 0)
    val createEntry: SharedFlow<CreateEntryAction> = _createEntry.asSharedFlow()

    private val _updateEntry = MutableSharedFlow<EntryInfo>(replay = 0)
    val updateEntry: SharedFlow<EntryInfo> = _updateEntry.asSharedFlow()

    private val _closeEntry = MutableSharedFlow<CloseType>(replay = 0)
    val closeEntry: SharedFlow<CloseType> = _closeEntry.asSharedFlow()

    private val _askToDiscardChanges = MutableSharedFlow<CloseType>(replay = 0)
    val askToDiscardChanges: SharedFlow<CloseType> = _askToDiscardChanges.asSharedFlow()

    private val _showOverwriteMessage = MutableSharedFlow<Unit>(replay = 0)
    val showOverwriteMessage: SharedFlow<Unit> = _showOverwriteMessage.asSharedFlow()

    private val _onChangeFieldProtectionRequested = MutableSharedFlow<FieldProtection>(replay = 0)
    val onChangeFieldProtectionRequested: SharedFlow<FieldProtection> = _onChangeFieldProtectionRequested.asSharedFlow()

    private val _onFieldProtectionUpdated = MutableSharedFlow<FieldProtection>(replay = 0)
    val onFieldProtectionUpdated: SharedFlow<FieldProtection> = _onFieldProtectionUpdated.asSharedFlow()

    private val _retrieveEntryInfoForClosing = MutableSharedFlow<CloseType>(replay = 0)
    val retrieveEntryInfoForClosing: SharedFlow<CloseType> = _retrieveEntryInfoForClosing.asSharedFlow()

    val entryLoaded: Boolean
        get() = entryEditUIState.value.loaded

    fun loadTemplateEntry(database: ContextualDatabase) {
        mDatabase = database
        allowCustomFields = database.allowEntryCustomFields() == true
        allowOTP = database.allowOTP == true
        loadTemplateEntry(mEntryId, mParentId, mRegisterInfo)
    }

    fun loadTemplateEntry(
        entryId: NodeId<UUID>?,
        parentId: NodeId<*>?,
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
            mTemplates?.let {
                _onTemplatesInitialized.emit(it)
            }
            withContext(Dispatchers.IO) {
                mDatabase?.let { database ->
                    // Update the entry
                    mEntryId?.let {
                        // Create an Entry copy to modify from the database entry
                        val entry = database.getEntryById(it)
                        // Retrieve the parent
                        entry?.let { entry ->
                            // If no parent, add root group as parent
                            if (entry.parent == null) {
                                entry.parent = database.rootGroup
                            }
                            // Define if current entry is a template (in direct template group)
                            isTemplate = database.entryIsTemplate(entry)
                            var entryInfo: EntryInfo? = null
                            // Decode the entry / load entry info
                            database.getEntryInfoFrom(entry = entry, raw = true).let { tempEntryInfo ->
                                // Retrieve data from registration
                                registerInfo?.let { regInfo ->
                                    if(tempEntryInfo.saveRegisterInfo(database, regInfo))
                                        _showOverwriteMessage.emit(Unit)
                                }
                                entryInfo = tempEntryInfo
                            }
                            loadEntryInfo(database, entryInfo)
                        }
                    }
                    // Create the entry
                    mParentId?.let {
                        val parent = database.getGroupById(it)
                        parent?.let { parentGroup ->
                            val entry = database.createEntry()?.apply {
                                // Add the default icon from parent if not a folder
                                val parentIcon = parentGroup.icon
                                // Set default icon
                                if (parentIcon.custom.isUnknown
                                    && parentIcon.standard.id != IconImageStandard.FOLDER_ID
                                ) {
                                    icon = IconImage(parentIcon.standard)
                                }
                                if (!parentIcon.custom.isUnknown) {
                                    icon = IconImage(parentIcon.custom)
                                }
                                // Set default username
                                username = database.defaultUsername
                            }
                            isTemplate = database.entryIsTemplate(entry)

                            var entryInfo: EntryInfo? = null
                            // Decode the entry / load entry info
                            entry?.let {
                                database.getEntryInfoFrom(entry = entry, raw = true).let { tempEntryInfo ->
                                    // Retrieve data from registration
                                    registerInfo?.let { regInfo ->
                                        tempEntryInfo.saveRegisterInfo(database, regInfo)
                                    }
                                    entryInfo = tempEntryInfo
                                }
                            }
                            loadEntryInfo(database, entryInfo)
                        }
                    }
                }
            }
        }
    }

    private suspend fun loadEntryInfo(database: Database, entryInfo: EntryInfo?) {
        mInitialEntryInfo = entryInfo?.let { entryInfo ->
            EntryInfo(entryInfo)
        }
        entryInfo?.let {
            _onEntryLoaded.emit(entryInfo)
        }
        val selectedTemplate = entryInfo?.template ?: Template.STANDARD
        val templates = Templates(
            templates = database.getTemplates(isTemplate),
            defaultTemplate = selectedTemplate
        )
        mTemplates = templates
        _onTemplatesInitialized.emit(templates)
        _onTemplateChanged.emit(selectedTemplate)
        _entryEditUIState.update { entryEdit ->
            entryEdit.copy(
                loaded = true,
                entryInfo = entryInfo
            )
        }
    }

    fun changeTemplate(template: Template) {
        viewModelScope.launch {
            _onTemplateChanged.emit(template)
        }
    }

    fun requestEntryValidation() {
        viewModelScope.launch {
            _onEntryValidationRequested.emit(Unit)
        }
    }

    fun unlockAction() {
        actionLocked = false
    }

    fun saveEntryInfo(entryInfo: EntryInfo) {
        if (actionLocked.not()) {
            actionLocked = true
            // Delete temp attachment if not completely downloaded
            mDatabase?.removeTempAttachmentsNotCompleted(entryInfo)
            viewModelScope.launch {
                mEntryId?.let {
                    _updateEntry.emit(entryInfo)
                } ?: mParentId?.let { parentId ->
                    _createEntry.emit(CreateEntryAction(
                        parentId = parentId,
                        newEntry = entryInfo
                    ))
                } ?: run {
                    actionLocked = false
                }
            }
        }
    }

    fun requestPasswordSelection(passwordField: Field) {
        this.passwordField = passwordField
        viewModelScope.launch {
            _requestPasswordSelection.emit(Unit)
        }
    }

    fun selectPassword(passwordField: Field) {
        viewModelScope.launch {
            _onPasswordSelected.emit(passwordField)
        }
    }

    fun requestChangeFieldProtection(fieldProtection: FieldProtection) {
        viewModelScope.launch {
            _onChangeFieldProtectionRequested.emit(fieldProtection)
        }
    }

    fun requestCustomFieldEdition(customField: Field) {
        viewModelScope.launch {
            _requestCustomFieldEdition.emit(customField)
        }
    }

    fun addCustomField(newField: Field) {
        viewModelScope.launch {
            _onCustomFieldEdited.emit(FieldEdition(null, newField))
        }
    }

    fun editCustomField(oldField: Field, newField: Field) {
        viewModelScope.launch {
            _onCustomFieldEdited.emit(FieldEdition(oldField, newField))
        }
    }

    fun removeCustomField(oldField: Field) {
        viewModelScope.launch {
            _onCustomFieldEdited.emit(FieldEdition(oldField, null))
        }
    }

    fun showCustomFieldEditionError() {
        viewModelScope.launch {
            _onCustomFieldError.emit(Unit)
        }
    }

    fun setupOtp() {
        viewModelScope.launch {
            _requestSetupOtp.emit(Unit)
        }
    }

    fun createOtp(otpElement: OtpElement) {
        viewModelScope.launch {
            _onOtpCreated.emit(otpElement)
        }
    }

    fun buildNewAttachment(attachmentToUploadUri: Uri, fileName: String) {
        viewModelScope.launch {
            _onBuildNewAttachment.emit(AttachmentBuild(attachmentToUploadUri, fileName))
        }
    }

    fun startUploadAttachment(attachmentToUploadUri: Uri, attachment: Attachment) {
        viewModelScope.launch {
            _onStartUploadAttachment.emit(AttachmentUpload(attachmentToUploadUri, attachment)
            )
        }
    }

    fun deleteAttachment(attachment: Attachment) {
        viewModelScope.launch {
            _attachmentDeleted.emit(attachment)
        }
    }

    fun onAttachmentAction(entryAttachmentState: EntryAttachmentState?) {
        entryAttachmentState?.let {
            mDatabase?.addTempAttachment(entryAttachmentState)
        }
        _onAttachmentAction.value = entryAttachmentState
    }

    fun binaryPreviewLoaded(entryAttachmentState: EntryAttachmentState, viewPosition: Float) {
        viewModelScope.launch {
            _onBinaryPreviewLoaded.emit(AttachmentPosition(entryAttachmentState, viewPosition))
        }
    }

    fun updateFieldProtection(fieldProtection: FieldProtection, value: Boolean) {
        fieldProtection.isCurrentlyProtected = value
        viewModelScope.launch {
            _onFieldProtectionUpdated.emit(fieldProtection)
        }
    }

    fun askToClose(closeType: CloseType) {
        viewModelScope.launch {
            _retrieveEntryInfoForClosing.emit(closeType)
        }
    }

    fun askToCloseEntry(currentEntryInfo: EntryInfo?, closeType: CloseType) {
        if (backPressedAlreadyApproved.not()) {
            if (mInitialEntryInfo != currentEntryInfo) {
                viewModelScope.launch {
                    _askToDiscardChanges.emit(closeType)
                }
            } else {
                viewModelScope.launch {
                    _closeEntry.emit(closeType)
                }
            }
        } else {
            viewModelScope.launch {
                _closeEntry.emit(closeType)
            }
        }
    }

    fun approveDiscardChanges(closeType: CloseType) {
        backPressedAlreadyApproved = true
        viewModelScope.launch {
            _closeEntry.emit(closeType)
        }
    }

    data class Templates(
        val templates: List<Template>,
        val defaultTemplate: Template,
    )
    data class FieldEdition(val oldField: Field?, val newField: Field?)
    data class AttachmentBuild(val attachmentToUploadUri: Uri, val fileName: String)
    data class AttachmentUpload(val attachmentToUploadUri: Uri, val attachment: Attachment)
    data class AttachmentPosition(val entryAttachmentState: EntryAttachmentState, val viewPosition: Float)

    data class EntryEditState(
        val loaded: Boolean = false,
        val entryInfo: EntryInfo? = null
    )
    data class CreateEntryAction(
        val parentId: NodeId<*>,
        val newEntry: EntryInfo
    )

    enum class CloseType {
        DATABASE_BACK_PRESSED,
        CANCEL_SPECIAL_MODE
    }

    companion object {
        private val TAG = EntryEditViewModel::class.java.name
    }
}