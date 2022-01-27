/*
 * Copyright 2021 Jeremy Jamet / Kunzisoft.
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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.Tags

class TagsAdapter(context: Context) : RecyclerView.Adapter<TagsAdapter.TagViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var mTags: Tags = Tags()
    var onItemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = inflater.inflate(R.layout.item_tag, parent, false)
        return TagViewHolder(view)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val field = mTags.get(position)
        holder.name.text = field
        holder.bind(field, onItemClickListener)
    }

    override fun getItemCount(): Int {
        return mTags.size()
    }

    fun setTags(tags: Tags) {
        mTags.setTags(tags)
        notifyDataSetChanged()
    }

    fun clear() {
        mTags.clear()
    }

    interface OnItemClickListener {
        fun onItemClick(item: String)
    }

    inner class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var name: TextView = itemView.findViewById(R.id.tag_name)

        fun bind(item: String, listener: OnItemClickListener?) {
            itemView.setOnClickListener { listener?.onItemClick(item) }
        }
    }
}