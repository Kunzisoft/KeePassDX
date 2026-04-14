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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.Tag
import com.kunzisoft.keepass.database.element.Tags

class TagsAdapter(
    context: Context,
    val globalViewType: TagViewType = TagViewType.STANDARD
) : RecyclerView.Adapter<TagsAdapter.TagViewHolder>() {

    @ColorInt
    private val mTextColor: Int
    @ColorInt
    private val mColorSecondary: Int
    @ColorInt
    private val mColorOnSecondary: Int

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var mTags: Tags = Tags()
    private var mSelectedTags: Tags = Tags()
    var onItemClickListener: OnItemClickListener? = null

    init {
        context.obtainStyledAttributes(intArrayOf(android.R.attr.textColor)).also { taTextColor ->
            this.mTextColor = taTextColor.getColor(0, Color.BLACK)
        }.recycle()
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
            TagViewType.CHIP -> R.layout.item_tag_chip
        }, parent, false)
        return TagViewHolder(view)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val tag = mTags.get(position)
        val tagIsSelected = mSelectedTags.contains(tag)
        holder.name.apply {
            text = tag.name
        }
        when (globalViewType) {
            TagViewType.SMALL -> {
                // Tint text depending on selection
                holder.name.setTextColor(if (tagIsSelected) mColorOnSecondary else mColorSecondary)
                // Tint background depending on selection
                ViewCompat.setBackgroundTintList(
                    holder.name,
                    ColorStateList.valueOf(
                        (if (tagIsSelected) mColorOnSecondary else mColorSecondary)
                    )
                )
            }
            TagViewType.CHIP -> {
                holder.container?.let {
                    ViewCompat.setBackgroundTintList(
                        it,
                        ColorStateList.valueOf(
                            (if (tagIsSelected) mColorSecondary else mTextColor)
                        )
                    )
                }
                holder.check?.let { checkView ->
                    checkView.visibility = if (tagIsSelected) View.VISIBLE else View.GONE
                    ViewCompat.setBackgroundTintList(
                        checkView,
                        ColorStateList.valueOf(
                            (if (tagIsSelected) mColorSecondary else mTextColor)
                        )
                    )
                }
                holder.name.setTextColor(if (tagIsSelected) mColorSecondary else mTextColor)
            }
            else -> {
                // No text color change in standard mode
            }
        }
        holder.bind(tag, onItemClickListener)
    }

    override fun getItemCount(): Int {
        return mTags.size()
    }

    fun setTags(newTags: Tags) {
        val oldTags = Tags(mTags)
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldTags.size()
            override fun getNewListSize(): Int = newTags.size()
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldTags.get(oldItemPosition).name == newTags.get(newItemPosition).name
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldTags.get(oldItemPosition) == newTags.get(newItemPosition)
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        mTags.setTags(newTags)
        diffResult.dispatchUpdatesTo(this)
    }

    fun clear() {
        val size = mTags.size()
        if (size > 0) {
            mTags.clear()
            notifyItemRangeRemoved(0, size)
        }
    }

    fun getSelectedStringTags(): List<String> {
        return mSelectedTags.toStringList()
    }

    fun selectTags(tags: List<String>) {
        tags.forEach {
            val tag = Tag(it)
            mSelectedTags.put(tag)
            val index = mTags.indexOf(tag)
            if (index != -1) {
                notifyItemChanged(index)
            }
        }
    }

    fun toggleSelection(tag: Tag) {
        if (mSelectedTags.contains(tag))
            mSelectedTags.remove(tag)
        else
            mSelectedTags.put(tag)
        val index = mTags.indexOf(tag)
        if (index != -1) {
            notifyItemChanged(index)
        }
    }

    fun toggleSelection(isSelected: Boolean) {
        if (isSelected)
            mSelectedTags.replaceAll(mTags)
        else
            mSelectedTags.clear()
        notifyItemRangeChanged(0, mTags.size())
    }

    interface OnItemClickListener {
        fun onItemClick(item: Tag)
        fun onItemLongClick(item: Tag): Boolean
    }

    class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var container: View? = itemView.findViewById(R.id.tag_container)
        var check: View? = itemView.findViewById(R.id.tag_check)
        var name: TextView = itemView.findViewById(R.id.tag_name)

        fun bind(item: Tag, listener: OnItemClickListener?) {
            listener?.let {
                itemView.setOnClickListener { listener.onItemClick(item) }
                itemView.setOnLongClickListener { listener.onItemLongClick(item) }
            }
        }
    }

    enum class TagViewType {
        STANDARD, SMALL, CHIP
    }
}