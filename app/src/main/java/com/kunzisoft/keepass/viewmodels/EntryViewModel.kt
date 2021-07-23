package com.kunzisoft.keepass.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kunzisoft.keepass.app.database.IOActionTask
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.element.template.Template
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.otp.OtpElement
import java.util.*


class EntryViewModel: ViewModel() {

    private val mDatabase: Database? = Database.getInstance()

    private var mEntryTemplate: Template? = null
    private var mEntry: Entry? = null
    private var mLastEntryVersion: Entry? = null
    private var mHistoryPosition: Int = -1

    val template : LiveData<Template> get() = _template
    private val _template = MutableLiveData<Template>()

    val entryInfo : LiveData<EntryInfo> get() = _entryInfo
    private val _entryInfo = MutableLiveData<EntryInfo>()

    val entryIsHistory : LiveData<Boolean> get() = _entryIsHistory
    private val _entryIsHistory = MutableLiveData<Boolean>()

    val entryHistory : LiveData<List<EntryInfo>> get() = _entryHistory
    private val _entryHistory = MutableLiveData<List<EntryInfo>>()

    val onOtpElementUpdated : LiveData<OtpElement?> get() = _onOtpElementUpdated
    private val _onOtpElementUpdated = SingleLiveEvent<OtpElement?>()

    val attachmentSelected : LiveData<Attachment> get() = _attachmentSelected
    private val _attachmentSelected = SingleLiveEvent<Attachment>()
    val onAttachmentAction : LiveData<EntryAttachmentState?> get() = _onAttachmentAction
    private val _onAttachmentAction = MutableLiveData<EntryAttachmentState?>()

    val historySelected : LiveData<EntryHistory> get() = _historySelected
    private val _historySelected = SingleLiveEvent<EntryHistory>()

    fun loadEntry(entryId: NodeId<UUID>, historyPosition: Int) {
        IOActionTask(
            {
                // Manage current version and history
                mLastEntryVersion = mDatabase?.getEntryById(entryId)

                mEntry = if (historyPosition > -1) {
                    mLastEntryVersion?.getHistory()?.get(historyPosition)
                } else {
                    mLastEntryVersion
                }

                mEntryTemplate = mEntry?.let {
                    mDatabase?.getTemplate(it)
                } ?: Template.STANDARD

                mHistoryPosition = historyPosition

                // To simplify template field visibility
                mEntry?.let { entry ->
                    // Add mLastEntryVersion to check the parent and define the template state
                    mDatabase?.decodeEntryWithTemplateConfiguration(entry, mLastEntryVersion)?.let {
                        // To update current modification time
                        it.touch(modified = false, touchParents = false)

                        // Build history info
                        val entryInfoHistory = it.getHistory().map { entryHistory ->
                            entryHistory.getEntryInfo(mDatabase)
                        }

                        EntryInfoHistory(
                            mEntryTemplate ?: Template.STANDARD,
                            it.getEntryInfo(mDatabase),
                            entryInfoHistory
                        )
                    }
                }
            },
            { entryInfoHistory ->
                if (entryInfoHistory != null) {
                    _template.value = entryInfoHistory.template
                    _entryInfo.value = entryInfoHistory.entryInfo
                    _entryIsHistory.value = mHistoryPosition != -1
                    _entryHistory.value = entryInfoHistory.entryHistory
                }
            }
        ).execute()
    }

    fun updateEntry() {
        mEntry?.nodeId?.let { nodeId ->
            loadEntry(nodeId, mHistoryPosition)
        }
    }

    // TODO Remove
    fun getEntry(): Entry? {
        return mEntry
    }

    // TODO Remove
    fun getMainEntry(): Entry? {
        return mLastEntryVersion
    }

    // TODO Remove
    fun getEntryHistoryPosition(): Int {
        return mHistoryPosition
    }

    // TODO Remove
    fun getEntryIsHistory(): Boolean {
        return entryIsHistory.value ?: false
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

    data class EntryInfoHistory(val template: Template,
                                val entryInfo: EntryInfo,
                                val entryHistory: List<EntryInfo>)
    // Custom data class to manage entry to retrieve and define is it's an history item (!= -1)
    data class EntryHistory(var nodeId: NodeId<UUID>,
                            var template: Template?,
                            var entryInfo: EntryInfo,
                            var historyPosition: Int = -1)

    companion object {
        private val TAG = EntryViewModel::class.java.name
    }
}