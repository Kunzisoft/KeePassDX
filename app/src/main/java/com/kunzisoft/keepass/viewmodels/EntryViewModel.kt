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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.element.template.Template
import com.kunzisoft.keepass.database.element.template.TemplateField
import com.kunzisoft.keepass.database.helper.getLocalizedName
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.FieldProtection
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.timeout.ClipboardHelper
import com.kunzisoft.keepass.utils.IOActionTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID


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

    private var mClipboardHelper: ClipboardHelper = ClipboardHelper(application)

    val entryInfoHistory : LiveData<EntryInfoHistory?> get() = _entryInfoHistory
    private val _entryInfoHistory = MutableLiveData<EntryInfoHistory?>()

    val entryHistory : LiveData<List<EntryInfo>?> get() = _entryHistory
    private val _entryHistory = MutableLiveData<List<EntryInfo>?>()

    val onOtpElementUpdated : LiveData<OtpElement?> get() = _onOtpElementUpdated
    private val _onOtpElementUpdated = SingleLiveEvent<OtpElement?>()

    val attachmentSelected : LiveData<Attachment> get() = _attachmentSelected
    private val _attachmentSelected = SingleLiveEvent<Attachment>()
    val onAttachmentAction : LiveData<EntryAttachmentState?> get() = _onAttachmentAction
    private val _onAttachmentAction = MutableLiveData<EntryAttachmentState?>()

    val sectionSelected : LiveData<EntrySection> get() = _sectionSelected
    private val _sectionSelected = MutableLiveData<EntrySection>()

    val historySelected : LiveData<EntryHistory> get() = _historySelected
    private val _historySelected = SingleLiveEvent<EntryHistory>()

    private val mEntryState = MutableStateFlow<EntryState>(EntryState.Loading)
    val entryState: StateFlow<EntryState> = mEntryState.asStateFlow()

    fun loadDatabase(database: ContextualDatabase?) {
        loadEntry(database, mainEntryId, historyPosition)
    }

    fun loadEntry(database: ContextualDatabase?, mainEntryId: NodeId<UUID>?, historyPosition: Int = -1) {
        this.mainEntryId = mainEntryId
        this.historyPosition = historyPosition

        if (database != null && mainEntryId != null) {
            IOActionTask(
                {
                    val mainEntry = database.getEntryById(mainEntryId)
                    // To sort by access
                    mainEntry?.let {
                        it.touch(modified = false, touchParents = false)
                        database.updateEntry(entry = it, dataModified = false)
                    }
                    val currentEntry = if (historyPosition > -1) {
                        mainEntry?.getHistory()?.get(historyPosition)
                    } else {
                        mainEntry
                    }

                    val entryTemplate = currentEntry?.let {
                        database.getTemplate(it)
                    } ?: Template.STANDARD

                    // To simplify template field visibility
                    currentEntry?.let { entry ->
                        // Add mainEntry to check the parent and define the template state
                        database.decodeEntryWithTemplateConfiguration(entry, mainEntry).let {
                            // To update current modification time
                            it.touch(modified = false, touchParents = false)

                            // Build history info
                            val entryInfoHistory = it.getHistory().map { entryHistory ->
                                entryHistory.getEntryInfo(database)
                            }

                            EntryInfoHistory(
                                mainEntry!!.nodeId,
                                historyPosition,
                                entryTemplate,
                                it.getEntryInfo(database),
                                entryInfoHistory
                            )
                        }
                    }
                },
                { entryInfoHistory ->
                    if (entryInfoHistory != null) {
                        this.mainEntryId = entryInfoHistory.mainEntryId
                        this.entryInfo = entryInfoHistory.entryInfo
                        this.historyPosition = historyPosition
                        this.entryIsHistory = historyPosition > -1
                        this.entryLoaded = true
                    }
                    _entryInfoHistory.value = entryInfoHistory
                    _entryHistory.value = entryInfoHistory?.entryHistory
                }
            ).execute()
        }
    }

    fun updateProtectionField(fieldProtection: FieldProtection, value: Boolean) {
        fieldProtection.isCurrentlyProtected = value
        mEntryState.value = EntryState.OnFieldProtectionUpdated(fieldProtection)
    }

    fun requestChangeFieldProtection(fieldProtection: FieldProtection) {
        mEntryState.value = EntryState.OnChangeFieldProtectionRequested(fieldProtection)
    }

    fun requestCopyField(fieldProtection: FieldProtection) {
        // Only request the User Verification if the field is protected and not shown
        val field = fieldProtection.field
        if (field.protectedValue.isProtected && fieldProtection.isCurrentlyProtected)
            mEntryState.value = EntryState.RequestCopyProtectedField(fieldProtection)
        else
            copyToClipboard(field)
    }

    fun onOtpElementUpdated(optElement: OtpElement?) {
        _onOtpElementUpdated.value = optElement
    }

    fun onAttachmentSelected(attachment: Attachment) {
        _attachmentSelected.value = attachment
    }

    fun onAttachmentAction(entryAttachmentState: EntryAttachmentState?) {
        _onAttachmentAction.value = entryAttachmentState
    }

    fun onHistorySelected(item: EntryInfo, position: Int) {
        _historySelected.value = EntryHistory(NodeIdUUID(item.id), null, item, position)
    }

    fun selectSection(section: EntrySection) {
        _sectionSelected.value = section
    }

    fun copyToClipboard(field: Field) {
        mClipboardHelper.timeoutCopyToClipboard(
            TemplateField.getLocalizedName(getApplication(), field.name),
            field.protectedValue.toString(),
            field.protectedValue.isProtected
        )
    }

    fun copyToClipboard(text: String) {
        mClipboardHelper.timeoutCopyToClipboard(text, text)
    }

    fun actionPerformed() {
        mEntryState.value = EntryState.Loading
    }

    data class EntryInfoHistory(var mainEntryId: NodeId<UUID>,
                                var historyPosition: Int,
                                val template: Template,
                                val entryInfo: EntryInfo,
                                val entryHistory: List<EntryInfo>)
    // Custom data class to manage entry to retrieve and define is it's an history item (!= -1)
    data class EntryHistory(var nodeId: NodeId<UUID>,
                            var template: Template?,
                            var entryInfo: EntryInfo,
                            var historyPosition: Int = -1)

    enum class EntrySection(var position: Int) {
        MAIN(0), ADVANCED(1);

        companion object {
            fun getEntrySectionByPosition(position: Int): EntrySection {
                return if (position == 1) ADVANCED else MAIN
            }
        }
    }

    sealed class EntryState {
        object Loading: EntryState()
        data class OnChangeFieldProtectionRequested(
            val fieldProtection: FieldProtection
        ): EntryState()
        data class OnFieldProtectionUpdated(
            val fieldProtection: FieldProtection
        ): EntryState()
        data class RequestCopyProtectedField(
            val fieldProtection: FieldProtection
        ): EntryState()
    }

    companion object {
        private val TAG = EntryViewModel::class.java.name
    }
}