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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.template.Template
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.FieldProtection
import com.kunzisoft.keepass.model.RegisterInfo
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.utils.IOActionTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID


class EntryEditViewModel: NodeEditViewModel() {

    private var mDatabase: ContextualDatabase? = null
    private var mEntryId: NodeId<UUID>? = null
    private var mParentId: NodeId<*>? = null
    private var mRegisterInfo: RegisterInfo? = null
    private var mParent: Group? = null
    private var mEntry: Entry? = null
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

    val templatesEntry : LiveData<TemplatesEntry?> get() = _templatesEntry
    private val _templatesEntry = MutableLiveData<TemplatesEntry?>()

    val requestEntryInfoUpdate : LiveData<Void?> get() = _requestEntryInfoUpdate
    private val _requestEntryInfoUpdate = SingleLiveEvent<Void?>()

    val onTemplateChanged : LiveData<Template> get() = _onTemplateChanged
    private val _onTemplateChanged = MutableLiveData<Template>()

    val requestPasswordSelection : LiveData<Void?> get() = _requestPasswordSelection
    private val _requestPasswordSelection = SingleLiveEvent<Void?>()
    val onPasswordSelected : LiveData<Field> get() = _onPasswordSelected
    private val _onPasswordSelected = SingleLiveEvent<Field>()

    val requestCustomFieldEdition : LiveData<Field> get() = _requestCustomFieldEdition
    private val _requestCustomFieldEdition = SingleLiveEvent<Field>()
    val onCustomFieldEdited : LiveData<FieldEdition> get() = _onCustomFieldEdited
    private val _onCustomFieldEdited = SingleLiveEvent<FieldEdition>()
    val onCustomFieldError : LiveData<Void?> get() = _onCustomFieldError
    private val _onCustomFieldError = SingleLiveEvent<Void?>()

    val requestSetupOtp : LiveData<Void?> get() = _requestSetupOtp
    private val _requestSetupOtp = SingleLiveEvent<Void?>()
    val onOtpCreated : LiveData<OtpElement> get() = _onOtpCreated
    private val _onOtpCreated = SingleLiveEvent<OtpElement>()

    val onBuildNewAttachment : LiveData<AttachmentBuild> get() = _onBuildNewAttachment
    private val _onBuildNewAttachment = SingleLiveEvent<AttachmentBuild>()
    val onStartUploadAttachment : LiveData<AttachmentUpload> get() = _onStartUploadAttachment
    private val _onStartUploadAttachment = SingleLiveEvent<AttachmentUpload>()
    val attachmentDeleted : LiveData<Attachment> get() = _attachmentDeleted
    private val _attachmentDeleted = SingleLiveEvent<Attachment>()
    val onAttachmentAction : LiveData<EntryAttachmentState?> get() = _onAttachmentAction
    private val _onAttachmentAction = MutableLiveData<EntryAttachmentState?>()
    val onBinaryPreviewLoaded : LiveData<AttachmentPosition> get() = _onBinaryPreviewLoaded
    private val _onBinaryPreviewLoaded = SingleLiveEvent<AttachmentPosition>()

    private val mEntryEditState = MutableStateFlow<EntryEditState>(EntryEditState.Loading)
    val entryEditState: StateFlow<EntryEditState> = mEntryEditState.asStateFlow()

    val entryLoaded: Boolean
        get() = templatesEntry.value != null

    fun loadTemplateEntry(database: ContextualDatabase) {
        mDatabase = database
        allowCustomFields = database.allowEntryCustomFields() == true
        allowOTP = database.allowOTP == true
        loadTemplateEntry(database, mEntryId, mParentId, mRegisterInfo)
    }

    fun loadTemplateEntry(
        database: ContextualDatabase?,
        entryId: NodeId<UUID>?,
        parentId: NodeId<*>?,
        registerInfo: RegisterInfo?
    ) {
        this.mEntryId = entryId
        this.mParentId = parentId
        this.mRegisterInfo = registerInfo

        // TODO Move in Database
        database?.let {
            mEntryId?.let {
                IOActionTask(
                    scope = viewModelScope,
                    action = {
                        // Create an Entry copy to modify from the database entry
                        mEntry = database.getEntryById(it)
                        // Retrieve the parent
                        mEntry?.let { entry ->
                            // If no parent, add root group as parent
                            if (entry.parent == null) {
                                entry.parent = database.rootGroup
                            }
                            // Define if current entry is a template (in direct template group)
                            isTemplate = database.entryIsTemplate(mEntry)
                            decodeTemplateEntry(
                                database,
                                entry,
                                isTemplate,
                                registerInfo
                            )
                        }
                    },
                    onActionComplete = { templatesEntry ->
                        mEntryId = null
                        mInitialEntryInfo = templatesEntry?.entryInfo?.let { entryInfo ->
                            EntryInfo(entryInfo)
                        }
                        _templatesEntry.value = templatesEntry
                        if (templatesEntry?.overwrittenData == true) {
                            mEntryEditState.value = EntryEditState.ShowOverwriteMessage
                        }
                    }
                ).execute()
            }

            mParentId?.let {
                IOActionTask(
                    scope = viewModelScope,
                    action = {
                        mParent = database.getGroupById(it)
                        mParent?.let { parentGroup ->
                            mEntry = database.createEntry()?.apply {
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
                                // Warning only the entry recognize is parent, parent don't yet recognize the new entry
                                // Useful to recognize child state (ie: entry is a template)
                                parent = parentGroup
                            }
                            isTemplate = database.entryIsTemplate(mEntry)
                            decodeTemplateEntry(
                                database,
                                mEntry,
                                isTemplate,
                                registerInfo
                            )
                        }
                    },
                    onActionComplete = { templatesEntry ->
                        mParentId = null
                        mInitialEntryInfo = templatesEntry?.entryInfo?.let { entryInfo ->
                            EntryInfo(entryInfo)
                        }
                        _templatesEntry.value = templatesEntry
                    }
                ).execute()
            }
        }
    }

    private fun decodeTemplateEntry(
        database: ContextualDatabase,
        entry: Entry?,
        isTemplate: Boolean,
        registerInfo: RegisterInfo?
    ): TemplatesEntry {
        val templates = database.getTemplates(isTemplate)
        var entryInfo: EntryInfo? = null
        var overwrittenData = false
        // Decode the entry / load entry info
        entry?.let {
            database.getEntryInfoFrom(entry = it, raw = true).let { tempEntryInfo ->
                // Retrieve data from registration
                registerInfo?.let { regInfo ->
                    overwrittenData = tempEntryInfo.saveRegisterInfo(database, regInfo)
                }
                entryInfo = tempEntryInfo
            }
        }
        return TemplatesEntry(
            template = onTemplateChanged.value,
            templates = templates,
            defaultTemplate = entryInfo?.template ?: Template.STANDARD,
            entryInfo = entryInfo,
            overwrittenData = overwrittenData
        )
    }

    fun changeTemplate(template: Template) {
        if (_onTemplateChanged.value != template) {
            _onTemplateChanged.value = template
        }
    }

    fun requestEntryInfoUpdate() {
        _requestEntryInfoUpdate.call()
    }

    fun unlockAction() {
        actionLocked = false
    }

    fun saveEntryInfo(entryInfo: EntryInfo) {
        if (actionLocked.not()) {
            actionLocked = true
            // Delete temp attachment if not completely downloaded
            mDatabase?.removeTempAttachmentsNotCompleted(entryInfo)
            mParent?.nodeId?.let { parentId ->
                mEntryEditState.value = EntryEditState.CreateEntry(
                    parentId = parentId,
                    newEntry = entryInfo
                )
            } ?: mEntry?.nodeId?.let { oldEntryId ->
                mEntryEditState.value = EntryEditState.UpdateEntry(
                    oldEntryId = oldEntryId,
                    updateEntry = entryInfo
                )
            }
        }
    }

    fun requestPasswordSelection(passwordField: Field) {
        this.passwordField = passwordField
        _requestPasswordSelection.call()
    }

    fun selectPassword(passwordField: Field) {
        _onPasswordSelected.value = passwordField
    }

    fun requestChangeFieldProtection(fieldProtection: FieldProtection) {
        mEntryEditState.value = EntryEditState.OnChangeFieldProtectionRequested(fieldProtection)
    }

    fun requestCustomFieldEdition(customField: Field) {
        _requestCustomFieldEdition.value = customField
    }

    fun addCustomField(newField: Field) {
        _onCustomFieldEdited.value = FieldEdition(null, newField)
    }

    fun editCustomField(oldField: Field, newField: Field) {
        _onCustomFieldEdited.value = FieldEdition(oldField, newField)
    }

    fun removeCustomField(oldField: Field) {
        _onCustomFieldEdited.value = FieldEdition(oldField, null)
    }

    fun showCustomFieldEditionError() {
        _onCustomFieldError.call()
    }

    fun setupOtp() {
        _requestSetupOtp.call()
    }

    fun createOtp(otpElement: OtpElement) {
        _onOtpCreated.value = otpElement
    }

    fun buildNewAttachment(attachmentToUploadUri: Uri, fileName: String) {
        _onBuildNewAttachment.value = AttachmentBuild(attachmentToUploadUri, fileName)
    }

    fun startUploadAttachment(attachmentToUploadUri: Uri, attachment: Attachment) {
        _onStartUploadAttachment.value = AttachmentUpload(attachmentToUploadUri, attachment)
    }

    fun deleteAttachment(attachment: Attachment) {
        _attachmentDeleted.value = attachment
    }

    fun onAttachmentAction(entryAttachmentState: EntryAttachmentState?) {
        entryAttachmentState?.let {
            mDatabase?.addTempAttachment(entryAttachmentState)
        }
        _onAttachmentAction.value = entryAttachmentState
    }

    fun binaryPreviewLoaded(entryAttachmentState: EntryAttachmentState, viewPosition: Float) {
        _onBinaryPreviewLoaded.value = AttachmentPosition(entryAttachmentState, viewPosition)
    }

    fun updateFieldProtection(fieldProtection: FieldProtection, value: Boolean) {
        fieldProtection.isCurrentlyProtected = value
        mEntryEditState.value = EntryEditState.OnFieldProtectionUpdated(fieldProtection)
    }

    fun actionPerformed() {
        mEntryEditState.value = EntryEditState.Loading
    }

    fun askToClose(closeType: CloseType) {
        mEntryEditState.value = EntryEditState.RetrieveEntryInfoForClosing(closeType)
    }

    fun askToCloseEntry(currentEntryInfo: EntryInfo?, closeType: CloseType) {
        if (backPressedAlreadyApproved.not()) {
            if (mInitialEntryInfo != currentEntryInfo) {
                mEntryEditState.value = EntryEditState.AskToDiscardChanges(closeType)
            } else {
                mEntryEditState.value = EntryEditState.CloseEntry(closeType)
            }
        } else {
            mEntryEditState.value = EntryEditState.CloseEntry(closeType)
        }
    }

    fun approveDiscardChanges(closeType: CloseType) {
        backPressedAlreadyApproved = true
        mEntryEditState.value = EntryEditState.CloseEntry(closeType)
    }

    data class TemplatesEntry(
        val template: Template?,
        val templates: List<Template>,
        val defaultTemplate: Template,
        val entryInfo: EntryInfo?,
        val overwrittenData: Boolean = false
    )
    data class FieldEdition(val oldField: Field?, val newField: Field?)
    data class AttachmentBuild(val attachmentToUploadUri: Uri, val fileName: String)
    data class AttachmentUpload(val attachmentToUploadUri: Uri, val attachment: Attachment)
    data class AttachmentPosition(val entryAttachmentState: EntryAttachmentState, val viewPosition: Float)

    sealed class EntryEditState {
        object Loading: EntryEditState()
        object ShowOverwriteMessage: EntryEditState()
        data class OnChangeFieldProtectionRequested(
            val fieldProtection: FieldProtection
        ): EntryEditState()
        data class OnFieldProtectionUpdated(
            val fieldProtection: FieldProtection
        ): EntryEditState()
        data class CreateEntry(
            val parentId: NodeId<*>, val newEntry: EntryInfo
        ): EntryEditState()
        data class UpdateEntry(
            val oldEntryId: NodeId<UUID>, val updateEntry: EntryInfo
        ): EntryEditState()
        data class CloseEntry(
            val closeType: CloseType
        ): EntryEditState()
        data class RetrieveEntryInfoForClosing(
            val closeType: CloseType
        ): EntryEditState()
        data class AskToDiscardChanges(
            val closeType: CloseType
        ): EntryEditState()
    }

    enum class CloseType {
        DATABASE_BACK_PRESSED,
        CANCEL_SPECIAL_MODE
    }

    companion object {
        private val TAG = EntryEditViewModel::class.java.name
    }
}