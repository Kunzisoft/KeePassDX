package com.kunzisoft.keepass.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.template.Template
import com.kunzisoft.keepass.model.AttachmentState
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.StreamDirection


class EntryEditViewModel: ViewModel() {

    val requestEntryInfoUpdate : LiveData<Void?> get() = _requestEntryInfoUpdate
    private val _requestEntryInfoUpdate = SingleLiveEvent<Void?>()
    val onEntryInfoUpdated : LiveData<EntryInfoTempAttachments> get() = _onEntryInfoUpdated
    private val _onEntryInfoUpdated = SingleLiveEvent<EntryInfoTempAttachments>()

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
    val onDateSelected : LiveData<Date> get() = _onDateSelected
    private val _onDateSelected = SingleLiveEvent<Date>()
    val onTimeSelected : LiveData<Time> get() = _onTimeSelected
    private val _onTimeSelected = SingleLiveEvent<Time>()

    private val mTempAttachments = mutableListOf<EntryAttachmentState>()
    val attachmentDeleted : LiveData<Attachment> get() = _attachmentDeleted
    private val _attachmentDeleted = SingleLiveEvent<Attachment>()
    val onAttachmentAction : LiveData<EntryAttachmentState?> get() = _onAttachmentAction
    private val _onAttachmentAction = MutableLiveData<EntryAttachmentState?>()
    val onBinaryPreviewLoaded : LiveData<AttachmentPosition> get() = _onBinaryPreviewLoaded
    private val _onBinaryPreviewLoaded = SingleLiveEvent<AttachmentPosition>()

    fun requestEntryInfoUpdate() {
        _requestEntryInfoUpdate.call()
    }

    fun updateEntryInfo(entryInfo: EntryInfo) {
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

        _onEntryInfoUpdated.value = EntryInfoTempAttachments(entryInfo, mTempAttachments)
    }

    fun assignTemplate(template: Template) {
        if (this.onTemplateChanged.value != template) {
            _onTemplateChanged.value = template
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
        _onDateSelected.value = Date(year, month, day)
    }

    fun selectTime(hours: Int, minutes: Int) {
        _onTimeSelected.value = Time(hours, minutes)
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

    data class EntryInfoTempAttachments(val entryInfo: EntryInfo, val tempAttachments: List<EntryAttachmentState>)
    data class Date(val year: Int, val month: Int, val day: Int)
    data class Time(val hours: Int, val minutes: Int)
    data class FieldEdition(val oldField: Field?, val newField: Field?)
    data class AttachmentPosition(val entryAttachmentState: EntryAttachmentState, val viewPosition: Float)

    companion object {
        private val TAG = EntryEditViewModel::class.java.name
    }
}