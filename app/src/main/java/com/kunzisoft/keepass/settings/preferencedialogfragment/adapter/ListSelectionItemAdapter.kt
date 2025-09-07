/*
 * Copyright 2025 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.settings.preferencedialogfragment.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R

class ListSelectionItemAdapter<T>()
    : RecyclerView.Adapter<ListSelectionItemAdapter.SelectionViewHolder>() {

    private val itemList: MutableList<T> = mutableListOf()
    var selectedItems: MutableList<T> = mutableListOf()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var itemSelectedCallback: ItemSelectedCallback<T>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectionViewHolder {
        return SelectionViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.pref_dialog_list_item, parent, false))
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onBindViewHolder(holder: SelectionViewHolder, position: Int) {
        val item = itemList[position]

        holder.container.apply {
            isSelected = selectedItems.contains(item)
        }
        holder.textView.apply {
            text = item.toString()
            setOnClickListener {
                if (selectedItems.contains(item))
                    selectedItems.remove(item)
                else
                    selectedItems.add(item)
                itemSelectedCallback?.onItemSelected(item)
                notifyDataSetChanged()
            }
        }
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    fun setItems(items: List<T>) {
        this.itemList.clear()
        this.itemList.addAll(items)
    }

    interface ItemSelectedCallback<T> {
        fun onItemSelected(item: T)
    }

    class SelectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textView: TextView = itemView.findViewById(R.id.pref_dialog_list_text)
        var container: ViewGroup = itemView.findViewById(R.id.pref_dialog_list_container)
    }
}
