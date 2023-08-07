/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.activities.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.SortDialogFragment
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.activities.helpers.SpecialMode
import com.kunzisoft.keepass.adapters.NodesAdapter
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.SortNodeEnum
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.database.element.node.Type
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.utils.KeyboardUtil.hideKeyboard
import com.kunzisoft.keepass.viewmodels.GroupViewModel
import java.util.LinkedList

class GroupFragment : DatabaseFragment(), SortDialogFragment.SortSelectionListener {

    private var nodeClickListener: NodeClickListener? = null
    private var onScrollListener: OnScrollListener? = null
    private var groupRefreshed: GroupRefreshedListener? = null

    private var mNodesRecyclerView: RecyclerView? = null
    private var mLayoutManager: LinearLayoutManager? = null
    private var mAdapter: NodesAdapter? = null

    private val mGroupViewModel: GroupViewModel by activityViewModels()

    private var mCurrentGroup: Group? = null

    var nodeActionSelectionMode = false
        private set
    var nodeActionPasteMode: PasteMode = PasteMode.UNDEFINED
        private set
    private val listActionNodes = LinkedList<Node>()
    private val listPasteNodes = LinkedList<Node>()

    private var notFoundView: View? = null
    private var isASearchResult: Boolean = false

    private var specialMode: SpecialMode = SpecialMode.DEFAULT

    private var mRecycleBinEnable: Boolean = false
    private var mRecycleBin: Group? = null

    private var mRecycleViewScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == SCROLL_STATE_IDLE) {
                mGroupViewModel.assignPosition(getFirstVisiblePosition())
            }
        }
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            onScrollListener?.onScrolled(dy)
        }
    }

    private val menuProvider: MenuProvider = object: MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.tree, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menu_sort -> {
                    context?.let { context ->
                        val sortDialogFragment: SortDialogFragment =
                            if (mRecycleBinEnable) {
                                SortDialogFragment.getInstance(
                                    PreferencesUtil.getListSort(context),
                                    PreferencesUtil.getAscendingSort(context),
                                    PreferencesUtil.getGroupsBeforeSort(context),
                                    PreferencesUtil.getRecycleBinBottomSort(context)
                                )
                            } else {
                                SortDialogFragment.getInstance(
                                    PreferencesUtil.getListSort(context),
                                    PreferencesUtil.getAscendingSort(context),
                                    PreferencesUtil.getGroupsBeforeSort(context)
                                )
                            }

                        sortDialogFragment.show(childFragmentManager, "sortDialog")
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // TODO Change to ViewModel
        try {
            nodeClickListener = context as NodeClickListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException(context.toString()
                    + " must implement " + NodesAdapter.NodeClickCallback::class.java.name)
        }

        try {
            onScrollListener = context as OnScrollListener
        } catch (e: ClassCastException) {
            onScrollListener = null
            // Context menu can be omit
            Log.w(
                TAG, context.toString()
                    + " must implement " + RecyclerView.OnScrollListener::class.java.name)
        }

        try {
            groupRefreshed = context as GroupRefreshedListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException(context.toString()
                    + " must implement " + GroupRefreshedListener::class.java.name)
        }
    }

    override fun onDetach() {
        nodeClickListener = null
        onScrollListener = null
        groupRefreshed = null
        super.onDetach()
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        mRecycleBinEnable = database?.isRecycleBinEnabled == true
        mRecycleBin = database?.recycleBin

        context?.let { context ->
            database?.let { database ->
                mAdapter = NodesAdapter(context, database).apply {
                    setOnNodeClickListener(object : NodesAdapter.NodeClickCallback {
                        override fun onNodeClick(database: ContextualDatabase, node: Node) {
                            if (nodeActionSelectionMode) {
                                if (listActionNodes.contains(node)) {
                                    // Remove selected item if already selected
                                    listActionNodes.remove(node)
                                } else {
                                    // Add selected item if not already selected
                                    listActionNodes.add(node)
                                }
                                nodeClickListener?.onNodeSelected(database, listActionNodes)
                                setActionNodes(listActionNodes)
                                notifyNodeChanged(node)
                            } else {
                                nodeClickListener?.onNodeClick(database, node)
                            }
                        }

                        override fun onNodeLongClick(database: ContextualDatabase, node: Node): Boolean {
                            if (nodeActionPasteMode == PasteMode.UNDEFINED) {
                                // Select the first item after a long click
                                if (!listActionNodes.contains(node))
                                    listActionNodes.add(node)

                                nodeClickListener?.onNodeSelected(database, listActionNodes)

                                setActionNodes(listActionNodes)
                                notifyNodeChanged(node)
                                activity?.hideKeyboard()
                            }
                            return true
                        }
                    })
                }
                mNodesRecyclerView?.adapter = mAdapter
            }
        }
    }

    override fun onDatabaseActionFinished(
        database: ContextualDatabase,
        actionTask: String,
        result: ActionRunnable.Result
    ) {
        super.onDatabaseActionFinished(database, actionTask, result)

        // Too many special cases to make specific additions or deletions,
        // rebuilt the list works well.
        if (result.isSuccess) {
            rebuildList()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        // To apply theme
        return inflater.inflate(R.layout.fragment_nodes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.addMenuProvider(menuProvider, viewLifecycleOwner)

        mNodesRecyclerView = view.findViewById(R.id.nodes_list)
        notFoundView = view.findViewById(R.id.not_found_container)

        mLayoutManager = LinearLayoutManager(context)
        mNodesRecyclerView?.apply {
            scrollBarStyle = View.SCROLLBARS_INSIDE_INSET
            layoutManager = mLayoutManager
            adapter = mAdapter
        }
        resetAppTimeoutWhenViewFocusedOrChanged(view)

        mGroupViewModel.group.observe(viewLifecycleOwner) {
            mCurrentGroup = it.group
            isASearchResult = it.group.isVirtual
            rebuildList()
            it.showFromPosition?.let { position ->
                mNodesRecyclerView?.scrollToPosition(position)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        mNodesRecyclerView?.addOnScrollListener(mRecycleViewScrollListener)
        activity?.intent?.let {
            specialMode = EntrySelectionHelper.retrieveSpecialModeFromIntent(it)
        }
    }

    override fun onPause() {

        mNodesRecyclerView?.removeOnScrollListener(mRecycleViewScrollListener)
        super.onPause()
    }

    fun getFirstVisiblePosition(): Int {
        return mLayoutManager?.findFirstVisibleItemPosition() ?: 0
    }

    private fun rebuildList() {
        try {
            // Add elements to the list
            mCurrentGroup?.let { currentGroup ->
                // Thrown an exception when sort cannot be performed
                mAdapter?.rebuildList(currentGroup)
            }
        } catch (e:Exception) {
            Log.e(TAG, "Unable to rebuild the list", e)
        }

        if (isASearchResult && mAdapter != null && mAdapter!!.isEmpty) {
            // To show the " no search entry found "
            notFoundView?.visibility = View.VISIBLE
        } else {
            notFoundView?.visibility = View.GONE
        }

        groupRefreshed?.onGroupRefreshed()
    }

    override fun onSortSelected(sortNodeEnum: SortNodeEnum,
                                sortNodeParameters: SortNodeEnum.SortNodeParameters) {
        // Save setting
        context?.let {
            PreferencesUtil.saveNodeSort(it, sortNodeEnum, sortNodeParameters)
        }

        // Tell the adapter to refresh it's list
        try {
            mAdapter?.notifyChangeSort(sortNodeEnum, sortNodeParameters)
            rebuildList()
        } catch (e:Exception) {
            Log.e(TAG, "Unable to sort the list", e)
        }
    }

    fun actionNodesCallback(database: ContextualDatabase,
                            nodes: List<Node>,
                            menuListener: NodesActionMenuListener?,
                            onDestroyActionMode: (mode: ActionMode?) -> Unit) : ActionMode.Callback {

        return object : ActionMode.Callback {

            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                nodeActionSelectionMode = false
                nodeActionPasteMode = PasteMode.UNDEFINED
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                menu?.clear()
                if (nodeActionPasteMode != PasteMode.UNDEFINED) {
                    mode?.menuInflater?.inflate(R.menu.node_paste_menu, menu)
                } else {
                    nodeActionSelectionMode = true
                    mode?.menuInflater?.inflate(R.menu.node_menu, menu)

                    // Open and Edit for a single item
                    if (nodes.size == 1) {
                        // Edition
                        if (database.isReadOnly
                                || (mRecycleBinEnable && nodes[0] == mRecycleBin)) {
                            menu?.removeItem(R.id.menu_edit)
                        }
                    } else {
                        menu?.removeItem(R.id.menu_open)
                        menu?.removeItem(R.id.menu_edit)
                    }

                    // Move
                    if (database.isReadOnly) {
                        menu?.removeItem(R.id.menu_move)
                    }

                    // Copy (not allowed for group)
                    if (database.isReadOnly
                            || nodes.any { it.type == Type.GROUP }) {
                        menu?.removeItem(R.id.menu_copy)
                    }

                    // Deletion
                    if (database.isReadOnly
                            || (mRecycleBinEnable && nodes.any { it == mRecycleBin })) {
                        menu?.removeItem(R.id.menu_delete)
                    }
                }

                // Add the number of items selected in title
                mode?.title = nodes.size.toString()
                return true
            }

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                if (menuListener == null)
                    return false
                return when (item?.itemId) {
                    R.id.menu_open -> menuListener.onOpenMenuClick(database, nodes[0])
                    R.id.menu_edit -> menuListener.onEditMenuClick(database, nodes[0])
                    R.id.menu_copy -> {
                        nodeActionPasteMode = PasteMode.PASTE_FROM_COPY
                        mAdapter?.unselectActionNodes()
                        val returnValue = menuListener.onCopyMenuClick(database, nodes)
                        nodeActionSelectionMode = false
                        returnValue
                    }
                    R.id.menu_move -> {
                        nodeActionPasteMode = PasteMode.PASTE_FROM_MOVE
                        mAdapter?.unselectActionNodes()
                        val returnValue = menuListener.onMoveMenuClick(database, nodes)
                        nodeActionSelectionMode = false
                        returnValue
                    }
                    R.id.menu_delete -> menuListener.onDeleteMenuClick(database, nodes)
                    R.id.menu_paste -> {
                        val returnValue = menuListener.onPasteMenuClick(database, nodeActionPasteMode, nodes)
                        nodeActionPasteMode = PasteMode.UNDEFINED
                        nodeActionSelectionMode = false
                        returnValue
                    }
                    else -> false
                }
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                listActionNodes.clear()
                listPasteNodes.clear()
                mAdapter?.unselectActionNodes()
                nodeActionPasteMode = PasteMode.UNDEFINED
                nodeActionSelectionMode = false
                onDestroyActionMode(mode)
            }
        }
    }

    /**
     * Callback listener to redefine to do an action when a node is click
     */
    interface NodeClickListener {
        fun onNodeClick(database: ContextualDatabase, node: Node)
        fun onNodeSelected(database: ContextualDatabase, nodes: List<Node>): Boolean
    }

    /**
     * Menu listener to redefine to do an action in menu
     */
    interface NodesActionMenuListener {
        fun onOpenMenuClick(database: ContextualDatabase, node: Node): Boolean
        fun onEditMenuClick(database: ContextualDatabase, node: Node): Boolean
        fun onCopyMenuClick(database: ContextualDatabase, nodes: List<Node>): Boolean
        fun onMoveMenuClick(database: ContextualDatabase, nodes: List<Node>): Boolean
        fun onDeleteMenuClick(database: ContextualDatabase, nodes: List<Node>): Boolean
        fun onPasteMenuClick(database: ContextualDatabase, pasteMode: PasteMode?, nodes: List<Node>): Boolean
    }

    enum class PasteMode {
        UNDEFINED, PASTE_FROM_COPY, PASTE_FROM_MOVE
    }

    interface OnScrollListener {

        /**
         * Callback method to be invoked when the RecyclerView has been scrolled. This will be
         * called after the scroll has completed.
         *
         * @param dy The amount of vertical scroll.
         */
        fun onScrolled(dy: Int)
    }

    interface GroupRefreshedListener {
        fun onGroupRefreshed()
    }

    companion object {
        private val TAG = GroupFragment::class.java.name
    }
}
