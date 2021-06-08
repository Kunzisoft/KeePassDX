package com.kunzisoft.keepass.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kunzisoft.keepass.database.element.template.Template
import com.kunzisoft.keepass.model.EntryInfo


class EntryEditViewModel: ViewModel() {
    
    val entryInfoLoaded : LiveData<EntryInfo> get() = _entryInfo
    private val _entryInfo  = MutableLiveData<EntryInfo>()

    val saveEntryRequested : LiveData<EntryInfo> get() = _requestSaveEntry
    private val _requestSaveEntry = SingleLiveEvent<EntryInfo>()
    val saveEntryResponded : LiveData<EntryInfo> get() = _responseSaveEntry
    private val _responseSaveEntry = SingleLiveEvent<EntryInfo>()

    val templateChanged : LiveData<Template> get() = _template
    private val _template = SingleLiveEvent<Template>()

    fun setEntryInfo(entryInfo: EntryInfo?) {
        _entryInfo.value = entryInfo
    }

    fun sendRequestSaveEntry() {
        _requestSaveEntry.value = entryInfoLoaded.value
    }

    fun setResponseSaveEntry(entryInfo: EntryInfo?) {
        _responseSaveEntry.value = entryInfo
    }

    fun assignTemplate(template: Template) {
        if (this.templateChanged.value != template) {
            _template.value = template
        }
    }

    companion object {
        private val TAG = EntryEditViewModel::class.java.name
    }
}