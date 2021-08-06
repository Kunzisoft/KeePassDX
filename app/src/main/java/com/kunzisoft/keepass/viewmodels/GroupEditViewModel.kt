package com.kunzisoft.keepass.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kunzisoft.keepass.model.GroupInfo

class GroupEditViewModel: NodeEditViewModel() {

    val groupNamesNotAllowed : LiveData<List<String>?> get() = _groupNamesNotAllowed
    private val _groupNamesNotAllowed = MutableLiveData<List<String>?>()

    val onGroupCreated : LiveData<GroupInfo> get() = _onGroupCreated
    private val _onGroupCreated = SingleLiveEvent<GroupInfo>()

    val onGroupUpdated : LiveData<GroupInfo> get() = _onGroupUpdated
    private val _onGroupUpdated = SingleLiveEvent<GroupInfo>()

    fun setGroupNamesNotAllowed(groupNames: List<String>?) {
        this._groupNamesNotAllowed.value = groupNames
    }

    fun approveGroupCreation(groupInfo: GroupInfo) {
        this._onGroupCreated.value = groupInfo
    }

    fun approveGroupUpdate(groupInfo: GroupInfo) {
        this._onGroupUpdated.value = groupInfo
    }
}