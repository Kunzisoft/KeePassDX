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
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView
import android.util.TypedValue
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.ViewSwitcher
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryEntity
import com.kunzisoft.keepass.utils.FileDatabaseInfo
import com.kunzisoft.keepass.utils.UriUtil

class FileDatabaseHistoryAdapter(private val context: Context)
    : RecyclerView.Adapter<FileDatabaseHistoryAdapter.FileDatabaseHistoryViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var fileItemOpenListener: ((FileDatabaseHistoryEntity)->Unit)? = null
    private var fileSelectClearListener: ((FileDatabaseHistoryEntity)->Boolean)? = null
    private var saveAliasListener: ((FileDatabaseHistoryEntity)->Unit)? = null

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
        // Get info from position
        val fileHistoryEntity = listDatabaseFiles[position]
        val fileDatabaseInfo = FileDatabaseInfo(context, fileHistoryEntity.databaseUri)

        // Click item to open file
        if (fileItemOpenListener != null)
            holder.fileContainer.setOnClickListener {
                fileItemOpenListener?.invoke(fileHistoryEntity)
            }

        // File alias
        holder.fileAlias.text = fileDatabaseInfo.retrieveDatabaseAlias(fileHistoryEntity.databaseAlias)

        // File path
        holder.filePath.text = UriUtil.decode(fileDatabaseInfo.fileUri?.toString())

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

        // Save alias modification
        holder.fileAliasCloseButton.setOnClickListener {
            // Change the alias
            fileHistoryEntity.databaseAlias = holder.fileAliasEdit.text.toString()
            saveAliasListener?.invoke(fileHistoryEntity)

            // Finish save mode
            holder.fileMainSwitcher.showPrevious()
            // Refresh current position to show alias
            notifyItemChanged(position)
        }

        // Open alias modification
        holder.fileModifyButton.setOnClickListener {
            holder.fileAliasEdit.setText(holder.fileAlias.text)
            holder.fileMainSwitcher.showNext()
        }

        holder.fileDeleteButton.setOnClickListener {
            fileSelectClearListener?.invoke(fileHistoryEntity)
        }

        if (isExpanded) {
            mPreviousExpandedPosition = position
        }

        holder.fileInformation.setOnClickListener {
            mExpandedPosition = if (isExpanded) -1 else position

            // Notify change
            if (mPreviousExpandedPosition < itemCount)
                notifyItemChanged(mPreviousExpandedPosition)
            notifyItemChanged(position)
        }

        // Refresh View / Close alias modification if not contains fileAlias
        if (holder.fileMainSwitcher.currentView.findViewById<View>(R.id.file_alias)
                != holder.fileAlias)
            holder.fileMainSwitcher.showPrevious()
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

    fun setOnFileDatabaseHistoryOpenListener(listener : ((FileDatabaseHistoryEntity)->Unit)?) {
        this.fileItemOpenListener = listener
    }

    fun setOnFileDatabaseHistoryDeleteListener(listener : ((FileDatabaseHistoryEntity)->Boolean)?) {
        this.fileSelectClearListener = listener
    }

    fun setOnSaveAliasListener(listener : ((FileDatabaseHistoryEntity)->Unit)?) {
        this.saveAliasListener = listener
    }

    inner class FileDatabaseHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var fileContainer: ViewGroup = itemView.findViewById(R.id.file_container_basic_info)

        var fileAlias: TextView = itemView.findViewById(R.id.file_alias)
        var fileInformation: ImageView = itemView.findViewById(R.id.file_information)

        var fileMainSwitcher: ViewSwitcher = itemView.findViewById(R.id.file_main_switcher)
        var fileAliasEdit: EditText = itemView.findViewById(R.id.file_alias_edit)
        var fileAliasCloseButton: ImageView = itemView.findViewById(R.id.file_alias_save)

        var fileExpandContainer: ViewGroup = itemView.findViewById(R.id.file_expand_container)
        var fileModifyButton: ImageView = itemView.findViewById(R.id.file_modify_button)
        var fileDeleteButton: ImageView = itemView.findViewById(R.id.file_delete_button)
        var filePath: TextView = itemView.findViewById(R.id.file_path)
        var filePreciseInfoContainer: ViewGroup = itemView.findViewById(R.id.file_precise_info_container)
        var fileModification: TextView = itemView.findViewById(R.id.file_modification)
        var fileSize: TextView = itemView.findViewById(R.id.file_size)
    }
}
