package com.kunzisoft.keepass.viewmodels

import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.model.GroupInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GroupEditViewModel: NodeEditViewModel() {

    private val mGroupEditState = MutableStateFlow<GroupEditState>(GroupEditState.Wait)
    val groupEditState: StateFlow<GroupEditState> = mGroupEditState.asStateFlow()

    var groupNamesNotAllowed : List<String>? = null

    fun approveGroupCreation(groupInfo: GroupInfo) {
        this.mGroupEditState.value = GroupEditState.CreateGroup(groupInfo)
    }

    fun approveGroupUpdate(groupInfo: GroupInfo) {
        // Assume it's only possible with UUID in KDBX database
        groupInfo.id?.let { groupId ->
            this.mGroupEditState.value = GroupEditState.UpdateGroup(NodeIdUUID(groupId), groupInfo)
        }
    }

    fun actionPerformed() {
        this.mGroupEditState.value = GroupEditState.Wait
    }

    sealed class GroupEditState {
        object Wait: GroupEditState()
        data class CreateGroup(
            val groupInfo: GroupInfo
        ): GroupEditState()
        data class UpdateGroup(
            var oldGroupId: NodeId<*>,
            val groupInfo: GroupInfo
        ): GroupEditState()
    }
}