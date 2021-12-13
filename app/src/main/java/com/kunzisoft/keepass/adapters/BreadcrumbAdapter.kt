package com.kunzisoft.keepass.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.node.Node

class BreadcrumbAdapter(val context: Context) : RecyclerView.Adapter<BreadcrumbAdapter.BreadcrumbGroupViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var mNodeBreadcrumb: MutableList<Node?> = mutableListOf()
    var onItemClickListener: ((item: Node, position: Int)->Unit)? = null

    fun setNode(node: Node) {
        mNodeBreadcrumb.clear()
        var currentNode = node
        while (currentNode.containsParent()) {
            currentNode.parent?.let { parent ->
                currentNode = parent
                mNodeBreadcrumb.add(0, currentNode)
            }
        }
        mNodeBreadcrumb.add(0, null)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BreadcrumbGroupViewHolder {
        return BreadcrumbGroupViewHolder(inflater.inflate(R.layout.item_breadcrumb, parent, false))
    }

    override fun onBindViewHolder(holder: BreadcrumbGroupViewHolder, position: Int) {
        val node = mNodeBreadcrumb[position]

        holder.breadcrumbTextView.text = when {
            node == null -> ""
            node.title.isEmpty() -> context.getString(R.string.root)
            else -> node.title
        }

        holder.breadcrumbTextView.setOnClickListener {
            node?.let {
                onItemClickListener?.invoke(it, position)
            }
        }
    }

    override fun getItemCount(): Int {
        return mNodeBreadcrumb.size
    }

    inner class BreadcrumbGroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var breadcrumbTextView: TextView = itemView.findViewById(R.id.breadcrumb_text)
    }
}