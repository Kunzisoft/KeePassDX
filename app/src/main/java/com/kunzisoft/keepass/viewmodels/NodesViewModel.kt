package com.kunzisoft.keepass.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kunzisoft.keepass.database.element.node.Node

class NodesViewModel: ViewModel() {

    val nodesToDelete : LiveData<List<Node>> get() = _nodesToDelete
    private val _nodesToDelete = MutableLiveData<List<Node>>()

    val nodesToPermanentlyDelete : LiveData<List<Node>> get() = _nodesToPermanentlyDelete
    private val _nodesToPermanentlyDelete = SingleLiveEvent<List<Node>>()

    fun deleteNodes(nodes: List<Node>) {
        this._nodesToDelete.value = nodes.toList()
    }

    fun permanentlyDeleteNodes(nodes: List<Node>) {
        this._nodesToDelete.value = listOf()
        this._nodesToPermanentlyDelete.value = nodes
    }
}