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
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.SortNodeEnum
import com.kunzisoft.keepass.database.element.Tag
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.NodeType
import com.kunzisoft.keepass.icons.IconDrawableFactory
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.GroupInfo
import com.kunzisoft.keepass.model.SortedEntryInfo
import com.kunzisoft.keepass.model.SortedGroupInfo
import com.kunzisoft.keepass.model.SortedNodeInfo
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.view.OtpDisplayView
import com.kunzisoft.keepass.view.setTextSize
import com.kunzisoft.keepass.view.strikeOut

/**
 * Create node list adapter.
 * @param context Context to use
 */
class NodesAdapter(
    private val context: Context,
    private val iconDrawableFactory: IconDrawableFactory,
    private var sortDatabaseParameters: SortNodeEnum.SortDatabaseParameters
) : RecyclerView.Adapter<NodesAdapter.NodeViewHolder>() {

    private var sortNode: SortNodeEnum = SortNodeEnum.DB
    private var sortNodeParameters: SortNodeEnum.SortNodeParameters = SortNodeEnum.SortNodeParameters()
    private var mNodeComparator: Comparator<SortedNodeInfo> = buildNodeComparator()
    private val mNodeSortedListCallback: NodeSortedListCallback
    private val mNodeSortedList: SortedList<SortedNodeInfo>
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private var mCalculateViewTypeTextSize = Array(2) { true } // number of view type
    private var mTextSizeUnit: Int = TypedValue.COMPLEX_UNIT_PX
    private var mPrefSizeMultiplier: Float = 0F
    private var mTextDefaultDimension: Float = 0F
    private var mSubTextDefaultDimension: Float = 0F
    private var mMetaTextDefaultDimension: Float = 0F
    private var mNumberChildrenTextDefaultDimension: Float = 0F
    private var mIconDefaultDimension: Float = 0F

    private var mShowEntryColors: Boolean = true
    private var mShowUserNames: Boolean = true
    private var mShowNumberEntries: Boolean = true
    private var mShowTags: Boolean = false
    private var mShowUUID: Boolean = false
    private var mRecursiveNumberEntries: Boolean = false
    private var mOldVirtualGroup = false
    private var mVirtualGroup = false

    private var mActionNodesList = mutableListOf<SortedNodeInfo>()
    private var mActionNodeIds = mutableSetOf<NodeId<*>>()
    private var mNodeClickCallback: NodeClickCallback? = null

    @ColorInt
    private val mColorSurfaceContainer: Int
    @ColorInt
    private val mTextColorPrimary: Int
    @ColorInt
    private val mTextColor: Int
    @ColorInt
    private val mTextColorSecondary: Int
    @ColorInt
    private val mColorSecondary: Int
    @ColorInt
    private val mColorOnSecondary: Int

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
        this.mNodeSortedList = SortedList(SortedNodeInfo::class.java, mNodeSortedListCallback)

        context.obtainStyledAttributes(intArrayOf(R.attr.colorSurfaceContainer)).also { taColorSurfaceContainer ->
            this.mColorSurfaceContainer = taColorSurfaceContainer.getColor(0, Color.BLACK)
        }.recycle()
        // Retrieve the color to tint the icon
        context.obtainStyledAttributes(intArrayOf(android.R.attr.textColorPrimary)).also { taTextColorPrimary ->
            this.mTextColorPrimary = taTextColorPrimary.getColor(0, Color.BLACK)
        }.recycle()
        // To get text color
        context.obtainStyledAttributes(intArrayOf(android.R.attr.textColor)).also { taTextColor ->
            this.mTextColor = taTextColor.getColor(0, Color.BLACK)
        }.recycle()
        // To get text color secondary
        context.obtainStyledAttributes(intArrayOf(android.R.attr.textColorSecondary)).also { taTextColorSecondary ->
            this.mTextColorSecondary = taTextColorSecondary.getColor(0, Color.BLACK)
        }.recycle()
        // To get background color for selection
        context.obtainStyledAttributes(intArrayOf(R.attr.colorSecondary)).also { taColorSecondary ->
            this.mColorSecondary = taColorSecondary.getColor(0, Color.GRAY)
        }.recycle()
        // To get text color for selection
        context.obtainStyledAttributes(intArrayOf(R.attr.colorOnSecondary)).also { taColorOnSecondary ->
            this.mColorOnSecondary = taColorOnSecondary.getColor(0, Color.WHITE)
        }.recycle()
    }

    private fun assignPreferences() {
        this.mPrefSizeMultiplier = PreferencesUtil.getListTextSize(context)

        notifyChangeSort(
            sortNodeEnum = PreferencesUtil.getListSort(context),
            sortDatabaseParameters = sortDatabaseParameters,
            sortNodeParameters = SortNodeEnum.SortNodeParameters(
                ascending = PreferencesUtil.getAscendingSort(context),
                groupsBefore = PreferencesUtil.getGroupsBeforeSort(context),
                recycleBinBottom = PreferencesUtil.getRecycleBinBottomSort(context)
            )
        )

        this.mShowEntryColors = PreferencesUtil.showEntryColors(context)
        this.mShowUserNames = PreferencesUtil.showUsernamesListEntries(context)
        this.mShowNumberEntries = PreferencesUtil.showNumberEntries(context)
        this.mShowTags = PreferencesUtil.showTags(context)
        this.mShowUUID = PreferencesUtil.showUUID(context)
        this.mRecursiveNumberEntries = PreferencesUtil.recursiveNumberEntries(context)

        // Reinit textSize for all view type
        mCalculateViewTypeTextSize.forEachIndexed { index, _ -> mCalculateViewTypeTextSize[index] = true }
    }

    /**
     * Rebuild the list by clear and build children from the list of [nodes]
     * @param isSearch If the list is a search list
     */
    fun rebuildList(
        nodes: List<SortedNodeInfo>,
        isSearch: Boolean = false
    ) {
        mOldVirtualGroup = mVirtualGroup
        mVirtualGroup = isSearch
        assignPreferences()
        mNodeSortedList.replaceAll(nodes)
    }

    private inner class NodeSortedListCallback: SortedListAdapterCallback<SortedNodeInfo>(this) {
        override fun compare(item1: SortedNodeInfo, item2: SortedNodeInfo): Int {
            return mNodeComparator.compare(item1, item2)
        }

        override fun areContentsTheSame(oldItem: SortedNodeInfo, newItem: SortedNodeInfo): Boolean {
            if (mOldVirtualGroup != mVirtualGroup)
                return false
            var typeContentTheSame = true
            if (oldItem is SortedEntryInfo && newItem is SortedEntryInfo) {
                typeContentTheSame = oldItem.getVisualTitle() == newItem.getVisualTitle()
                        && oldItem.username == newItem.username
                        && oldItem.backgroundColor == newItem.backgroundColor
                        && oldItem.foregroundColor == newItem.foregroundColor
                        && oldItem.otpModel == newItem.otpModel
                        && oldItem.containsAttachment() == newItem.containsAttachment()
            } else if (oldItem is SortedGroupInfo && newItem is SortedGroupInfo) {
                typeContentTheSame = oldItem.numberOfChildEntries == newItem.numberOfChildEntries
                        && oldItem.notes == newItem.notes
            }
            return typeContentTheSame
                    && oldItem.nodeId == newItem.nodeId
                    && oldItem::class == newItem::class
                    && oldItem.title == newItem.title
                    && oldItem.icon == newItem.icon
                    && oldItem.creationTime == newItem.creationTime
                    && oldItem.lastModificationTime == newItem.lastModificationTime
                    && oldItem.lastAccessTime == newItem.lastAccessTime
                    && oldItem.expiryTime == newItem.expiryTime
                    && oldItem.expires == newItem.expires
                    && oldItem.isCurrentlyExpires == newItem.isCurrentlyExpires
                    && oldItem.tags == newItem.tags
                    && oldItem.path == newItem.path
        }

        override fun areItemsTheSame(item1: SortedNodeInfo, item2: SortedNodeInfo): Boolean {
            return item1.nodeId == item2.nodeId
        }
    }

    fun notifyNodeChanged(node: SortedNodeInfo) {
        for (i in 0 until mNodeSortedList.size()) {
            if (mNodeSortedList.get(i).nodeId == node.nodeId) {
                notifyItemChanged(i)
                break
            }
        }
    }

    fun setActionNodes(actionNodes: List<SortedNodeInfo>) {
        val oldIds = mActionNodeIds.toSet()
        val newIds = actionNodes.map { it.nodeId }.toSet()

        val nodesToUpdate = mutableSetOf<SortedNodeInfo>()
        // Items that were selected and now are not
        mActionNodesList.forEach { if (it.nodeId !in newIds) nodesToUpdate.add(it) }
        // Items that were not selected and now are
        actionNodes.forEach { if (it.nodeId !in oldIds) nodesToUpdate.add(it) }

        this.mActionNodesList.apply {
            clear()
            addAll(actionNodes)
        }
        this.mActionNodeIds.apply {
            clear()
            addAll(newIds)
        }
        nodesToUpdate.forEach {
            notifyNodeChanged(it)
        }
    }

    fun unselectActionNodes() {
        setActionNodes(listOf())
    }

    fun buildNodeComparator(): Comparator<SortedNodeInfo> {
        return this.sortNode.getNodeComparator(
            this.sortDatabaseParameters,
            this.sortNodeParameters
        )
    }

    /**
     * Notify a change sort of the list
     */
    fun notifyChangeSort(
        sortNodeEnum: SortNodeEnum? = null,
        sortDatabaseParameters: SortNodeEnum.SortDatabaseParameters? = null,
        sortNodeParameters: SortNodeEnum.SortNodeParameters? = null
    ) {
        this.sortNode = sortNodeEnum ?: this.sortNode
        this.sortDatabaseParameters = sortDatabaseParameters ?: this.sortDatabaseParameters
        this.sortNodeParameters = sortNodeParameters ?: this.sortNodeParameters
        this.mNodeComparator = buildNodeComparator()
    }

    override fun getItemViewType(position: Int): Int {
        return when (mNodeSortedList.get(position)) {
            is GroupInfo -> NodeType.GROUP.ordinal
            else -> NodeType.ENTRY.ordinal
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NodeViewHolder {
        val view: View = if (viewType == NodeType.GROUP.ordinal) {
            mInflater.inflate(R.layout.item_list_nodes_group, parent, false)
        } else {
            mInflater.inflate(R.layout.item_list_nodes_entry, parent, false)
        }
        val nodeViewHolder = NodeViewHolder(view)
        mTextDefaultDimension = nodeViewHolder.text.textSize
        mSubTextDefaultDimension = nodeViewHolder.subText?.textSize ?: mSubTextDefaultDimension
        mMetaTextDefaultDimension = nodeViewHolder.meta.textSize
        nodeViewHolder.numberChildren?.let {
            mNumberChildrenTextDefaultDimension = it.textSize
        }
        return nodeViewHolder
    }

    override fun onBindViewHolder(holder: NodeViewHolder, position: Int) {
        val node = mNodeSortedList.get(position)

        // Node selection
        val isSelected = mActionNodeIds.contains(node.nodeId)
        holder.container.isSelected = isSelected

        // Assign text
        holder.text.apply {
            text = node.title
            setTextSize(mTextSizeUnit, mTextDefaultDimension, mPrefSizeMultiplier)
            strikeOut(node.isCurrentlyExpires)
        }
        // Tags
        holder.tags.apply {
            val tags = node.tags
            if (mShowTags) {
                val tagsAdapter = TagsAdapter(this.context, TagsAdapter.TagViewType.SMALL)
                layoutManager = LinearLayoutManager(
                    context,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
                adapter = tagsAdapter
                tagsAdapter.setTags(tags)
                tagsAdapter.toggleSelection(isSelected)
                tagsAdapter.onItemClickListener = object : TagsAdapter.OnItemClickListener {
                    override fun onItemClick(item: Tag) {
                        mNodeClickCallback?.onNodeClick(node)
                    }

                    override fun onItemLongClick(item: Tag): Boolean {
                        mNodeClickCallback?.onNodeLongClick(node)
                        return true
                    }
                }
            }
            visibility = if (tags.isNotEmpty()) View.VISIBLE else View.GONE
        }
        // Add meta text to show UUID
        holder.meta.apply {
            val nodeId = node.nodeId.toVisualString()
            if (mShowUUID && nodeId != null) {
                text = nodeId
                setTextSize(mTextSizeUnit, mMetaTextDefaultDimension, mPrefSizeMultiplier)
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }
        // Add path
        node.path?.let { path ->
            holder.path?.apply {
                text = path
                visibility = View.VISIBLE
            }
        } ?: run {
            holder.path?.visibility = View.GONE
        }

        // Assign icon colors
        var iconColor = if (holder.container.isSelected)
            mColorOnSecondary
        else when (node) {
            is GroupInfo -> mTextColor
            is EntryInfo -> mColorSecondary
            else -> mColorSecondary
        }

        // Specific elements for entry
        if (node is EntryInfo) {
            holder.text.text = node.getVisualTitle()
            // Add subText with username
            holder.subText?.apply {
                val username = node.username
                if (mShowUserNames && username.isNotEmpty()) {
                    visibility = View.VISIBLE
                    text = username
                    setTextSize(mTextSizeUnit, mSubTextDefaultDimension, mPrefSizeMultiplier)
                    strikeOut(node.isCurrentlyExpires)
                } else {
                    visibility = View.GONE
                }
            }

            // OTP
            holder.otpDisplay?.setOtpModel(node.otpModel)

            // Attachment
            holder.attachmentIcon?.visibility =
                if (node.containsAttachment()) View.VISIBLE else View.GONE

            // Passkey
            holder.passkeyIcon?.visibility =
                if (node.passkey != null) View.VISIBLE else View.GONE

            // Assign colors
            assignBackgroundColor(holder.container, node)
            assignBackgroundColor(holder.otpDisplay, node)
            val foregroundColor = if (mShowEntryColors) node.foregroundColor else null
            if (!isSelected) {
                if (foregroundColor != null) {
                    holder.text.setTextColor(foregroundColor)
                    holder.subText?.setTextColor(foregroundColor)
                    holder.otpDisplay?.setTokenTextColor(foregroundColor)
                    holder.otpDisplay?.setProgressIndicatorColor(foregroundColor)
                    holder.attachmentIcon?.setColorFilter(foregroundColor)
                    holder.passkeyIcon?.setColorFilter(foregroundColor)
                    holder.meta.setTextColor(foregroundColor)
                    holder.path?.setTextColor(foregroundColor)
                    iconColor = foregroundColor
                } else {
                    holder.text.setTextColor(mTextColor)
                    holder.subText?.setTextColor(mTextColorSecondary)
                    holder.otpDisplay?.setTokenTextColor(mTextColorSecondary)
                    holder.otpDisplay?.setProgressIndicatorColor(mColorSecondary)
                    holder.attachmentIcon?.setColorFilter(mTextColorSecondary)
                    holder.passkeyIcon?.setColorFilter(mTextColorSecondary)
                    holder.meta.setTextColor(mTextColor)
                    holder.path?.setTextColor(mTextColor)
                }
            } else {
                holder.text.setTextColor(mColorOnSecondary)
                holder.subText?.setTextColor(mColorOnSecondary)
                holder.otpDisplay?.setTokenTextColor(mColorOnSecondary)
                holder.otpDisplay?.setProgressIndicatorColor(mColorOnSecondary)
                holder.attachmentIcon?.setColorFilter(mColorOnSecondary)
                holder.passkeyIcon?.setColorFilter(mColorOnSecondary)
                holder.meta.setTextColor(mColorOnSecondary)
                holder.path?.setTextColor(mColorOnSecondary)
            }
        }

        // Add number of entries in groups
        if (node is SortedGroupInfo) {
            if (mShowNumberEntries) {
                holder.numberChildren?.apply {
                    text = node.numberOfChildEntries.toString()
                    setTextSize(mTextSizeUnit, mNumberChildrenTextDefaultDimension, mPrefSizeMultiplier)
                    visibility = View.VISIBLE
                }
            } else {
                holder.numberChildren?.visibility = View.GONE
            }
        }

        // Assign image
        holder.imageIdentifier?.setColorFilter(iconColor)
        holder.icon.apply {
            iconDrawableFactory.assignDatabaseIcon(this, node.icon, iconColor)
            // Relative size of the icon
            layoutParams?.apply {
                height = (mIconDefaultDimension * mPrefSizeMultiplier).toInt()
                width = (mIconDefaultDimension * mPrefSizeMultiplier).toInt()
            }
        }

        // Assign click
        holder.container.setOnClickListener {
            mNodeClickCallback?.onNodeClick(node)
        }
        holder.container.setOnLongClickListener {
            mNodeClickCallback?.onNodeLongClick(node) ?: false
        }
    }

    private fun assignBackgroundColor(view: View?, entry: EntryInfo) {
        view?.let {
            ViewCompat.setBackgroundTintList(
                view,
                ColorStateList.valueOf(
                    if (!view.isSelected) {
                        (if (mShowEntryColors) entry.backgroundColor else null)
                            ?: mColorSurfaceContainer
                    } else {
                        mColorSecondary
                    }
                )
            )
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
     * Callback listener to redefine to do an action when a node is clicked
     */
    interface NodeClickCallback {
        fun onNodeClick(node: SortedNodeInfo)
        fun onNodeLongClick(node: SortedNodeInfo): Boolean
    }

    class NodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var container: View = itemView.findViewById(R.id.node_container)
        var imageIdentifier: ImageView? = itemView.findViewById(R.id.node_image_identifier)
        var icon: ImageView = itemView.findViewById(R.id.node_icon)
        var text: TextView = itemView.findViewById(R.id.node_text)
        var subText: TextView? = itemView.findViewById(R.id.node_subtext)
        var tags: RecyclerView = itemView.findViewById(R.id.node_tags_list_view)
        var meta: TextView = itemView.findViewById(R.id.node_meta)
        var path: TextView? = itemView.findViewById(R.id.node_path)
        var otpDisplay: OtpDisplayView? = itemView.findViewById(R.id.node_otp_display)
        var numberChildren: TextView? = itemView.findViewById(R.id.node_child_numbers)
        var attachmentIcon: ImageView? = itemView.findViewById(R.id.node_attachment_icon)
        var passkeyIcon: ImageView? = itemView.findViewById(R.id.node_passkey_icon)
    }

    companion object {
        private val TAG = NodesAdapter::class.java.name
    }
}
