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
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.database.CompressionAlgorithm
import com.kunzisoft.keepass.model.AttachmentState
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
        holder.binaryFileSize.text = Formatter.formatFileSize(context,
                entryAttachment.binaryAttachment.length())
        holder.binaryFileCompression.apply {
            if (entryAttachment.binaryAttachment.isCompressed == true) {
                text = CompressionAlgorithm.GZip.getName(context.resources)
                visibility = View.VISIBLE
            } else {
                text = ""
                visibility = View.GONE
            }
        }
        holder.binaryFileProgress.apply {
            visibility = when (entryAttachment.downloadState) {
                AttachmentState.NULL, AttachmentState.COMPLETE, AttachmentState.ERROR -> View.GONE
                AttachmentState.START, AttachmentState.IN_PROGRESS -> View.VISIBLE
            }
            progress = entryAttachment.downloadProgression
        }

        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(entryAttachment, position)
        }
    }

    override fun getItemCount(): Int {
        return entryAttachmentsList.size
    }

    fun updateProgress(entryAttachment: EntryAttachment) {
        val indexEntryAttachment = entryAttachmentsList.indexOfLast { current -> current.name == entryAttachment.name }
        if (indexEntryAttachment != -1) {
            entryAttachmentsList[indexEntryAttachment] = entryAttachment
            notifyItemChanged(indexEntryAttachment)
        }
    }

    fun clear() {
        entryAttachmentsList.clear()
    }

    inner class EntryBinariesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var binaryFileTitle: TextView = itemView.findViewById(R.id.item_attachment_title)
        var binaryFileSize: TextView = itemView.findViewById(R.id.item_attachment_size)
        var binaryFileCompression: TextView = itemView.findViewById(R.id.item_attachment_compression)
        var binaryFileProgress: ProgressBar = itemView.findViewById(R.id.item_attachment_progress)
    }
}