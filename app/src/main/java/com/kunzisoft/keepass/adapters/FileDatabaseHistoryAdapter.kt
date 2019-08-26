/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.adapters

import android.content.Context
import android.net.Uri
import android.support.annotation.ColorInt
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryEntity
import com.kunzisoft.keepass.settings.PreferencesUtil

class FileDatabaseHistoryAdapter(private val context: Context)
    : RecyclerView.Adapter<FileDatabaseHistoryAdapter.FileDatabaseHistoryViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var fileItemOpenListener: FileItemOpenListener? = null
    private var fileSelectClearListener: FileSelectClearListener? = null

    private val listDatabaseFiles = ArrayList<FileDatabaseHistoryEntity>()

    private var mExpandedPosition = -1
    private var mPreviousExpandedPosition = -1

    @ColorInt
    private val defaultColor: Int
    @ColorInt
    private val warningColor: Int

    init {

        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(R.attr.colorAccent, typedValue, true)
        warningColor = typedValue.data
        theme.resolveAttribute(android.R.attr.textColorHintInverse, typedValue, true)
        defaultColor = typedValue.data
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileDatabaseHistoryViewHolder {
        val view = inflater.inflate(R.layout.item_file_row, parent, false)
        return FileDatabaseHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileDatabaseHistoryViewHolder, position: Int) {
        val fileHistoryEntity = listDatabaseFiles[position]

        val fileDatabaseInfo = FileInfo(context, fileHistoryEntity.databaseUri)

        // Context menu creation
        holder.fileContainer.setOnCreateContextMenuListener(ContextMenuBuilder(fileDatabaseInfo))
        // Click item to open file
        if (fileItemOpenListener != null)
            holder.fileContainer.setOnClickListener(FileItemClickListener(fileHistoryEntity))

        // File alias
        val aliasText = fileHistoryEntity.databaseAlias
        holder.fileAlias.text = when {
            aliasText.isNotEmpty() -> aliasText
            PreferencesUtil.isFullFilePathEnable(context) -> Uri.decode(fileDatabaseInfo.fileUri.toString())
            else -> fileDatabaseInfo.fileName
        }

        // File path
        holder.filePath.text = Uri.decode(fileDatabaseInfo.fileUri.toString())
        holder.filePath.textSize = PreferencesUtil.getListTextSize(context)

        holder.filePreciseInfoContainer.visibility = if (fileDatabaseInfo.found()) {
            // Modification
            holder.fileModification.text = fileDatabaseInfo.getModificationString()
            // Size
            holder.fileSize.text = fileDatabaseInfo.getSizeString()

            View.VISIBLE
        } else
            View.GONE

        // Click on information
        val isExpanded = position == mExpandedPosition
        //This line hides or shows the layout in question
        holder.fileExpandContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE

        if (isExpanded)
            mPreviousExpandedPosition = position
        if (mPreviousExpandedPosition != position) {
            holder.fileInformation.setOnClickListener {
                mExpandedPosition = if (isExpanded) -1 else position
                notifyItemChanged(mPreviousExpandedPosition)
                notifyItemChanged(position)
            }
        }
    }

    override fun getItemCount(): Int {
        return listDatabaseFiles.size
    }

    fun addDatabaseFileHistoryList(listFileDatabaseHistoryToAdd: List<FileDatabaseHistoryEntity>) {
        listDatabaseFiles.clear()
        listDatabaseFiles.addAll(listFileDatabaseHistoryToAdd)
    }

    fun deleteDatabaseFileHistory(fileDatabaseHistoryToDelete: FileDatabaseHistoryEntity) {
        listDatabaseFiles.remove(fileDatabaseHistoryToDelete)
    }

    fun setOnItemClickListener(fileItemOpenListener: FileItemOpenListener) {
        this.fileItemOpenListener = fileItemOpenListener
    }

    fun setFileSelectClearListener(fileSelectClearListener: FileSelectClearListener) {
        this.fileSelectClearListener = fileSelectClearListener
    }

    interface FileItemOpenListener {
        fun onFileItemOpenListener(fileDatabaseHistoryEntity: FileDatabaseHistoryEntity)
    }

    interface FileSelectClearListener {
        fun onFileSelectClearListener(fileInfo: FileInfo): Boolean
    }

    private inner class FileItemClickListener(private val fileDatabaseHistoryEntity: FileDatabaseHistoryEntity) : View.OnClickListener {

        override fun onClick(v: View) {
            fileItemOpenListener?.onFileItemOpenListener(fileDatabaseHistoryEntity)
        }
    }

    private inner class ContextMenuBuilder(private val fileInfo: FileInfo) : View.OnCreateContextMenuListener {

        private val mOnMyActionClickListener = MenuItem.OnMenuItemClickListener { item ->
            if (fileSelectClearListener == null)
                return@OnMenuItemClickListener false
            when (item.itemId) {
                MENU_CLEAR -> fileSelectClearListener!!.onFileSelectClearListener(fileInfo)
                else -> false
            }
        }

        override fun onCreateContextMenu(contextMenu: ContextMenu?, view: View?, contextMenuInfo: ContextMenu.ContextMenuInfo?) {
            contextMenu?.add(Menu.NONE, MENU_CLEAR, Menu.NONE, R.string.remove_from_filelist)
                    ?.setOnMenuItemClickListener(mOnMyActionClickListener)
        }
    }

    inner class FileDatabaseHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var fileContainer: ViewGroup = itemView.findViewById(R.id.file_main_container)

        var fileAlias: TextView = itemView.findViewById(R.id.file_alias)
        var fileInformation: ImageView = itemView.findViewById(R.id.file_information)

        var fileExpandContainer: ViewGroup = itemView.findViewById(R.id.file_expand_container)
        var filePath: TextView = itemView.findViewById(R.id.file_path)
        var filePreciseInfoContainer: ViewGroup = itemView.findViewById(R.id.file_precise_info_container)
        var fileModification: TextView = itemView.findViewById(R.id.file_modification)
        var fileSize: TextView = itemView.findViewById(R.id.file_size)
    }

    companion object {

        private const val MENU_CLEAR = 1
    }
}
