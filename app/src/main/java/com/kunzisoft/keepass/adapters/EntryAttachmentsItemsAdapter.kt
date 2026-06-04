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
import android.content.res.TypedArray
import android.graphics.Color
import android.text.format.Formatter
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.ImageViewerActivity
import com.kunzisoft.keepass.database.element.binary.BinaryCache
import com.kunzisoft.keepass.database.element.database.CompressionAlgorithm
import com.kunzisoft.keepass.database.helper.getLocalizedName
import com.kunzisoft.keepass.model.AttachmentState
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.model.StreamDirection
import com.kunzisoft.keepass.services.AttachmentFileNotificationService.Companion.FILE_PROGRESSION_MAX
import com.kunzisoft.keepass.tasks.BinaryDatabaseManager
import com.kunzisoft.keepass.view.collapse
import com.kunzisoft.keepass.view.expand
import kotlin.math.max


class EntryAttachmentsItemsAdapter(val context: Context)
    : RecyclerView.Adapter<EntryAttachmentsItemsAdapter.EntryBinariesViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    private val itemsSortedList: SortedList<EntryAttachmentState> = SortedList(
        EntryAttachmentState::class.java,
        object: SortedListAdapterCallback<EntryAttachmentState>(this) {
        override fun compare(
            p0: EntryAttachmentState,
            p1: EntryAttachmentState
        ): Int {
            return p0.attachment.name.compareTo(p1.attachment.name)
        }

        override fun areContentsTheSame(
            p0: EntryAttachmentState,
            p1: EntryAttachmentState
        ): Boolean {
            return p0.attachment.name == p1.attachment.name
                    && p0.downloadState == p1.downloadState
                    && p0.downloadProgression == p1.downloadProgression
                    && p0.streamDirection == p1.streamDirection
                    && p0.attachment.binaryData.getSize() == p1.attachment.binaryData.getSize()
        }

        override fun areItemsTheSame(
            p0: EntryAttachmentState,
            p1: EntryAttachmentState
        ): Boolean {
            return p0.attachment.name == p1.attachment.name
        }
    })

    var onDeleteButtonClickListener: ((item: EntryAttachmentState)->Unit)? = null

    var onListSizeChangedListener: ((previousSize: Int, newSize: Int)->Unit)? = null

    var binaryCache: BinaryCache? = null
    var onItemClickListener: ((item: EntryAttachmentState)->Unit)? = null

    // Approximately
    private val mImagePreviewMaxWidth = max(
            context.resources.displayMetrics.widthPixels,
            context.resources.getDimensionPixelSize(R.dimen.item_file_info_height)
    )
    private var mTitleColor: Int

    init {
        // Get the primary text color of the theme
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        val typedArray: TypedArray = context.obtainStyledAttributes(
            typedValue.data, intArrayOf(android.R.attr.textColor)
        )
        mTitleColor = typedArray.getColor(0, -1)
        typedArray.recycle()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryBinariesViewHolder {
        return EntryBinariesViewHolder(
            inflater.inflate(R.layout.item_attachment,
                parent,
                false)
        )
    }

    override fun onBindViewHolder(holder: EntryBinariesViewHolder, position: Int) {
        val entryAttachmentState = itemsSortedList.get(position)

        holder.itemView.visibility = View.VISIBLE
        holder.binaryFileThumbnail.apply {
            // Perform image loading only if upload is finished
            if (entryAttachmentState.downloadState == AttachmentState.COMPLETE) {
                // Load the bitmap image
                binaryCache?.let { binaryCache ->
                    BinaryDatabaseManager.loadBitmap(
                        binaryCache,
                        entryAttachmentState.attachment.binaryData,
                        mImagePreviewMaxWidth
                    ) { imageLoaded ->
                        if (imageLoaded == null) {
                            visibility = View.GONE
                        } else {
                            setImageBitmap(imageLoaded)
                            if (visibility != View.VISIBLE) {
                                expand(
                                    animate = true,
                                    defaultHeight = resources.getDimensionPixelSize(R.dimen.image_preview_height)
                                )
                            }
                        }
                    }
                }
            } else {
                visibility = View.GONE
            }
            this.setOnClickListener {
                ImageViewerActivity.getInstance(context, entryAttachmentState.attachment)
            }
        }
        holder.binaryFileBroken.apply {
            setColorFilter(Color.RED)
            visibility = if (entryAttachmentState.attachment.binaryData.isCorrupted) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
        holder.binaryFileTitle.text = entryAttachmentState.attachment.name
        if (entryAttachmentState.attachment.binaryData.isCorrupted) {
            holder.binaryFileTitle.setTextColor(Color.RED)
        } else {
            holder.binaryFileTitle.setTextColor(mTitleColor)
        }

        val size = entryAttachmentState.attachment.binaryData.getSize()
        holder.binaryFileSize.text = Formatter.formatFileSize(context, size)
        holder.binaryFileCompression.apply {
            if (entryAttachmentState.attachment.binaryData.isCompressed) {
                text = CompressionAlgorithm.GZIP.getLocalizedName(context.resources)
                visibility = View.VISIBLE
            } else {
                text = ""
                visibility = View.GONE
            }
        }
        holder.binaryFileProgress.max = FILE_PROGRESSION_MAX
        when (entryAttachmentState.streamDirection) {
            StreamDirection.UPLOAD -> {
                holder.binaryFileProgressIcon.isActivated = true
                when (entryAttachmentState.downloadState) {
                    AttachmentState.START,
                    AttachmentState.IN_PROGRESS -> {
                        holder.binaryFileProgressContainer.visibility = View.VISIBLE
                        holder.binaryFileProgress.apply {
                            visibility = View.VISIBLE
                            progress = entryAttachmentState.downloadProgression
                        }
                        holder.binaryFileDeleteButton.apply {
                            visibility = View.GONE
                            setOnClickListener(null)
                        }
                    }
                    AttachmentState.ERROR,
                    AttachmentState.CANCELED,
                    AttachmentState.COMPLETE -> {
                        holder.binaryFileProgressContainer.visibility = View.GONE
                        holder.binaryFileProgress.visibility = View.GONE
                        holder.binaryFileDeleteButton.apply {
                            visibility = View.VISIBLE
                            onBindDeleteButton(holder, this, entryAttachmentState, position)
                        }
                    }
                }
                holder.binaryFileInfo.setOnClickListener(null)
            }
            StreamDirection.DOWNLOAD -> {
                holder.binaryFileProgressIcon.isActivated = false
                holder.binaryFileProgressContainer.visibility = View.VISIBLE
                holder.binaryFileDeleteButton.visibility = View.GONE
                holder.binaryFileProgress.apply {
                    visibility = when (entryAttachmentState.downloadState) {
                        AttachmentState.COMPLETE,
                        AttachmentState.CANCELED,
                        AttachmentState.ERROR -> View.GONE

                        AttachmentState.START,
                        AttachmentState.IN_PROGRESS -> View.VISIBLE
                    }
                    setProgressCompat(entryAttachmentState.downloadProgression, true)
                }
                holder.binaryFileInfo.setOnClickListener {
                    onItemClickListener?.invoke(entryAttachmentState)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return itemsSortedList.size()
    }

    fun assignItems(items: List<EntryAttachmentState>) {
        val previousSize = itemCount
        itemsSortedList.clear()
        itemsSortedList.addAll(items)
        notifyDataSetChanged()
        onListSizeChangedListener?.invoke(previousSize, itemCount)
    }

    fun indexOf(item: EntryAttachmentState): Int {
        return itemsSortedList.indexOf(item)
    }

    fun putItem(item: EntryAttachmentState) {
        val previousSize = itemCount
        itemsSortedList.add(item)
        notifyItemChanged(itemsSortedList.indexOf(item))
        onListSizeChangedListener?.invoke(previousSize, itemCount)
    }

    fun onBindDeleteButton(holder: EntryBinariesViewHolder, deleteButton: View, item: EntryAttachmentState, position: Int) {
        deleteButton.apply {
            visibility = View.VISIBLE
            setOnClickListener {
                holder.itemView.collapse(true) {
                    onDeleteButtonClickListener?.invoke(item)
                }
            }
        }
    }

    fun clear() {
        itemsSortedList.clear()
    }

    class EntryBinariesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var binaryFileThumbnail: ImageView = itemView.findViewById(R.id.item_attachment_thumbnail)
        var binaryFileInfo: View = itemView.findViewById(R.id.item_attachment_info)
        var binaryFileBroken: ImageView = itemView.findViewById(R.id.item_attachment_broken)
        var binaryFileTitle: TextView = itemView.findViewById(R.id.item_attachment_title)
        var binaryFileSize: TextView = itemView.findViewById(R.id.item_attachment_size)
        var binaryFileCompression: TextView = itemView.findViewById(R.id.item_attachment_compression)
        var binaryFileProgressContainer: View = itemView.findViewById(R.id.item_attachment_progress_container)
        var binaryFileProgressIcon: ImageView = itemView.findViewById(R.id.item_attachment_icon)
        var binaryFileProgress: CircularProgressIndicator = itemView.findViewById(R.id.item_attachment_progress)
        var binaryFileDeleteButton: View = itemView.findViewById(R.id.item_attachment_delete_button)
    }
}
