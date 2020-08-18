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
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.database.CompressionAlgorithm
import com.kunzisoft.keepass.model.AttachmentState
import com.kunzisoft.keepass.model.EntryAttachment

class EntryAttachmentsItemsAdapter(context: Context, private val editable: Boolean)
    : AnimatedItemsAdapter<EntryAttachment, EntryAttachmentsItemsAdapter.EntryBinariesViewHolder>(context) {

    var onItemClickListener: ((item: EntryAttachment)->Unit)? = null

    private val mDatabase = Database.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryBinariesViewHolder {
        return EntryBinariesViewHolder(inflater.inflate(R.layout.item_attachment, parent, false))
    }

    override fun onBindViewHolder(holder: EntryBinariesViewHolder, position: Int) {
        val entryAttachment = itemsList[position]

        holder.itemView.visibility = View.VISIBLE
        holder.binaryFileTitle.text = entryAttachment.name
        holder.binaryFileSize.text = Formatter.formatFileSize(context,
                entryAttachment.binaryAttachment.length())
        holder.binaryFileCompression.apply {
            if (mDatabase.compressionAlgorithm == CompressionAlgorithm.GZip
                    || entryAttachment.binaryAttachment.isCompressed == true) {
                text = CompressionAlgorithm.GZip.getName(context.resources)
                visibility = View.VISIBLE
            } else {
                text = ""
                visibility = View.GONE
            }
        }
        if (editable) {
            holder.binaryFileProgressContainer.visibility = View.GONE
            holder.binaryFileDeleteButton.apply {
                visibility = View.VISIBLE
                onBindDeleteButton(holder, this, entryAttachment, position)
            }
        } else {
            holder.binaryFileProgressContainer.visibility = View.VISIBLE
            holder.binaryFileDeleteButton.visibility = View.GONE
            holder.binaryFileProgress.apply {
                visibility = when (entryAttachment.downloadState) {
                    AttachmentState.NULL, AttachmentState.COMPLETE, AttachmentState.ERROR -> View.GONE
                    AttachmentState.START, AttachmentState.IN_PROGRESS -> View.VISIBLE
                }
                progress = entryAttachment.downloadProgression
            }
            holder.itemView.setOnClickListener {
                onItemClickListener?.invoke(entryAttachment)
            }
        }
    }

    fun updateProgress(entryAttachment: EntryAttachment) {
        val indexEntryAttachment = itemsList.indexOfLast { current -> current.name == entryAttachment.name }
        if (indexEntryAttachment != -1) {
            itemsList[indexEntryAttachment] = entryAttachment
            notifyItemChanged(indexEntryAttachment)
        }
    }

    inner class EntryBinariesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var binaryFileTitle: TextView = itemView.findViewById(R.id.item_attachment_title)
        var binaryFileSize: TextView = itemView.findViewById(R.id.item_attachment_size)
        var binaryFileCompression: TextView = itemView.findViewById(R.id.item_attachment_compression)
        var binaryFileProgressContainer: View = itemView.findViewById(R.id.item_attachment_progress_container)
        var binaryFileProgress: ProgressBar = itemView.findViewById(R.id.item_attachment_progress)
        var binaryFileDeleteButton: View = itemView.findViewById(R.id.item_attachment_delete_button)
    }
}