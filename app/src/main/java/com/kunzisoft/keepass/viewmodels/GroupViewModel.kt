package com.kunzisoft.keepass.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kunzisoft.keepass.app.database.IOActionTask
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.node.NodeId


class GroupViewModel: ViewModel() {

    private var mDatabase: Database? = null

    val group : LiveData<SuperGroup> get() = _group
    private val _group = MutableLiveData<SuperGroup>()

    val onGroupSelected : LiveData<SuperGroup> get() = _onGroupSelected
    private val _onGroupSelected = SingleLiveEvent<SuperGroup>()

    fun setDatabase(database: Database?) {
        this.mDatabase = database
    }

    fun loadGroup(groupId: NodeId<*>?) {
        IOActionTask(
            {
                if (groupId != null) {
                    mDatabase?.getGroupById(groupId)
                } else {
                    mDatabase?.rootGroup
                }
            },
            { group ->
                if (group != null) {
                    _group.value = SuperGroup(group, mDatabase?.recycleBin == group)
                }
            }
        ).execute()
    }

    fun loadGroup(group: Group) {
        _group.value = SuperGroup(group, mDatabase?.recycleBin == group)
    }

    fun loadGroupFromSearch(searchQuery: String,
                            omitBackup: Boolean) {
        IOActionTask(
            {
                mDatabase?.createVirtualGroupFromSearch(searchQuery, omitBackup)
            },
            { group ->
                if (group != null) {
                    _group.value = SuperGroup(group, mDatabase?.recycleBin == group)
                }
            }
        ).execute()
    }

    fun selectGroup(group: Group) {
        _onGroupSelected.value = SuperGroup(group, mDatabase?.recycleBin == group)
    }

    data class SuperGroup(val group: Group, val isRecycleBin: Boolean)

    companion object {
        private val TAG = GroupViewModel::class.java.name
    }
}