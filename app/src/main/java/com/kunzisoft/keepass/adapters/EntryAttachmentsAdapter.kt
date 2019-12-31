package com.kunzisoft.keepass.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.model.EntryAttachment

class EntryAttachmentsAdapter(val context: Context) : RecyclerView.Adapter<EntryAttachmentsAdapter.EntryBinariesViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    var entryAttachmentsList: MutableList<EntryAttachment> = ArrayList()
    var onItemClickListener: ((item: EntryAttachment, position: Int)->Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryBinariesViewHolder {
        return EntryBinariesViewHolder(inflater.inflate(R.layout.item_attachment, parent, false))
    }

    override fun onBindViewHolder(holder: EntryBinariesViewHolder, position: Int) {
        val entryAttachment = entryAttachmentsList[position]

        holder.binaryFileTitle.text = entryAttachment.name
        holder.binaryFileSize.text = entryAttachment.binaryAttachment.length().toString() // TODO change to MB

        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(entryAttachment, position)
        }
    }

    override fun getItemCount(): Int {
        return entryAttachmentsList.size
    }

    fun clear() {
        entryAttachmentsList.clear()
    }

    inner class EntryBinariesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var binaryFileTitle: TextView = itemView.findViewById(R.id.item_attachment_title)
        var binaryFileSize: TextView = itemView.findViewById(R.id.item_attachment_size)
    }
}