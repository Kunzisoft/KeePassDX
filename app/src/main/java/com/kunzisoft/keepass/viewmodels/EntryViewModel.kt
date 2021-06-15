package com.kunzisoft.keepass.viewmodels

import android.os.Parcel
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.otp.OtpElement
import java.util.*


class EntryViewModel: ViewModel() {

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

    fun loadEntryInfo(entryInfo: EntryInfo) {
        _entryInfo.value = entryInfo
    }

    fun loadEntryHistory(entryHistory: List<Entry>) {
        _entryHistory.value = entryHistory
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

    // Custom data class to manage entry to retrieve and define is it's an history item (!= -1)
    data class EntryHistory(var nodeIdUUID: NodeId<UUID>?,
                            var entry: Entry?,
                            var lastEntryVersion: Entry?,
                            var historyPosition: Int = -1): Parcelable {
        constructor(parcel: Parcel) : this(
                parcel.readParcelable(NodeId::class.java.classLoader),
                parcel.readParcelable(Entry::class.java.classLoader),
                parcel.readParcelable(Entry::class.java.classLoader),
                parcel.readInt())

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeParcelable(nodeIdUUID, flags)
            parcel.writeParcelable(entry, flags)
            parcel.writeParcelable(lastEntryVersion, flags)
            parcel.writeInt(historyPosition)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<EntryHistory> {
            override fun createFromParcel(parcel: Parcel): EntryHistory {
                return EntryHistory(parcel)
            }

            override fun newArray(size: Int): Array<EntryHistory?> {
                return arrayOfNulls(size)
            }
        }
    }

    companion object {
        private val TAG = EntryViewModel::class.java.name
    }
}