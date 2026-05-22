/*
 * Copyright 2020 Jeremy Jamet / Kunzisoft.
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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Tag
import com.kunzisoft.keepass.database.element.node.DefaultNodeFilter
import com.kunzisoft.keepass.database.element.node.EmptyNodeFilter
import com.kunzisoft.keepass.database.element.node.NodeFilter
import com.kunzisoft.keepass.icons.IconDrawableFactory
import com.kunzisoft.keepass.model.GroupInfo
import com.kunzisoft.keepass.model.SearchGroupInfo
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.view.strikeOut

class BreadcrumbAdapter(val context: Context, val database: ContextualDatabase?)
    : RecyclerView.Adapter<BreadcrumbAdapter.BreadcrumbGroupViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    var iconDrawableFactory: IconDrawableFactory? = null
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    private var mNodeBreadcrumb: MutableList<GroupInfo> = mutableListOf()
    var onItemClickListener: ((item: GroupInfo, position: Int)->Unit)? = null
    var onLongItemClickListener: ((item: GroupInfo, position: Int)->Unit)? = null

    private var mShowNumberEntries = false
    private var mShowTags = false
    private var mShowUUID = false
    private var mRecursiveNumberOfEntries = false
    private var mNodeFilter: NodeFilter = EmptyNodeFilter()
    private var mIconColor: Int = 0

    init {
        mShowNumberEntries = PreferencesUtil.showNumberEntries(context)
        mShowTags = PreferencesUtil.showTags(context)
        mShowUUID = PreferencesUtil.showUUID(context)
        mRecursiveNumberOfEntries = PreferencesUtil.recursiveNumberEntries(context)
        mNodeFilter = DefaultNodeFilter(
            database = database,
            showExpired = PreferencesUtil.showExpiredEntries(context),
            showTemplate = PreferencesUtil.showTemplates(context)
        )

        // Retrieve the color to tint the icon
        val taIconColor = context.theme.obtainStyledAttributes(intArrayOf(R.attr.colorOnSurface))
        mIconColor = taIconColor.getColor(0, Color.WHITE)
        taIconColor.recycle()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setNode(breadcrumb: List<GroupInfo>) {
        mNodeBreadcrumb.clear()
        mNodeBreadcrumb.addAll(breadcrumb)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            mNodeBreadcrumb.size - 1 -> 0
            else -> 1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BreadcrumbGroupViewHolder {
        return BreadcrumbGroupViewHolder(inflater.inflate(
            when (viewType) {
                0 -> R.layout.item_breadcrumb_important
                else -> R.layout.item_breadcrumb
            }, parent, false)
        )
    }

    override fun onBindViewHolder(holder: BreadcrumbGroupViewHolder, position: Int) {
        val node = mNodeBreadcrumb[position]

        holder.groupNameView.apply {
            text = node.title
            strikeOut(node.isCurrentlyExpires)
        }

        holder.itemView.apply {
            setOnClickListener {
                onItemClickListener?.invoke(node, position)
            }
            setOnLongClickListener {
                onLongItemClickListener?.invoke(node, position)
                true
            }
        }

        holder.groupIconView?.let { imageView ->
            iconDrawableFactory?.assignDatabaseIcon(
                imageView,
                node.icon,
                mIconColor
            )
        }

        holder.groupNumbersView?.apply {
            if (mShowNumberEntries) {
                text = database?.getNumberChildrenEntries(
                    node = node,
                    recursiveNumberOfEntries = mRecursiveNumberOfEntries,
                    filter = mNodeFilter
                ).toString()
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }

        holder.tagsListView.apply {
            val tags = node.tags
            if (mShowTags) {
                val tagsAdapter = TagsAdapter(context, TagsAdapter.TagViewType.SMALL)
                layoutManager = LinearLayoutManager(
                    context,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
                adapter = tagsAdapter
                tagsAdapter.setTags(tags)
                tagsAdapter.onItemClickListener =
                    object : TagsAdapter.OnItemClickListener {
                        override fun onItemClick(item: Tag) {
                            onItemClickListener?.invoke(node, position)
                        }

                        override fun onItemLongClick(item: Tag): Boolean {
                            onLongItemClickListener?.invoke(node, position)
                            return true
                        }
                    }
            }
            visibility = if (tags.isNotEmpty()) View.VISIBLE else View.GONE
        }

        holder.groupMetaView?.apply {
            val meta = node.nodeId.toVisualString()
            visibility = if (meta != null
                && node !is SearchGroupInfo
                && mShowUUID
            ) {
                text = meta
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    override fun getItemCount(): Int {
        return mNodeBreadcrumb.size
    }

    class BreadcrumbGroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var groupIconView: ImageView? = itemView.findViewById(R.id.group_icon)
        var groupNumbersView: TextView? = itemView.findViewById(R.id.group_numbers)
        var groupNameView: TextView = itemView.findViewById(R.id.group_name)
        var groupMetaView: TextView? = itemView.findViewById(R.id.group_meta)
        var tagsListView: RecyclerView = itemView.findViewById(R.id.group_tags_list_view)
    }
}