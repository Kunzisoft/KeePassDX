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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.element.template.Template
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.utils.IOActionTask
import java.util.UUID


class EntryViewModel: ViewModel() {

    private var mMainEntryId: NodeId<UUID>? = null
    private var mHistoryPosition: Int = -1

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

    fun loadDatabase(database: ContextualDatabase?) {
        loadEntry(database, mMainEntryId, mHistoryPosition)
    }

    fun loadEntry(database: ContextualDatabase?, mainEntryId: NodeId<UUID>?, historyPosition: Int = -1) {
        this.mMainEntryId = mainEntryId
        this.mHistoryPosition = historyPosition

        if (database != null && mainEntryId != null) {
            IOActionTask(
                {
                    val mainEntry = database.getEntryById(mainEntryId)
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
                    _entryInfoHistory.value = entryInfoHistory
                    _entryHistory.value = entryInfoHistory?.entryHistory
                }
            ).execute()
        }
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

    companion object {
        private val TAG = EntryViewModel::class.java.name
    }
}