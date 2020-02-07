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
package com.kunzisoft.keepass.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode

import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.adapters.NodeAdapter
import com.kunzisoft.keepass.database.element.SortNodeEnum
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.activities.dialogs.SortDialogFragment
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.activities.stylish.StylishFragment
import com.kunzisoft.keepass.activities.helpers.ReadOnlyHelper
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.node.Type
import java.util.*

class ListNodesFragment : StylishFragment(), SortDialogFragment.SortSelectionListener {

    private var nodeClickListener: NodeClickListener? = null
    private var onScrollListener: OnScrollListener? = null

    private var listView: RecyclerView? = null
    var mainGroup: Group? = null
        private set
    private var mAdapter: NodeAdapter? = null

    var nodeActionSelectionMode = false
        private set
    var nodeActionPasteMode: PasteMode = PasteMode.UNDEFINED
        private set
    private val listActionNodes = LinkedList<Node>()
    private val listPasteNodes = LinkedList<Node>()

    private var notFoundView: View? = null
    private var isASearchResult: Boolean = false

    // Preferences for sorting
    private var prefs: SharedPreferences? = null

    private var readOnly: Boolean = false
        get() {
            return field || selectionMode
        }
    private var selectionMode: Boolean = false

    val isEmpty: Boolean
        get() = mAdapter == null || mAdapter?.itemCount?:0 <= 0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            nodeClickListener = context as NodeClickListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException(context.toString()
                    + " must implement " + NodeAdapter.NodeClickCallback::class.java.name)
        }

        try {
            onScrollListener = context as OnScrollListener
        } catch (e: ClassCastException) {
            onScrollListener = null
            // Context menu can be omit
            Log.w(TAG, context.toString()
                    + " must implement " + RecyclerView.OnScrollListener::class.java.name)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        readOnly = ReadOnlyHelper.retrieveReadOnlyFromInstanceStateOrArguments(savedInstanceState, arguments)

        arguments?.let { args ->
            // Contains all the group in element
            if (args.containsKey(GROUP_KEY)) {
                mainGroup = args.getParcelable(GROUP_KEY)
            }
            if (args.containsKey(IS_SEARCH)) {
                isASearchResult = args.getBoolean(IS_SEARCH)
            }
        }

        contextThemed?.let { context ->
            mAdapter = NodeAdapter(context)
            mAdapter?.apply {
                setOnNodeClickListener(object : NodeAdapter.NodeClickCallback {
                    override fun onNodeClick(node: Node) {
                        if (nodeActionSelectionMode) {
                            if (listActionNodes.contains(node)) {
                                // Remove selected item if already selected
                                listActionNodes.remove(node)
                            } else {
                                // Add selected item if not already selected
                                listActionNodes.add(node)
                            }
                            nodeClickListener?.onNodeSelected(listActionNodes)
                            setActionNodes(listActionNodes)
                            notifyNodeChanged(node)
                        } else {
                            nodeClickListener?.onNodeClick(node)
                        }
                    }

                    override fun onNodeLongClick(node: Node): Boolean {
                        if (nodeActionPasteMode == PasteMode.UNDEFINED) {
                            // Select the first item after a long click
                            if (!listActionNodes.contains(node))
                                listActionNodes.add(node)

                            nodeClickListener?.onNodeSelected(listActionNodes)

                            setActionNodes(listActionNodes)
                            notifyNodeChanged(node)
                        }
                        return true
                    }
                })
            }
        }
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        ReadOnlyHelper.onSaveInstanceState(outState, readOnly)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        // To apply theme
        val rootView = inflater.cloneInContext(contextThemed)
                .inflate(R.layout.fragment_list_nodes, container, false)
        listView = rootView.findViewById(R.id.nodes_list)
        notFoundView = rootView.findViewById(R.id.not_found_container)

        onScrollListener?.let { onScrollListener ->
            listView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    onScrollListener.onScrolled(dy)
                }
            })
        }

        rebuildList()

        return rootView
    }

    override fun onResume() {
        super.onResume()

        activity?.intent?.let {
            selectionMode = EntrySelectionHelper.retrieveEntrySelectionModeFromIntent(it)
        }

        // Refresh data
        mAdapter?.notifyDataSetChanged()

        if (isASearchResult && mAdapter!= null && mAdapter!!.isEmpty) {
            // To show the " no search entry found "
            listView?.visibility = View.GONE
            notFoundView?.visibility = View.VISIBLE
        } else {
            listView?.visibility = View.VISIBLE
            notFoundView?.visibility = View.GONE
        }
    }

    fun rebuildList() {
        // Add elements to the list
        mainGroup?.let { mainGroup ->
            mAdapter?.rebuildList(mainGroup)
        }
        listView?.apply {
            scrollBarStyle = View.SCROLLBARS_INSIDE_INSET
            layoutManager = LinearLayoutManager(context)
            adapter = mAdapter
        }
    }

    override fun onSortSelected(sortNodeEnum: SortNodeEnum, ascending: Boolean, groupsBefore: Boolean, recycleBinBottom: Boolean) {
        // Toggle setting
        prefs?.edit()?.apply {
            putString(getString(R.string.sort_node_key), sortNodeEnum.name)
            putBoolean(getString(R.string.sort_ascending_key), ascending)
            putBoolean(getString(R.string.sort_group_before_key), groupsBefore)
            putBoolean(getString(R.string.sort_recycle_bin_bottom_key), recycleBinBottom)
            apply()
        }

        // Tell the adapter to refresh it's list
        mAdapter?.notifyChangeSort(sortNodeEnum, ascending, groupsBefore)
        mainGroup?.let { mainGroup ->
            mAdapter?.rebuildList(mainGroup)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.tree, menu)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.menu_sort -> {
                context?.let { context ->
                    val sortDialogFragment: SortDialogFragment =
                            if (Database.getInstance().isRecycleBinEnabled) {
                                SortDialogFragment.getInstance(
                                        PreferencesUtil.getListSort(context),
                                        PreferencesUtil.getAscendingSort(context),
                                        PreferencesUtil.getGroupsBeforeSort(context),
                                        PreferencesUtil.getRecycleBinBottomSort(context))
                            } else {
                                SortDialogFragment.getInstance(
                                        PreferencesUtil.getListSort(context),
                                        PreferencesUtil.getAscendingSort(context),
                                        PreferencesUtil.getGroupsBeforeSort(context))
                            }

                    sortDialogFragment.show(childFragmentManager, "sortDialog")
                }
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    fun actionNodesCallback(nodes: List<Node>,
                            menuListener: NodesActionMenuListener?) : ActionMode.Callback {

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

                    val database = Database.getInstance()

                    // Open and Edit for a single item
                    if (nodes.size == 1) {
                        // Edition
                        if (readOnly
                                || (database.isRecycleBinEnabled && nodes[0] == database.recycleBin)) {
                            menu?.removeItem(R.id.menu_edit)
                        }
                    } else {
                        menu?.removeItem(R.id.menu_open)
                        menu?.removeItem(R.id.menu_edit)
                    }

                    // Copy and Move (not for groups)
                    if (readOnly
                            || isASearchResult
                            || nodes.any { it.type == Type.GROUP }) {
                        // TODO COPY For Group
                        menu?.removeItem(R.id.menu_copy)
                        menu?.removeItem(R.id.menu_move)
                    }

                    // Deletion
                    if (readOnly
                            || (database.isRecycleBinEnabled && nodes.any { it == database.recycleBin })) {
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
                    R.id.menu_open -> menuListener.onOpenMenuClick(nodes[0])
                    R.id.menu_edit -> menuListener.onEditMenuClick(nodes[0])
                    R.id.menu_copy -> {
                        nodeActionPasteMode = PasteMode.PASTE_FROM_COPY
                        mAdapter?.unselectActionNodes()
                        val returnValue = menuListener.onCopyMenuClick(nodes)
                        nodeActionSelectionMode = false
                        returnValue
                    }
                    R.id.menu_move -> {
                        nodeActionPasteMode = PasteMode.PASTE_FROM_MOVE
                        mAdapter?.unselectActionNodes()
                        val returnValue = menuListener.onMoveMenuClick(nodes)
                        nodeActionSelectionMode = false
                        returnValue
                    }
                    R.id.menu_delete -> menuListener.onDeleteMenuClick(nodes)
                    R.id.menu_paste -> {
                        val returnValue = menuListener.onPasteMenuClick(nodeActionPasteMode, nodes)
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
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            EntryEditActivity.ADD_OR_UPDATE_ENTRY_REQUEST_CODE -> {
                if (resultCode == EntryEditActivity.ADD_ENTRY_RESULT_CODE
                        || resultCode == EntryEditActivity.UPDATE_ENTRY_RESULT_CODE) {
                    data?.getParcelableExtra<Node>(EntryEditActivity.ADD_OR_UPDATE_ENTRY_KEY)?.let { newNode ->
                        if (resultCode == EntryEditActivity.ADD_ENTRY_RESULT_CODE)
                            mAdapter?.addNode(newNode)
                        if (resultCode == EntryEditActivity.UPDATE_ENTRY_RESULT_CODE) {
                            //mAdapter.updateLastNodeRegister(newNode);
                            mainGroup?.let { mainGroup ->
                                mAdapter?.rebuildList(mainGroup)
                            }
                        }
                    } ?: Log.e(this.javaClass.name, "New node can be retrieve in Activity Result")
                }
            }
        }
    }

    fun contains(node: Node): Boolean {
        return mAdapter?.contains(node) ?: false
    }

    fun addNode(newNode: Node) {
        mAdapter?.addNode(newNode)
    }

    fun addNodes(newNodes: List<Node>) {
        mAdapter?.addNodes(newNodes)
    }

    fun updateNode(oldNode: Node, newNode: Node? = null) {
        mAdapter?.updateNode(oldNode, newNode ?: oldNode)
    }

    fun updateNodes(oldNodes: List<Node>, newNodes: List<Node>) {
        mAdapter?.updateNodes(oldNodes, newNodes)
    }

    fun removeNode(pwNode: Node) {
        mAdapter?.removeNode(pwNode)
    }

    fun removeNodes(nodes: List<Node>) {
        mAdapter?.removeNodes(nodes)
    }

    fun removeNodeAt(position: Int) {
        mAdapter?.removeNodeAt(position)
    }

    fun removeNodesAt(positions: IntArray) {
        mAdapter?.removeNodesAt(positions)
    }

    /**
     * Callback listener to redefine to do an action when a node is click
     */
    interface NodeClickListener {
        fun onNodeClick(node: Node)
        fun onNodeSelected(nodes: List<Node>): Boolean
    }

    /**
     * Menu listener to redefine to do an action in menu
     */
    interface NodesActionMenuListener {
        fun onOpenMenuClick(node: Node): Boolean
        fun onEditMenuClick(node: Node): Boolean
        fun onCopyMenuClick(nodes: List<Node>): Boolean
        fun onMoveMenuClick(nodes: List<Node>): Boolean
        fun onDeleteMenuClick(nodes: List<Node>): Boolean
        fun onPasteMenuClick(pasteMode: PasteMode?, nodes: List<Node>): Boolean
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

    companion object {

        private val TAG = ListNodesFragment::class.java.name

        private const val GROUP_KEY = "GROUP_KEY"
        private const val IS_SEARCH = "IS_SEARCH"

        fun newInstance(group: Group?, readOnly: Boolean, isASearch: Boolean): ListNodesFragment {
            val bundle = Bundle()
            if (group != null) {
                bundle.putParcelable(GROUP_KEY, group)
            }
            bundle.putBoolean(IS_SEARCH, isASearch)
            ReadOnlyHelper.putReadOnlyInBundle(bundle, readOnly)
            val listNodesFragment = ListNodesFragment()
            listNodesFragment.arguments = bundle
            return listNodesFragment
        }
    }
}
