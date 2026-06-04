/*
 * Copyright 2026 Jeremy Jamet / Kunzisoft.
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.binary.BinaryData
import com.kunzisoft.keepass.model.AttachmentState
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.StreamDirection
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel to handle the attachments of an entry
 */
class AttachmentsViewModel : ViewModel() {

    private val binaries = mutableListOf<BinaryData>()

    private val _attachmentsUIState = MutableStateFlow<AttachmentUIState>(
        AttachmentUIState(listOf())
    )
    val attachmentsUIState = _attachmentsUIState.asStateFlow()

    private val _attachmentEvents = MutableSharedFlow<AttachmentEvent>(replay = 0)
    val attachmentEvents: SharedFlow<AttachmentEvent> = _attachmentEvents.asSharedFlow()

    private val tempAttachments: List<EntryAttachmentState>
        get() = attachmentsUIState.value.attachments


    private var initialized = false

    fun getAttachments(): List<Attachment> {
        return tempAttachments.map { it.attachment }
    }

    /**
     * Initialize attachments, has only effect the first time
     */
    fun initializeAttachments(attachments: List<Attachment>) {
        if (!initialized) {
            initialized = true
            _attachmentsUIState.update { state ->
                state.copy(
                    attachments = state.attachments.toMutableList().apply {
                        clear()
                        addAll(attachments.map {
                            EntryAttachmentState(it, StreamDirection.UPLOAD)
                        })
                    }
                )
            }
        }
    }

    fun buildNewAttachment(attachmentToUploadUri: Uri, fileName: String) {
        viewModelScope.launch {
            _attachmentEvents.emit(AttachmentEvent.OnBuildNewAttachment(attachmentToUploadUri, fileName))
        }
    }

    fun startUploadAttachment(attachmentToUploadUri: Uri, attachment: Attachment) {
        viewModelScope.launch {
            _attachmentEvents.emit(AttachmentEvent.OnStartUploadAttachment(attachmentToUploadUri, attachment))
        }
    }

    fun deleteAttachment(attachment: EntryAttachmentState) {
        viewModelScope.launch {
            _attachmentEvents.emit(AttachmentEvent.DeleteAttachment(attachment.attachment))
            _attachmentsUIState.update { state ->
                state.copy(attachments = state.attachments.toMutableList().apply {
                    remove(attachment)
                })
            }
        }
    }

    fun onNewBinaryAttachmentBuilt(
        attachment: Attachment,
        allowMultipleAttachment: Boolean,
        attachmentToUploadUri: Uri,
    ) {
        this.binaries.add(attachment.binaryData)
        // Ask to replace the current attachment
        if ((!allowMultipleAttachment && tempAttachments.isNotEmpty()) ||
            tempAttachments.any { it.attachment.name == attachment.name }) {
            viewModelScope.launch {
                _attachmentEvents.emit(AttachmentEvent.ShowReplaceFile(
                    attachmentToUploadUri = attachmentToUploadUri,
                    attachment = attachment
                ))
            }
        } else {
            startUploadAttachment(attachmentToUploadUri, attachment)
        }

    }

    fun onAttachmentAction(entryAttachmentState: EntryAttachmentState?) {
        entryAttachmentState?.let { newState ->
            _attachmentsUIState.update { state ->
                val newAttachments = state.attachments.toMutableList()
                val index = newAttachments.indexOfFirst { it.attachment.name == newState.attachment.name }
                val itemToAdd = newState.copy()
                if (index != -1) {
                    newAttachments[index] = itemToAdd
                } else {
                    newAttachments.add(itemToAdd)
                }
                state.copy(attachments = newAttachments)
            }
            when (newState.downloadState) {
                AttachmentState.CANCELED -> {}
                AttachmentState.ERROR -> {
                    viewModelScope.launch {
                        _attachmentEvents.emit(AttachmentEvent.OnAttachmentError)
                    }
                }
                else -> {
                    viewModelScope.launch {
                        if (newState.downloadState == AttachmentState.COMPLETE
                            || newState.downloadState == AttachmentState.START) {
                            _attachmentEvents.emit(
                                AttachmentEvent.HighlightAttachment(
                                    newState
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun removeTempAttachmentsNotCompleted(database: Database?, entryInfo: EntryInfo) {
        // Do not save entry in upload progression
        tempAttachments.forEach { attachmentState ->
            if (attachmentState.streamDirection == StreamDirection.UPLOAD) {
                if (attachmentState.downloadState != AttachmentState.COMPLETE) {
                    // Remove attachment not finished from info
                    entryInfo.attachments.remove(attachmentState.attachment)
                }
            }
        }

        // Delete orphaned binary previously added if not used
        val binariesUsed = tempAttachments.map { it.attachment.binaryData }.toSet()
        binaries.subtract(binariesUsed).forEach { binaryNotUsed ->
            database?.removeBinaryIfNotUsed(binaryNotUsed)
        }

        viewModelScope.launch {
            _attachmentEvents.emit(AttachmentEvent.OnEntryReadyForSave(entryInfo))
        }
    }

    data class AttachmentUIState(
        val attachments: List<EntryAttachmentState>
    )

    sealed class AttachmentEvent {

        data class OnBuildNewAttachment(
            val attachmentToUploadUri: Uri,
            val fileName: String
        ) : AttachmentEvent()

        data class OnStartUploadAttachment(
            val attachmentToUploadUri: Uri,
            val attachment: Attachment
        ) : AttachmentEvent()

        data class HighlightAttachment(
            val attachment: EntryAttachmentState
        ) : AttachmentEvent()

        data class DeleteAttachment(
            val attachment: Attachment
        ) : AttachmentEvent()

        data class ShowReplaceFile(
            val attachmentToUploadUri: Uri,
            val attachment: Attachment
        ) : AttachmentEvent()

        object OnAttachmentError : AttachmentEvent()

        data class OnEntryReadyForSave(
            val entryInfo: EntryInfo
        ) : AttachmentEvent()
    }
}