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
import com.kunzisoft.keepass.model.AttachmentState
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.FieldProtection
import com.kunzisoft.keepass.model.RegisterInfo
import com.kunzisoft.keepass.model.StreamDirection
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.utils.IOActionTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val mTempAttachments = mutableListOf<EntryAttachmentState>()

    var passwordField: Field? = null

    // To show dialog only one time
    var backPressedAlreadyApproved = false

    // Useful to not relaunch a current action
    private var actionLocked: Boolean = false

    val templatesEntry : LiveData<TemplatesEntry?> get() = _templatesEntry
    private val _templatesEntry = MutableLiveData<TemplatesEntry?>()

    val requestEntryInfoUpdate : LiveData<EntryUpdate> get() = _requestEntryInfoUpdate
    private val _requestEntryInfoUpdate = SingleLiveEvent<EntryUpdate>()
    val onEntrySaved : LiveData<EntrySave> get() = _onEntrySaved
    private val _onEntrySaved = SingleLiveEvent<EntrySave>()

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
    val entryEditState: StateFlow<EntryEditState> = mEntryEditState

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
        val entryTemplate = entry?.let { database.getTemplate(it) }
                ?: Template.STANDARD
        var entryInfo: EntryInfo? = null
        var overwrittenData = false
        // Decode the entry / load entry info
        entry?.let {
            database.decodeEntryWithTemplateConfiguration(it).let { entry ->
                // Load entry info
                entry.getEntryInfo(database, true).let { tempEntryInfo ->
                    // Retrieve data from registration
                    registerInfo?.let { regInfo ->
                        overwrittenData = tempEntryInfo.saveRegisterInfo(database, regInfo)
                    }
                    entryInfo = tempEntryInfo
                }
            }
        }
        return TemplatesEntry(
            template = onTemplateChanged.value,
            templates = templates,
            defaultTemplate = entryTemplate,
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
        _requestEntryInfoUpdate.value = EntryUpdate
    }

    fun unlockAction() {
        actionLocked = false
    }

    fun saveEntryInfo(entryInfo: EntryInfo) {
        if (actionLocked.not()) {
            actionLocked = true
            IOActionTask(
                scope = viewModelScope,
                action = {
                    removeTempAttachmentsNotCompleted(entryInfo)
                    mDatabase?.let { database ->
                        mEntry?.let { oldEntry ->
                            // Create a clone
                            var newEntry = Entry(oldEntry)

                            // Build info
                            newEntry.setEntryInfo(database, entryInfo)

                            // Encode entry properties for template
                            _onTemplateChanged.value?.let { template ->
                                newEntry = database.encodeEntryWithTemplateConfiguration(
                                        newEntry,
                                        template
                                    )
                            }

                            // Delete temp attachment if not used
                            mTempAttachments.forEach { tempAttachmentState ->
                                val tempAttachment = tempAttachmentState.attachment
                                database.attachmentPool.let { binaryPool ->
                                    if (!newEntry.getAttachments(binaryPool)
                                            .contains(tempAttachment)
                                    ) {
                                        database.removeAttachmentIfNotUsed(tempAttachment)
                                    }
                                }
                            }

                            // Return entry to save
                            EntrySave(oldEntry, newEntry, mParent)
                        }
                    }
                },
                onActionComplete = { entrySave ->
                    entrySave?.let {
                        _onEntrySaved.value = it
                    }
                }
            ).execute()
        }
    }

    private fun removeTempAttachmentsNotCompleted(entryInfo: EntryInfo) {
        // Do not save entry in upload progression
        mTempAttachments.forEach { attachmentState ->
            if (attachmentState.streamDirection == StreamDirection.UPLOAD) {
                when (attachmentState.downloadState) {
                    AttachmentState.START,
                    AttachmentState.IN_PROGRESS,
                    AttachmentState.CANCELED,
                    AttachmentState.ERROR -> {
                        // Remove attachment not finished from info
                        entryInfo.attachments = entryInfo.attachments.toMutableList().apply {
                            remove(attachmentState.attachment)
                        }
                    }
                    else -> {
                    }
                }
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
        // Add in temp list
        if (mTempAttachments.contains(entryAttachmentState)) {
            mTempAttachments.remove(entryAttachmentState)
        }
        if (entryAttachmentState != null) {
            mTempAttachments.add(entryAttachmentState)
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

    data class TemplatesEntry(
        val template: Template?,
        val templates: List<Template>,
        val defaultTemplate: Template,
        val entryInfo: EntryInfo?,
        val overwrittenData: Boolean = false
    )
    object EntryUpdate
    data class EntrySave(val oldEntry: Entry, val newEntry: Entry, val parent: Group?)
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
    }

    companion object {
        private val TAG = EntryEditViewModel::class.java.name
    }
}