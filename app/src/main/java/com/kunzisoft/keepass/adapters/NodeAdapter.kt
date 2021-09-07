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
package com.kunzisoft.keepass.adapters

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.SortNodeEnum
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.database.element.node.NodeVersionedInterface
import com.kunzisoft.keepass.database.element.node.Type
import com.kunzisoft.keepass.database.element.template.TemplateField
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.otp.OtpType
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.ClipboardHelper
import com.kunzisoft.keepass.view.setTextSize
import com.kunzisoft.keepass.view.strikeOut
import java.util.*

/**
 * Create node list adapter with contextMenu or not
 * @param context Context to use
 */
class NodeAdapter (private val context: Context,
                   private val database: Database)
    : RecyclerView.Adapter<NodeAdapter.NodeViewHolder>() {

    private var mNodeComparator: Comparator<NodeVersionedInterface<Group>>? = null
    private val mNodeSortedListCallback: NodeSortedListCallback
    private val mNodeSortedList: SortedList<Node>
    private val mInflater: LayoutInflater = LayoutInflater.from(context)

    private var mCalculateViewTypeTextSize = Array(2) { true } // number of view type
    private var mTextSizeUnit: Int = TypedValue.COMPLEX_UNIT_PX
    private var mPrefSizeMultiplier: Float = 0F
    private var mSubtextDefaultDimension: Float = 0F
    private var mInfoTextDefaultDimension: Float = 0F
    private var mNumberChildrenTextDefaultDimension: Float = 0F
    private var mIconDefaultDimension: Float = 0F

    private var mShowUserNames: Boolean = true
    private var mShowNumberEntries: Boolean = true
    private var mShowOTP: Boolean = false
    private var mShowUUID: Boolean = false
    private var mEntryFilters = arrayOf<Group.ChildFilter>()

    private var mActionNodesList = LinkedList<Node>()
    private var mNodeClickCallback: NodeClickCallback? = null
    private var mClipboardHelper = ClipboardHelper(context)

    @ColorInt
    private val mContentSelectionColor: Int
    @ColorInt
    private val mIconGroupColor: Int
    @ColorInt
    private val mIconEntryColor: Int

    /**
     * Determine if the adapter contains or not any element
     * @return true if the list is empty
     */
    val isEmpty: Boolean
        get() = mNodeSortedList.size() <= 0

    init {
        this.mIconDefaultDimension = context.resources.getDimension(R.dimen.list_icon_size_default)

        assignPreferences()

        this.mNodeSortedListCallback = NodeSortedListCallback()
        this.mNodeSortedList = SortedList(Node::class.java, mNodeSortedListCallback)

        // Color of content selection
        this.mContentSelectionColor = ContextCompat.getColor(context, R.color.white)
        // Retrieve the color to tint the icon
        val taTextColorPrimary = context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.textColorPrimary))
        this.mIconGroupColor = taTextColorPrimary.getColor(0, Color.BLACK)
        taTextColorPrimary.recycle()
        // In two times to fix bug compilation
        val taTextColor = context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.textColor))
        this.mIconEntryColor = taTextColor.getColor(0, Color.BLACK)
        taTextColor.recycle()
    }

    private fun assignPreferences() {
        this.mPrefSizeMultiplier = PreferencesUtil.getListTextSize(context)

        notifyChangeSort(
                PreferencesUtil.getListSort(context),
                        SortNodeEnum.SortNodeParameters(
                            PreferencesUtil.getAscendingSort(context),
                            PreferencesUtil.getGroupsBeforeSort(context),
                            PreferencesUtil.getRecycleBinBottomSort(context)
                        )
                )

        this.mShowUserNames = PreferencesUtil.showUsernamesListEntries(context)
        this.mShowNumberEntries = PreferencesUtil.showNumberEntries(context)
        this.mShowOTP = PreferencesUtil.showOTPToken(context)
        this.mShowUUID = PreferencesUtil.showUUID(context)

        this.mEntryFilters = Group.ChildFilter.getDefaults(context)

        // Reinit textSize for all view type
        mCalculateViewTypeTextSize.forEachIndexed { index, _ -> mCalculateViewTypeTextSize[index] = true }
    }

    /**
     * Rebuild the list by clear and build children from the group
     */
    fun rebuildList(group: Group) {
        assignPreferences()
        mNodeSortedList.replaceAll(group.getFilteredChildren(mEntryFilters))
    }

    private inner class NodeSortedListCallback: SortedListAdapterCallback<Node>(this) {
        override fun compare(item1: Node, item2: Node): Int {
            return mNodeComparator!!.compare(item1, item2)
        }

        override fun areContentsTheSame(oldItem: Node, newItem: Node): Boolean {
            var typeContentTheSame = true
            if (oldItem is Entry && newItem is Entry) {
                typeContentTheSame = oldItem.getVisualTitle() == newItem.getVisualTitle()
                        && oldItem.username == newItem.username
                        && oldItem.getOtpElement() == newItem.getOtpElement()
                        && oldItem.containsAttachment() == newItem.containsAttachment()
            } else if (oldItem is Group && newItem is Group) {
                typeContentTheSame = oldItem.numberOfChildEntries == newItem.numberOfChildEntries
            }
            return typeContentTheSame
                    && oldItem.nodeId == newItem.nodeId
                    && oldItem.type == newItem.type
                    && oldItem.title == newItem.title
                    && oldItem.icon == newItem.icon
                    && oldItem.isCurrentlyExpires == newItem.isCurrentlyExpires
        }

        override fun areItemsTheSame(item1: Node, item2: Node): Boolean {
            return item1 == item2
        }
    }

    fun contains(node: Node): Boolean {
        return mNodeSortedList.indexOf(node) != SortedList.INVALID_POSITION
    }

    /**
     * Add a node to the list
     * @param node Node to add
     */
    fun addNode(node: Node) {
        mNodeSortedList.add(node)
    }

    /**
     * Add nodes to the list
     * @param nodes Nodes to add
     */
    fun addNodes(nodes: List<Node>) {
        mNodeSortedList.addAll(nodes)
    }

    /**
     * Remove a node in the list
     * @param node Node to delete
     */
    fun removeNode(node: Node) {
        mNodeSortedList.remove(node)
    }

    /**
     * Remove nodes in the list
     * @param nodes Nodes to delete
     */
    fun removeNodes(nodes: List<Node>) {
        nodes.forEach { node ->
            mNodeSortedList.remove(node)
        }
    }

    /**
     * Remove a node at [position] in the list
     */
    fun removeNodeAt(position: Int) {
        mNodeSortedList.removeItemAt(position)
        // Refresh all the next items
        notifyItemRangeChanged(position, mNodeSortedList.size() - position)
    }

    /**
     * Remove nodes in the list by [positions]
     * Note : algorithm remove the higher position at each iteration
     */
    fun removeNodesAt(positions: IntArray) {
        val positionsSortDescending = positions.toMutableList()
        positionsSortDescending.sortDescending()
        positionsSortDescending.forEach {
            removeNodeAt(it)
        }
    }

    /**
     * Update a node in the list
     * @param oldNode Node before the update
     * @param newNode Node after the update
     */
    fun updateNode(oldNode: Node, newNode: Node) {
        mNodeSortedList.beginBatchedUpdates()
        mNodeSortedList.remove(oldNode)
        mNodeSortedList.add(newNode)
        mNodeSortedList.endBatchedUpdates()
    }

    /**
     * Update nodes in the list
     * @param oldNodes Nodes before the update
     * @param newNodes Node after the update
     */
    fun updateNodes(oldNodes: List<Node>, newNodes: List<Node>) {
        mNodeSortedList.beginBatchedUpdates()
        oldNodes.forEach { oldNode ->
            mNodeSortedList.remove(oldNode)
        }
        mNodeSortedList.addAll(newNodes)
        mNodeSortedList.endBatchedUpdates()
    }

    fun indexOf(node: Node): Int {
        return mNodeSortedList.indexOf(node)
    }

    fun notifyNodeChanged(node: Node) {
        notifyItemChanged(mNodeSortedList.indexOf(node))
    }

    fun setActionNodes(actionNodes: List<Node>) {
        this.mActionNodesList.apply {
            clear()
            addAll(actionNodes)
        }
    }

    fun unselectActionNodes() {
        mActionNodesList.forEach {
            notifyItemChanged(mNodeSortedList.indexOf(it))
        }
        this.mActionNodesList.apply {
            clear()
        }
    }

    /**
     * Notify a change sort of the list
     */
    fun notifyChangeSort(sortNodeEnum: SortNodeEnum,
                         sortNodeParameters: SortNodeEnum.SortNodeParameters) {
        this.mNodeComparator = sortNodeEnum.getNodeComparator(database, sortNodeParameters)
    }

    override fun getItemViewType(position: Int): Int {
        return mNodeSortedList.get(position).type.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NodeViewHolder {
        val view: View = if (viewType == Type.GROUP.ordinal) {
            mInflater.inflate(R.layout.item_list_nodes_group, parent, false)
        } else {
            mInflater.inflate(R.layout.item_list_nodes_entry, parent, false)
        }
        val nodeViewHolder = NodeViewHolder(view)
        mInfoTextDefaultDimension = nodeViewHolder.text.textSize
        mSubtextDefaultDimension = nodeViewHolder.subText.textSize
        nodeViewHolder.numberChildren?.let {
            mNumberChildrenTextDefaultDimension = it.textSize
        }
        return nodeViewHolder
    }

    override fun onBindViewHolder(holder: NodeViewHolder, position: Int) {
        val subNode = mNodeSortedList.get(position)

        // Node selection
        holder.container.isSelected = mActionNodesList.contains(subNode)

        // Assign image
        val iconColor = if (holder.container.isSelected)
            mContentSelectionColor
        else when (subNode.type) {
            Type.GROUP -> mIconGroupColor
            Type.ENTRY -> mIconEntryColor
        }
        holder.imageIdentifier?.setColorFilter(iconColor)
        holder.icon.apply {
            database.iconDrawableFactory.assignDatabaseIcon(this, subNode.icon, iconColor)
            // Relative size of the icon
            layoutParams?.apply {
                height = (mIconDefaultDimension * mPrefSizeMultiplier).toInt()
                width = (mIconDefaultDimension * mPrefSizeMultiplier).toInt()
            }
        }

        // Assign text
        holder.text.apply {
            text = subNode.title
            setTextSize(mTextSizeUnit, mInfoTextDefaultDimension, mPrefSizeMultiplier)
            strikeOut(subNode.isCurrentlyExpires)
        }
        // Add subText with username
        holder.subText.apply {
            text = ""
            strikeOut(subNode.isCurrentlyExpires)
            visibility = View.GONE
        }
        // Add meta text to show UUID
        holder.meta.apply {
            text = subNode.nodeId.toString()
            visibility = if (mShowUUID) View.VISIBLE else View.GONE
        }

        // Specific elements for entry
        if (subNode.type == Type.ENTRY) {
            val entry = subNode as Entry
            database.startManageEntry(entry)

            holder.text.text = entry.getVisualTitle()
            holder.subText.apply {
                val username = entry.username
                if (mShowUserNames && username.isNotEmpty()) {
                    visibility = View.VISIBLE
                    text = username
                    setTextSize(mTextSizeUnit, mSubtextDefaultDimension, mPrefSizeMultiplier)
                }
            }

            val otpElement = entry.getOtpElement()
            holder.otpContainer?.removeCallbacks(holder.otpRunnable)
            if (otpElement != null
                && mShowOTP
                && otpElement.token.isNotEmpty()) {

                // Execute runnable to show progress
                holder.otpRunnable.action = {
                    populateOtpView(holder, otpElement)
                }
                if (otpElement.type == OtpType.TOTP) {
                    holder.otpRunnable.postDelayed()
                }
                populateOtpView(holder, otpElement)

                holder.otpContainer?.visibility = View.VISIBLE
            } else {
                holder.otpContainer?.visibility = View.GONE
            }
            holder.attachmentIcon?.visibility =
                    if (entry.containsAttachment()) View.VISIBLE else View.GONE

            database.stopManageEntry(entry)
        }

        // Add number of entries in groups
        if (subNode.type == Type.GROUP) {
            if (mShowNumberEntries) {
                holder.numberChildren?.apply {
                    text = (subNode as Group)
                            .numberOfChildEntries
                            .toString()
                    setTextSize(mTextSizeUnit, mNumberChildrenTextDefaultDimension, mPrefSizeMultiplier)
                    visibility = View.VISIBLE
                }
            } else {
                holder.numberChildren?.visibility = View.GONE
            }
        }

        // Assign click
        holder.container.setOnClickListener {
            mNodeClickCallback?.onNodeClick(database, subNode)
        }
        holder.container.setOnLongClickListener {
            mNodeClickCallback?.onNodeLongClick(database, subNode) ?: false
        }
    }

    private fun populateOtpView(holder: NodeViewHolder?, otpElement: OtpElement?) {
        when (otpElement?.type) {
            OtpType.HOTP -> {
                holder?.otpProgress?.apply {
                    max = 100
                    progress = 100
                }
            }
            OtpType.TOTP -> {
                holder?.otpProgress?.apply {
                    max = otpElement.period
                    progress = otpElement.secondsRemaining
                }
            }
        }
        holder?.otpToken?.text = otpElement?.token
        holder?.otpContainer?.setOnClickListener {
            otpElement?.token?.let { token ->
                Toast.makeText(
                        context,
                        context.getString(R.string.copy_field,
                                TemplateField.getLocalizedName(context, TemplateField.LABEL_TOKEN)),
                        Toast.LENGTH_LONG
                ).show()
                mClipboardHelper.copyToClipboard(token)
            }
        }
    }

    class OtpRunnable(val view: View?): Runnable {

        var action: (() -> Unit)? = null

        override fun run() {
            action?.invoke()
            postDelayed()
        }

        fun postDelayed() {
            view?.postDelayed(this, 1000)
        }
    }

    override fun getItemCount(): Int {
        return mNodeSortedList.size()
    }

    /**
     * Assign a listener when a node is clicked
     */
    fun setOnNodeClickListener(nodeClickCallback: NodeClickCallback?) {
        this.mNodeClickCallback = nodeClickCallback
    }

    /**
     * Callback listener to redefine to do an action when a node is click
     */
    interface NodeClickCallback {
        fun onNodeClick(database: Database, node: Node)
        fun onNodeLongClick(database: Database, node: Node): Boolean
    }

    class NodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var container: View = itemView.findViewById(R.id.node_container)
        var imageIdentifier: ImageView? = itemView.findViewById(R.id.node_image_identifier)
        var icon: ImageView = itemView.findViewById(R.id.node_icon)
        var text: TextView = itemView.findViewById(R.id.node_text)
        var subText: TextView = itemView.findViewById(R.id.node_subtext)
        var meta: TextView = itemView.findViewById(R.id.node_meta)
        var otpContainer: ViewGroup? = itemView.findViewById(R.id.node_otp_container)
        var otpProgress: ProgressBar? = itemView.findViewById(R.id.node_otp_progress)
        var otpToken: TextView? = itemView.findViewById(R.id.node_otp_token)
        var otpRunnable: OtpRunnable = OtpRunnable(otpContainer)
        var numberChildren: TextView? = itemView.findViewById(R.id.node_child_numbers)
        var attachmentIcon: ImageView? = itemView.findViewById(R.id.node_attachment_icon)
    }

    companion object {
        private val TAG = NodeAdapter::class.java.name
    }
}
