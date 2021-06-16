package com.kunzisoft.keepass.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kunzisoft.keepass.app.database.IOActionTask
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.otp.OtpElement
import java.util.*


class EntryViewModel: ViewModel() {

    private val mDatabase: Database? = Database.getInstance()

    val entryInfo : LiveData<EntryInfo> get() = _entryInfo
    private val _entryInfo = MutableLiveData<EntryInfo>()

    val entryHistory : LiveData<List<Entry>> get() = _entryHistory
    private val _entryHistory = MutableLiveData<List<Entry>>()

    val otpElement : LiveData<OtpElement> get() = _otpElement
    private val _otpElement = SingleLiveEvent<OtpElement>()

    val attachmentSelected : LiveData<Attachment> get() = _attachmentSelected
    private val _attachmentSelected = SingleLiveEvent<Attachment>()
    val onAttachmentAction : LiveData<EntryAttachmentState?> get() = _onAttachmentAction
    private val _onAttachmentAction = MutableLiveData<EntryAttachmentState?>()

    val historySelected : LiveData<EntryHistory> get() = _historySelected
    private val _historySelected = SingleLiveEvent<EntryHistory>()

    fun loadEntry(entry: Entry) {
        IOActionTask(
            {
                // To simplify template field visibility
                mDatabase?.decodeEntryWithTemplateConfiguration(entry)?.let {
                    // To update current modification time
                    it.touch(modified = false, touchParents = false)
                    EntryInfoHistory(it.getEntryInfo(mDatabase), it.getHistory())
                }
            },
            {
                _entryInfo.value = it?.entryInfo
                _entryHistory.value = it?.entryHistory
            }
        ).execute()
    }

    fun onOtpElementUpdated(optElement: OtpElement) {
        _otpElement.value = optElement
    }

    fun onAttachmentSelected(attachment: Attachment) {
        _attachmentSelected.value = attachment
    }

    fun onAttachmentAction(entryAttachmentState: EntryAttachmentState?) {
        _onAttachmentAction.value = entryAttachmentState
    }

    fun onHistorySelected(item: Entry, position: Int) {
        _historySelected.value = EntryHistory(item.nodeId, item, null, position)
    }

    data class EntryInfoHistory(val entryInfo: EntryInfo, val entryHistory: List<Entry>)
    // Custom data class to manage entry to retrieve and define is it's an history item (!= -1)
    data class EntryHistory(var nodeIdUUID: NodeId<UUID>?,
                            var entry: Entry?,
                            var lastEntryVersion: Entry?,
                            var historyPosition: Int = -1)

    companion object {
        private val TAG = EntryViewModel::class.java.name
    }
}