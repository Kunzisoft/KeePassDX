/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.adapters

import android.content.Context
import android.graphics.Color
import android.support.v7.util.SortedList
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.util.SortedListAdapterCallback
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.SortNodeEnum
import com.kunzisoft.keepass.database.element.*
import com.kunzisoft.keepass.icons.assignDatabaseIcon
import com.kunzisoft.keepass.settings.PreferencesUtil

class NodeAdapter
/**
 * Create node list adapter with contextMenu or not
 * @param context Context to use
 */
(private val context: Context, private val menuInflater: MenuInflater)
    : RecyclerView.Adapter<NodeAdapter.NodeViewHolder>() {

    private val nodeSortedList: SortedList<NodeVersioned>
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var textSize: Float = 0.toFloat()
    private var subtextSize: Float = 0.toFloat()
    private var iconSize: Float = 0.toFloat()
    private var listSort: SortNodeEnum = SortNodeEnum.DB
    private var ascendingSort: Boolean = true
    private var groupsBeforeSort: Boolean = true
    private var recycleBinBottomSort: Boolean = true
    private var showUserNames: Boolean = false

    private var nodeClickCallback: NodeClickCallback? = null
    private var nodeMenuListener: NodeMenuListener? = null
    private var activateContextMenu: Boolean = false
    private var readOnly: Boolean = false
    private var isASearchResult: Boolean = false

    private val mDatabase: Database

    private val iconGroupColor: Int
    private val iconEntryColor: Int

    /**
     * Determine if the adapter contains or not any element
     * @return true if the list is empty
     */
    val isEmpty: Boolean
        get() = nodeSortedList.size() <= 0

    init {
        assignPreferences()
        this.activateContextMenu = false
        this.readOnly = false
        this.isASearchResult = false

        this.nodeSortedList = SortedList(NodeVersioned::class.java, object : SortedListAdapterCallback<NodeVersioned>(this) {
            override fun compare(item1: NodeVersioned, item2: NodeVersioned): Int {
                return listSort.getNodeComparator(ascendingSort, groupsBeforeSort, recycleBinBottomSort).compare(item1, item2)
            }

            override fun areContentsTheSame(oldItem: NodeVersioned, newItem: NodeVersioned): Boolean {
                return oldItem.title == newItem.title && oldItem.icon == newItem.icon
            }

            override fun areItemsTheSame(item1: NodeVersioned, item2: NodeVersioned): Boolean {
                return item1 == item2
            }
        })

        // Database
        this.mDatabase = Database.getInstance()

        // Retrieve the color to tint the icon
        val taTextColorPrimary = context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.textColorPrimary))
        this.iconGroupColor = taTextColorPrimary.getColor(0, Color.BLACK)
        taTextColorPrimary.recycle()
        // In two times to fix bug compilation
        val taTextColor = context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.textColor))
        this.iconEntryColor = taTextColor.getColor(0, Color.BLACK)
        taTextColor.recycle()
    }

    fun setReadOnly(readOnly: Boolean) {
        this.readOnly = readOnly
    }

    fun setIsASearchResult(isASearchResult: Boolean) {
        this.isASearchResult = isASearchResult
    }

    fun setActivateContextMenu(activate: Boolean) {
        this.activateContextMenu = activate
    }

    private fun assignPreferences() {
        val textSizeDefault = java.lang.Float.parseFloat(context.getString(R.string.list_size_default))
        this.textSize = PreferencesUtil.getListTextSize(context)
        this.subtextSize = context.resources.getInteger(R.integer.list_small_size_default) * textSize / textSizeDefault
        // Retrieve the icon size
        val iconDefaultSize = context.resources.getDimension(R.dimen.list_icon_size_default)
        this.iconSize = iconDefaultSize * textSize / textSizeDefault
        this.listSort = PreferencesUtil.getListSort(context)
        this.ascendingSort = PreferencesUtil.getAscendingSort(context)
        this.groupsBeforeSort = PreferencesUtil.getGroupsBeforeSort(context)
        this.recycleBinBottomSort = PreferencesUtil.getRecycleBinBottomSort(context)
        this.showUserNames = PreferencesUtil.showUsernamesListEntries(context)
    }

    /**
     * Rebuild the list by clear and build children from the group
     */
    fun rebuildList(group: GroupVersioned) {
        this.nodeSortedList.clear()
        assignPreferences()
        // TODO verify sort
        try {
            this.nodeSortedList.addAll(group.getChildren())
        } catch (e: Exception) {
            Log.e(TAG, "Can't add node elements to the list", e)
            Toast.makeText(context, "Can't add node elements to the list : " + e.message, Toast.LENGTH_LONG).show()
        }

    }

    /**
     * Add a node to the list
     * @param node Node to add
     */
    fun addNode(node: NodeVersioned) {
        nodeSortedList.add(node)
    }

    /**
     * Remove a node in the list
     * @param node Node to delete
     */
    fun removeNode(node: NodeVersioned) {
        nodeSortedList.remove(node)
    }

    /**
     * Update a node in the list
     * @param oldNode Node before the update
     * @param newNode Node after the update
     */
    fun updateNode(oldNode: NodeVersioned, newNode: NodeVersioned) {
        nodeSortedList.beginBatchedUpdates()
        nodeSortedList.remove(oldNode)
        nodeSortedList.add(newNode)
        nodeSortedList.endBatchedUpdates()
    }

    /**
     * Notify a change sort of the list
     */
    fun notifyChangeSort(sortNodeEnum: SortNodeEnum, ascending: Boolean, groupsBefore: Boolean) {
        this.listSort = sortNodeEnum
        this.ascendingSort = ascending
        this.groupsBeforeSort = groupsBefore
    }

    override fun getItemViewType(position: Int): Int {
        return nodeSortedList.get(position).type.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NodeViewHolder {
        val view: View = if (viewType == Type.GROUP.ordinal) {
            inflater.inflate(R.layout.item_list_nodes_group, parent, false)
        } else {
            inflater.inflate(R.layout.item_list_nodes_entry, parent, false)
        }
        return NodeViewHolder(view)
    }

    override fun onBindViewHolder(holder: NodeViewHolder, position: Int) {
        val subNode = nodeSortedList.get(position)
        // Assign image
        val iconColor = when (subNode.type) {
            Type.GROUP -> iconGroupColor
            Type.ENTRY -> iconEntryColor
        }
        holder.icon.assignDatabaseIcon(mDatabase.drawFactory, subNode.icon, iconColor)
        // Assign text
        holder.text.text = subNode.title
        // Assign click
        holder.container.setOnClickListener { nodeClickCallback?.onNodeClick(subNode) }
        // Context menu
        if (activateContextMenu) {
            holder.container.setOnCreateContextMenuListener(
                    ContextMenuBuilder(menuInflater, subNode, readOnly, isASearchResult, nodeMenuListener))
        }

        // Add username
        holder.subText.text = ""
        holder.subText.visibility = View.GONE
        if (subNode.type == Type.ENTRY) {
            val entry = subNode as EntryVersioned

            mDatabase.startManageEntry(entry)

            holder.text.text = entry.getVisualTitle()

            val username = entry.username
            if (showUserNames && username.isNotEmpty()) {
                holder.subText.visibility = View.VISIBLE
                holder.subText.text = username
            }

            mDatabase.stopManageEntry(entry)
        }

        // Assign image and text size
        // Relative size of the icon
        holder.icon.layoutParams?.height = iconSize.toInt()
        holder.icon.layoutParams?.width = iconSize.toInt()
        holder.text.textSize = textSize
        holder.subText.textSize = subtextSize
    }

    override fun getItemCount(): Int {
        return nodeSortedList.size()
    }

    /**
     * Assign a listener when a node is clicked
     */
    fun setOnNodeClickListener(nodeClickCallback: NodeClickCallback?) {
        this.nodeClickCallback = nodeClickCallback
    }

    /**
     * Assign a listener when an element of menu is clicked
     */
    fun setNodeMenuListener(nodeMenuListener: NodeMenuListener?) {
        this.nodeMenuListener = nodeMenuListener
    }

    /**
     * Callback listener to redefine to do an action when a node is click
     */
    interface NodeClickCallback {
        fun onNodeClick(node: NodeVersioned)
    }

    /**
     * Menu listener to redefine to do an action in menu
     */
    interface NodeMenuListener {
        fun onOpenMenuClick(node: NodeVersioned): Boolean
        fun onEditMenuClick(node: NodeVersioned): Boolean
        fun onCopyMenuClick(node: NodeVersioned): Boolean
        fun onMoveMenuClick(node: NodeVersioned): Boolean
        fun onDeleteMenuClick(node: NodeVersioned): Boolean
    }

    /**
     * Utility class for menu listener
     */
    private class ContextMenuBuilder(val menuInflater: MenuInflater,
                                     val node: NodeVersioned,
                                     val readOnly: Boolean,
                                     val isASearchResult: Boolean,
                                     val menuListener: NodeMenuListener?)
        : View.OnCreateContextMenuListener {

        private val mOnMyActionClickListener = MenuItem.OnMenuItemClickListener { item ->
            if (menuListener == null)
                return@OnMenuItemClickListener false
            when (item.itemId) {
                R.id.menu_open -> menuListener.onOpenMenuClick(node)
                R.id.menu_edit -> menuListener.onEditMenuClick(node)
                R.id.menu_copy -> menuListener.onCopyMenuClick(node)
                R.id.menu_move -> menuListener.onMoveMenuClick(node)
                R.id.menu_delete -> menuListener.onDeleteMenuClick(node)
                else -> false
            }
        }

        override fun onCreateContextMenu(contextMenu: ContextMenu?,
                                         view: View?,
                                         contextMenuInfo: ContextMenu.ContextMenuInfo?) {
            menuInflater.inflate(R.menu.node_menu, contextMenu)

            // Opening
            var menuItem = contextMenu?.findItem(R.id.menu_open)
            menuItem?.setOnMenuItemClickListener(mOnMyActionClickListener)

            val database = Database.getInstance()

            // Edition
            if (readOnly || node == database.recycleBin) {
                contextMenu?.removeItem(R.id.menu_edit)
            } else {
                menuItem = contextMenu?.findItem(R.id.menu_edit)
                menuItem?.setOnMenuItemClickListener(mOnMyActionClickListener)
            }

            // Copy (not for group)
            if (readOnly
                    || isASearchResult
                    || node == database.recycleBin
                    || node.type == Type.GROUP) {
                // TODO COPY For Group
                contextMenu?.removeItem(R.id.menu_copy)
            } else {
                menuItem = contextMenu?.findItem(R.id.menu_copy)
                menuItem?.setOnMenuItemClickListener(mOnMyActionClickListener)
            }

            // Move
            if (readOnly
                    || isASearchResult
                    || node == database.recycleBin) {
                contextMenu?.removeItem(R.id.menu_move)
            } else {
                menuItem = contextMenu?.findItem(R.id.menu_move)
                menuItem?.setOnMenuItemClickListener(mOnMyActionClickListener)
            }

            // Deletion
            if (readOnly || node == database.recycleBin) {
                contextMenu?.removeItem(R.id.menu_delete)
            } else {
                menuItem = contextMenu?.findItem(R.id.menu_delete)
                menuItem?.setOnMenuItemClickListener(mOnMyActionClickListener)
            }
        }
    }

    class NodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var container: View = itemView.findViewById(R.id.node_container)
        var icon: ImageView = itemView.findViewById(R.id.node_icon)
        var text: TextView = itemView.findViewById(R.id.node_text)
        var subText: TextView = itemView.findViewById(R.id.node_subtext)
    }

    companion object {
        private val TAG = NodeAdapter::class.java.name
    }
}
