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
package com.kunzisoft.keepass.adapters

import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import java.util.UUID

class KeyboardEntriesAdapter(context: Context) : RecyclerView.Adapter<KeyboardEntriesAdapter.EntryViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var entries: MutableList<KeyboardEntry> = mutableListOf()
    var selectedEntry: KeyboardEntry? = null
        private set
    var entrySelectionListener: EntrySelectionListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val view = inflater.inflate(R.layout.item_keyboard_entry, parent, false)
        return EntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        val entry = entries[position]
        holder.title.text = entry.title
        holder.subtitle.apply {
            val subtitle = entry.subtitle
            isVisible = subtitle.isNotEmpty()
            text = subtitle
        }
        holder.icon.apply {
            val icon = entry.icon
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && icon != null) {
                isVisible = true
                setImageIcon(icon)
            } else {
                isVisible = false
            }
        }
        holder.itemView.isSelected = entry == selectedEntry
        holder.bind(entry, entrySelectionListener)
    }

    override fun getItemCount(): Int {
        return entries.size
    }

    fun isEmpty(): Boolean {
        return entries.isEmpty()
    }

    fun setEntries(newEntries: List<KeyboardEntry>) {
        entries.clear()
        entries.addAll(newEntries)
        selectedEntry = newEntries.firstOrNull()
        selectedEntry?.let {
            entrySelectionListener?.onEntrySelected(it)
        }
    }

    fun clear() {
        entries.clear()
    }

    interface EntrySelectionListener {
        fun onEntrySelected(item: KeyboardEntry)
    }

    inner class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var icon: ImageView = itemView.findViewById(R.id.magikeyboard_entry_icon)
        var title: TextView = itemView.findViewById(R.id.magikeyboard_entry_text)
        var subtitle: TextView = itemView.findViewById(R.id.magikeyboard_entry_subtext)

        fun bind(item: KeyboardEntry, listener: EntrySelectionListener?) {
            itemView.setOnClickListener {
                selectedEntry = item
                listener?.onEntrySelected(item)
                notifyDataSetChanged()
            }
        }
    }
    
    data class KeyboardEntry(
        val id: UUID,
        val icon: Icon?,
        val title: String,
        val subtitle: String
    )
}
