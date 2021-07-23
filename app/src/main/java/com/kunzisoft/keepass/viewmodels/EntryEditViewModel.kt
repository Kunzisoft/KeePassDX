package com.kunzisoft.keepass.viewmodels

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kunzisoft.keepass.app.database.IOActionTask
import com.kunzisoft.keepass.database.element.*
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.template.Template
import com.kunzisoft.keepass.model.*
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.view.DataDate
import com.kunzisoft.keepass.view.DataTime
import java.util.*


class EntryEditViewModel: ViewModel() {

    private val mDatabase: Database? = Database.getInstance()

    private var mParent : Group? = null
    private var mEntry : Entry? = null
    private var mIsTemplate: Boolean = false

    private val mTempAttachments = mutableListOf<EntryAttachmentState>()

    val entryInfo : LiveData<EntryInfo> get() = _entryInfo
    private val _entryInfo = MutableLiveData<EntryInfo>()

    val requestEntryInfoUpdate : LiveData<Void?> get() = _requestEntryInfoUpdate
    private val _requestEntryInfoUpdate = SingleLiveEvent<Void?>()
    val onEntrySaved : LiveData<EntrySave> get() = _onEntrySaved
    private val _onEntrySaved = SingleLiveEvent<EntrySave>()

    val templates : LiveData<TemplatesLoad> get() = _templates
    private val _templates = MutableLiveData<TemplatesLoad>()
    val onTemplateChanged : LiveData<Template> get() = _onTemplateChanged
    private val _onTemplateChanged = SingleLiveEvent<Template>()

    val requestIconSelection : LiveData<IconImage> get() = _requestIconSelection
    private val _requestIconSelection = SingleLiveEvent<IconImage>()
    val onIconSelected : LiveData<IconImage> get() = _onIconSelected
    private val _onIconSelected = SingleLiveEvent<IconImage>()

    val requestPasswordSelection : LiveData<Field> get() = _requestPasswordSelection
    private val _requestPasswordSelection = SingleLiveEvent<Field>()
    val onPasswordSelected : LiveData<Field> get() = _onPasswordSelected
    private val _onPasswordSelected = SingleLiveEvent<Field>()

    val requestCustomFieldEdition : LiveData<Field> get() = _requestCustomFieldEdition
    private val _requestCustomFieldEdition = SingleLiveEvent<Field>()
    val onCustomFieldEdited : LiveData<FieldEdition> get() = _onCustomFieldEdited
    private val _onCustomFieldEdited = SingleLiveEvent<FieldEdition>()
    val onCustomFieldError : LiveData<Void?> get() = _onCustomFieldError
    private val _onCustomFieldError = SingleLiveEvent<Void?>()

    val requestDateTimeSelection : LiveData<DateInstant> get() = _requestDateTimeSelection
    private val _requestDateTimeSelection = SingleLiveEvent<DateInstant>()
    val onDateSelected : LiveData<DataDate> get() = _onDateSelected
    private val _onDateSelected = SingleLiveEvent<DataDate>()
    val onTimeSelected : LiveData<DataTime> get() = _onTimeSelected
    private val _onTimeSelected = SingleLiveEvent<DataTime>()

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

    fun initializeEntryToUpdate(entryId: NodeId<UUID>,
                                registerInfo: RegisterInfo?,
                                searchInfo: SearchInfo?) {
        IOActionTask(
            {
                mEntry = getEntry(entryId)
                mIsTemplate = isEntryATemplate()
            },
            {
                loadTemplateEntry(registerInfo, searchInfo)
            }
        ).execute()
    }

    fun initializeEntryToCreate(parentId: NodeId<*>,
                                registerInfo: RegisterInfo?,
                                searchInfo: SearchInfo?) {
        IOActionTask(
            {
                mParent = getGroup(parentId)
                mEntry = createEntry(mParent)
                mIsTemplate = isEntryATemplate()
            },
            {
                loadTemplateEntry(registerInfo, searchInfo)
            }
        ).execute()
    }

    private fun loadTemplateEntry(registerInfo: RegisterInfo?,
                                  searchInfo: SearchInfo?) {
        IOActionTask(
            {
                val templates = getTemplates()
                val entryTemplate = getEntryTemplate()
                TemplatesLoad(templates, entryTemplate)
            },
            { templatesLoad ->
                // Call templates init before populate entry info
                _templates.value = templatesLoad
                changeTemplate(templatesLoad!!.defaultTemplate)

                IOActionTask(
                    {
                        loadEntryInfo(registerInfo, searchInfo)
                    },
                    { entryInfo ->
                        _entryInfo.value = entryInfo
                    }
                ).execute()
            }
        ).execute()
    }

    private fun getEntry(entryId: NodeId<UUID>): Entry? {
        // Create an Entry copy to modify from the database entry
        val tempEntry = mDatabase?.getEntryById(entryId)
        // Retrieve the parent
        tempEntry?.let { entry ->
            // If no parent, add root group as parent
            if (entry.parent == null) {
                entry.parent = mDatabase?.rootGroup
            }
        }
        return tempEntry
    }

    private fun getGroup(groupId: NodeId<*>): Group? {
        return mDatabase?.getGroupById(groupId)
    }

    private fun createEntry(parentGroup: Group?): Entry? {
        return mDatabase?.createEntry()?.apply {
            // Add the default icon from parent if not a folder
            val parentIcon = parentGroup?.icon
            // Set default icon
            if (parentIcon != null) {
                if (parentIcon.custom.isUnknown
                    && parentIcon.standard.id != IconImageStandard.FOLDER_ID) {
                    icon = IconImage(parentIcon.standard)
                }
                if (!parentIcon.custom.isUnknown) {
                    icon = IconImage(parentIcon.custom)
                }
            }
            // Set default username
            username = mDatabase.defaultUsername
            // Warning only the entry recognize is parent, parent don't yet recognize the new entry
            // Useful to recognize child state (ie: entry is a template)
            parent = parentGroup
        }
    }

    private fun isEntryATemplate(): Boolean {
        // Define is current entry is a template (in direct template group)
        return mDatabase?.entryIsTemplate(mEntry) ?: false
    }

    private fun getTemplates(): List<Template> {
        return mDatabase?.getTemplates(mIsTemplate) ?: listOf()
    }

    private fun getEntryTemplate(): Template {
        return mEntry?.let { mDatabase?.getTemplate(it) } ?: Template.STANDARD
    }

    private fun loadEntryInfo(registerInfo: RegisterInfo?, searchInfo: SearchInfo?): EntryInfo? {
        // Decode the entry
        mEntry?.let {
            mDatabase?.decodeEntryWithTemplateConfiguration(it)?.let { entry ->
                // Load entry info
                entry.getEntryInfo(mDatabase, true).let { tempEntryInfo ->
                    // Retrieve data from registration
                    (registerInfo?.searchInfo ?: searchInfo)?.let { tempSearchInfo ->
                        tempEntryInfo.saveSearchInfo(mDatabase, tempSearchInfo)
                    }
                    registerInfo?.let { regInfo ->
                        tempEntryInfo.saveRegisterInfo(mDatabase, regInfo)
                    }

                    internalUpdateEntryInfo(tempEntryInfo)
                    return tempEntryInfo
                }
            }
        }
        return null
    }

    fun changeTemplate(template: Template) {
        if (_onTemplateChanged.value != template) {
            _onTemplateChanged.value = template
        }
    }

    // TODO Move
    fun entryIsTemplate(): Boolean {
        return mIsTemplate
    }

    fun requestEntryInfoUpdate() {
        _requestEntryInfoUpdate.call()
    }

    fun saveEntryInfo(entryInfo: EntryInfo) {
        IOActionTask(
            {
                internalUpdateEntryInfo(entryInfo)
                mEntry?.let { oldEntry ->
                    // Create a clone
                    var newEntry = Entry(oldEntry)

                    // Build info
                    newEntry.setEntryInfo(mDatabase, entryInfo)

                    // Encode entry properties for template
                    _onTemplateChanged.value?.let { template ->
                        newEntry =
                            mDatabase?.encodeEntryWithTemplateConfiguration(newEntry, template)
                                ?: newEntry
                    }

                    // Delete temp attachment if not used
                    mTempAttachments.forEach { tempAttachmentState ->
                        val tempAttachment = tempAttachmentState.attachment
                        mDatabase?.attachmentPool?.let { binaryPool ->
                            if (!newEntry.getAttachments(binaryPool).contains(tempAttachment)) {
                                mDatabase.removeAttachmentIfNotUsed(tempAttachment)
                            }
                        }
                    }

                    // Return entry to save
                    EntrySave(oldEntry, newEntry, mParent)
                }
            },
            { entrySave ->
                entrySave?.let {
                    _onEntrySaved.value = it
                }
            }
        ).execute()
    }

    private fun internalUpdateEntryInfo(entryInfo: EntryInfo) {
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

    fun requestIconSelection(oldIconImage: IconImage) {
        _requestIconSelection.value = oldIconImage
    }

    fun selectIcon(iconImage: IconImage) {
        _onIconSelected.value = iconImage
    }

    fun requestPasswordSelection(passwordField: Field) {
        _requestPasswordSelection.value = passwordField
    }

    fun selectPassword(passwordField: Field) {
        _onPasswordSelected.value = passwordField
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

    fun requestDateTimeSelection(dateInstant: DateInstant) {
        _requestDateTimeSelection.value = dateInstant
    }

    fun selectDate(year: Int, month: Int, day: Int) {
        _onDateSelected.value = DataDate(year, month, day)
    }

    fun selectTime(hours: Int, minutes: Int) {
        _onTimeSelected.value = DataTime(hours, minutes)
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
        if (entryAttachmentState?.downloadState == AttachmentState.START) {
            // Add in temp list
            mTempAttachments.add(entryAttachmentState)
        }
        _onAttachmentAction.value = entryAttachmentState
    }

    fun binaryPreviewLoaded(entryAttachmentState: EntryAttachmentState, viewPosition: Float) {
        _onBinaryPreviewLoaded.value = AttachmentPosition(entryAttachmentState, viewPosition)
    }

    data class TemplatesLoad(val templates: List<Template>, val defaultTemplate: Template)
    data class EntrySave(val oldEntry: Entry, val newEntry: Entry, val parent: Group?)
    data class FieldEdition(val oldField: Field?, val newField: Field?)
    data class AttachmentBuild(val attachmentToUploadUri: Uri, val fileName: String)
    data class AttachmentUpload(val attachmentToUploadUri: Uri, val attachment: Attachment)
    data class AttachmentPosition(val entryAttachmentState: EntryAttachmentState, val viewPosition: Float)

    companion object {
        private val TAG = EntryEditViewModel::class.java.name
    }
}