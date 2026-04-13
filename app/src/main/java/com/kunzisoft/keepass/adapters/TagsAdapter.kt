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
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.Tag
import com.kunzisoft.keepass.database.element.Tags

class TagsAdapter(
    context: Context,
    val globalViewType: TagViewType = TagViewType.STANDARD
) : RecyclerView.Adapter<TagsAdapter.TagViewHolder>() {

    @ColorInt
    private val mColorSecondary: Int
    @ColorInt
    private val mColorOnSecondary: Int

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var mTags: Tags = Tags()
    var onItemClickListener: OnItemClickListener? = null

    init {
        context.obtainStyledAttributes(intArrayOf(R.attr.colorSecondary)).also { taColorSecondary ->
            this.mColorSecondary = taColorSecondary.getColor(0, Color.GRAY)
        }.recycle()
        context.obtainStyledAttributes(intArrayOf(R.attr.colorOnSecondary)).also { taColorOnSecondary ->
            this.mColorOnSecondary = taColorOnSecondary.getColor(0, Color.WHITE)
        }.recycle()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = inflater.inflate(when(globalViewType) {
            TagViewType.STANDARD -> R.layout.item_tag
            TagViewType.SMALL -> R.layout.item_tag_small
        }, parent, false)
        return TagViewHolder(view)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val field = mTags.get(position)
        holder.name.apply {
            text = field.name
            setTextColor(if (field.isSelected) mColorOnSecondary else mColorSecondary)
            ViewCompat.setBackgroundTintList(
                this,
                ColorStateList.valueOf(
                    (if (field.isSelected) mColorOnSecondary else mColorSecondary))
            )
        }
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

    fun toggleSelection(isSelected: Boolean) {
        if (isSelected) mTags.selectAll() else mTags.deselectAll()
        notifyDataSetChanged()
    }

    interface OnItemClickListener {
        fun onItemClick(item: Tag)
        fun onItemLongClick(item: Tag): Boolean
    }

    class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var name: TextView = itemView.findViewById(R.id.tag_name)

        fun bind(item: Tag, listener: OnItemClickListener?) {
            listener?.let {
                itemView.setOnClickListener { listener.onItemClick(item) }
                itemView.setOnLongClickListener { listener.onItemLongClick(item) }
            }
        }
    }

    enum class TagViewType {
        STANDARD, SMALL
    }
}