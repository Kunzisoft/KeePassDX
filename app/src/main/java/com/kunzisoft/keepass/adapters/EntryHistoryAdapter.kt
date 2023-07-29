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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.utils.TimeUtil.getDateTimeString

class EntryHistoryAdapter(val context: Context) : RecyclerView.Adapter<EntryHistoryAdapter.EntryHistoryViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    var entryHistoryList: MutableList<EntryInfo> = ArrayList()
    var onItemClickListener: ((item: EntryInfo, position: Int)->Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryHistoryViewHolder {
        return EntryHistoryViewHolder(inflater.inflate(R.layout.item_list_entry_history, parent, false))
    }

    override fun onBindViewHolder(holder: EntryHistoryViewHolder, position: Int) {
        val entryHistory = entryHistoryList[position]

        holder.lastModifiedView.text = entryHistory.lastModificationTime.getDateTimeString(context.resources)
        holder.titleView.text = entryHistory.title
        holder.usernameView.text = entryHistory.username

        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(entryHistory, position)
        }
    }

    override fun getItemCount(): Int {
        return entryHistoryList.size
    }

    fun clear() {
        entryHistoryList.clear()
    }

    inner class EntryHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var lastModifiedView: TextView = itemView.findViewById(R.id.entry_history_last_modified)
        var titleView: TextView = itemView.findViewById(R.id.entry_history_title)
        var usernameView: TextView = itemView.findViewById(R.id.entry_history_username)
    }
}