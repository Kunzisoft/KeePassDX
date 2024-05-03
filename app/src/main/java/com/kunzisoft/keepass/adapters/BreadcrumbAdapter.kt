package com.kunzisoft.keepass.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.database.element.node.Type
import com.kunzisoft.keepass.icons.IconDrawableFactory
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.view.strikeOut

class BreadcrumbAdapter(val context: Context)
    : RecyclerView.Adapter<BreadcrumbAdapter.BreadcrumbGroupViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    var iconDrawableFactory: IconDrawableFactory? = null
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    private var mNodeBreadcrumb: MutableList<Node?> = mutableListOf()
    var onItemClickListener: ((item: Node, position: Int)->Unit)? = null
    var onLongItemClickListener: ((item: Node, position: Int)->Unit)? = null

    private var mShowNumberEntries = false
    private var mShowUUID = false
    private var mIconColor: Int = 0

    init {
        mShowNumberEntries = PreferencesUtil.showNumberEntries(context)
        mShowUUID = PreferencesUtil.showUUID(context)

        // Retrieve the color to tint the icon
        val taIconColor = context.theme.obtainStyledAttributes(intArrayOf(R.attr.colorOnSurface))
        mIconColor = taIconColor.getColor(0, Color.WHITE)
        taIconColor.recycle()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setNode(node: Node?) {
        mNodeBreadcrumb.clear()
        node?.let {
            var currentNode = it
            mNodeBreadcrumb.add(0, currentNode)
            while (currentNode.containsParent()) {
                currentNode.parent?.let { parent ->
                    currentNode = parent
                    mNodeBreadcrumb.add(0, currentNode)
                }
            }
        }
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
            text = node?.title ?: ""
            strikeOut(node?.isCurrentlyExpires ?: false)
        }

        holder.itemView.apply {
            setOnClickListener {
                node?.let {
                    onItemClickListener?.invoke(it, position)
                }
            }
            setOnLongClickListener {
                node?.let {
                    onLongItemClickListener?.invoke(it, position)
                }
                true
            }
        }

        if (node?.type == Type.GROUP) {
            (node as Group).let { group ->

                holder.groupIconView?.let { imageView ->
                    iconDrawableFactory?.assignDatabaseIcon(
                        imageView,
                        group.icon,
                        mIconColor
                    )
                }

                holder.groupNumbersView?.apply {
                    if (mShowNumberEntries) {
                        group.refreshNumberOfChildEntries(
                            Group.ChildFilter.getDefaults(
                                PreferencesUtil.showExpiredEntries(context)
                            )
                        )
                        text = group.recursiveNumberOfChildEntries.toString()
                        visibility = View.VISIBLE
                    } else {
                        visibility = View.GONE
                    }
                }

                holder.groupMetaView?.apply {
                    val meta = group.nodeId.toVisualString()
                    visibility = if (meta != null
                        && !group.isVirtual
                        && mShowUUID
                    ) {
                        text = meta
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                }
            }
        }

    }

    override fun getItemCount(): Int {
        return mNodeBreadcrumb.size
    }

    inner class BreadcrumbGroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var groupIconView: ImageView? = itemView.findViewById(R.id.group_icon)
        var groupNumbersView: TextView? = itemView.findViewById(R.id.group_numbers)
        var groupNameView: TextView = itemView.findViewById(R.id.group_name)
        var groupMetaView: TextView? = itemView.findViewById(R.id.group_meta)
    }
}