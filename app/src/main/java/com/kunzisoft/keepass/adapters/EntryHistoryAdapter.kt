package com.kunzisoft.keepass.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.Entry

class EntryHistoryAdapter(val context: Context) : RecyclerView.Adapter<EntryHistoryAdapter.EntryHistoryViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    var entryHistoryList: MutableList<Entry> = ArrayList()
    var onItemClickListener: ((item: Entry, position: Int)->Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryHistoryViewHolder {
        return EntryHistoryViewHolder(inflater.inflate(R.layout.item_list_entry_history, parent, false))
    }

    override fun onBindViewHolder(holder: EntryHistoryViewHolder, position: Int) {
        val entryHistory = entryHistoryList[position]

        holder.lastModifiedView.text = entryHistory.lastModificationTime.getDateTimeString(context.resources)
        holder.titleView.text = entryHistory.title
        holder.usernameView.text = entryHistory.username
        holder.urlView.text = entryHistory.url

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
        var urlView: TextView = itemView.findViewById(R.id.entry_history_url)
    }
}