package com.kunzisoft.keepass.viewmodels

import android.os.Parcel
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.otp.OtpElement
import java.util.*


class EntryViewModel: ViewModel() {

    private val mDatabase = Database.getInstance()

    val entry : LiveData<EntryHistory> get() = _entry
    private val _entry = MutableLiveData<EntryHistory>()

    val otpElement : LiveData<OtpElement> get() = _otpElement
    private val _otpElement = SingleLiveEvent<OtpElement>()

    val attachmentSelected : LiveData<Attachment> get() = _attachmentSelected
    private val _attachmentSelected = SingleLiveEvent<Attachment>()

    val historySelected : LiveData<EntryHistory> get() = _historySelected
    private val _historySelected = SingleLiveEvent<EntryHistory>()

    fun selectEntry(nodeIdUUID: NodeId<UUID>?, historyPosition: Int) {
        if (nodeIdUUID != null) {
            val entryLastVersion = mDatabase.getEntryById(nodeIdUUID)
            var entry = entryLastVersion
            if (historyPosition > -1) {
                entry = entry?.getHistory()?.get(historyPosition)
            }
            entry?.touch(modified = false, touchParents = false)
            _entry.value = EntryHistory(entry, entryLastVersion, historyPosition)
        } else {
            _entry.value = EntryHistory(null, null)
        }
    }

    fun reloadEntry() {
        _entry.value = entry.value
    }

    fun onOtpElementUpdated(optElement: OtpElement) {
        _otpElement.value = optElement
    }

    fun onAttachmentSelected(attachment: Attachment) {
        _attachmentSelected.value = attachment
    }

    fun onHistorySelected(item: Entry, position: Int) {
        _historySelected.value = EntryHistory(item, null, position)
    }

    // Custom data class to manage entry to retrieve and define is it's an history item (!= -1)
    data class EntryHistory(var entry: Entry?,
                            var lastEntryVersion: Entry?,
                            var historyPosition: Int = -1): Parcelable {
        constructor(parcel: Parcel) : this(
                parcel.readParcelable(Entry::class.java.classLoader),
                parcel.readParcelable(Entry::class.java.classLoader),
                parcel.readInt())

        override fun writeToParcel(parcel: Parcel, flags: Int) {
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