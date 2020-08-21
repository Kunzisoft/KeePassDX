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
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.model.DatabaseFile
import com.kunzisoft.keepass.view.collapse
import com.kunzisoft.keepass.view.expand

class FileDatabaseHistoryAdapter(context: Context)
    : RecyclerView.Adapter<FileDatabaseHistoryAdapter.FileDatabaseHistoryViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var defaultDatabaseListener: ((DatabaseFile?) -> Unit)? = null
    private var fileItemOpenListener: ((DatabaseFile)->Unit)? = null
    private var fileSelectClearListener: ((DatabaseFile)->Boolean)? = null
    private var saveAliasListener: ((DatabaseFile)->Unit)? = null

    private val listDatabaseFiles = ArrayList<DatabaseFile>()

    private var mDefaultDatabaseFile: DatabaseFile? = null
    private var mExpandedDatabaseFile: DatabaseFile? = null
    private var mPreviousExpandedDatabaseFile: DatabaseFile? = null

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
        val view = inflater.inflate(R.layout.item_file_info, parent, false)
        return FileDatabaseHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileDatabaseHistoryViewHolder, position: Int) {
        // Get info from position
        val databaseFile = listDatabaseFiles[position]

        // Click item to open file
        holder.fileContainer.setOnClickListener {
            fileItemOpenListener?.invoke(databaseFile)
        }

        // Default database
        holder.defaultFileButton.apply {
            this.isChecked = mDefaultDatabaseFile == databaseFile
            setOnClickListener {
                defaultDatabaseListener?.invoke(if (isChecked) databaseFile else null)
            }
        }

        // File alias
        holder.fileAlias.text = databaseFile.databaseAlias

        // File path
        holder.filePath.text = databaseFile.databaseDecodedPath

        if (databaseFile.databaseFileExists) {
            holder.fileInformationButton.clearColorFilter()
        } else {
            holder.fileInformationButton.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY)
        }

        // Modification
        databaseFile.databaseLastModified?.let {
            holder.fileModification.text = it
            holder.fileModification.visibility = View.VISIBLE
        } ?: run {
            holder.fileModification.visibility = View.GONE
        }

        // Size
        databaseFile.databaseSize?.let {
            holder.fileSize.text = it
            holder.fileSize.visibility = View.VISIBLE
        } ?: run {
            holder.fileSize.visibility = View.GONE
        }

        // Click on information
        val isExpanded = databaseFile == mExpandedDatabaseFile
        // Hides or shows info
        holder.fileExpandContainer.apply {
            if (isExpanded) {
                if (visibility != View.VISIBLE) {
                    visibility = View.VISIBLE
                    expand(true, resources.getDimensionPixelSize(R.dimen.item_file_info_height))
                }
            } else {
                collapse(true)
            }
        }

        // Save alias modification
        holder.fileAliasCloseButton.setOnClickListener {
            // Change the alias
            databaseFile.databaseAlias = holder.fileAliasEdit.text.toString()
            saveAliasListener?.invoke(databaseFile)

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
            fileSelectClearListener?.invoke(databaseFile)
        }

        if (isExpanded) {
            mPreviousExpandedDatabaseFile = databaseFile
        }
        holder.fileInformationButton.apply {
            animate().rotation(if (isExpanded) 180F else 0F).start()
            setOnClickListener {
                mExpandedDatabaseFile = if (isExpanded) null else databaseFile
                // Notify change
                val previousExpandedPosition = listDatabaseFiles.indexOf(mPreviousExpandedDatabaseFile)
                notifyItemChanged(previousExpandedPosition)
                val expandedPosition = listDatabaseFiles.indexOf(mExpandedDatabaseFile)
                notifyItemChanged(expandedPosition)
            }
        }

        // Refresh View / Close alias modification if not contains fileAlias
        if (holder.fileMainSwitcher.currentView.findViewById<View>(R.id.file_alias)
                != holder.fileAlias)
            holder.fileMainSwitcher.showPrevious()
    }

    override fun getItemCount(): Int {
        return listDatabaseFiles.size
    }

    fun clearDatabaseFileHistoryList() {
        listDatabaseFiles.clear()
    }

    fun addDatabaseFileHistory(fileDatabaseHistoryToAdd: DatabaseFile) {
        listDatabaseFiles.add(0, fileDatabaseHistoryToAdd)
        notifyItemInserted(0)
    }

    fun updateDatabaseFileHistory(fileDatabaseHistoryToUpdate: DatabaseFile) {
        val index = listDatabaseFiles.indexOf(fileDatabaseHistoryToUpdate)
        if (listDatabaseFiles.remove(fileDatabaseHistoryToUpdate)) {
            listDatabaseFiles.add(index, fileDatabaseHistoryToUpdate)
            notifyItemChanged(index)
        }
    }

    fun deleteDatabaseFileHistory(fileDatabaseHistoryToDelete: DatabaseFile) {
        val index = listDatabaseFiles.indexOf(fileDatabaseHistoryToDelete)
        if (listDatabaseFiles.remove(fileDatabaseHistoryToDelete)) {
            notifyItemRemoved(index)
        }
    }

    fun replaceAllDatabaseFileHistoryList(listFileDatabaseHistoryToAdd: List<DatabaseFile>) {
        if (listDatabaseFiles.isEmpty()) {
            listFileDatabaseHistoryToAdd.forEach {
                listDatabaseFiles.add(it)
                notifyItemInserted(listDatabaseFiles.size)
            }
        } else {
            listDatabaseFiles.clear()
            listDatabaseFiles.addAll(listFileDatabaseHistoryToAdd)
            notifyDataSetChanged()
        }
    }

    fun setDefaultDatabase(databaseUri: Uri?) {
        val defaultDatabaseFile = listDatabaseFiles.firstOrNull { it.databaseUri == databaseUri }
        mDefaultDatabaseFile = defaultDatabaseFile
        notifyDataSetChanged()
    }

    fun setOnDefaultDatabaseListener(listener: ((DatabaseFile?) -> Unit)?) {
        this.defaultDatabaseListener = listener
    }

    fun setOnFileDatabaseHistoryOpenListener(listener : ((DatabaseFile)->Unit)?) {
        this.fileItemOpenListener = listener
    }

    fun setOnFileDatabaseHistoryDeleteListener(listener : ((DatabaseFile)->Boolean)?) {
        this.fileSelectClearListener = listener
    }

    fun setOnSaveAliasListener(listener : ((DatabaseFile)->Unit)?) {
        this.saveAliasListener = listener
    }

    inner class FileDatabaseHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var fileContainer: ViewGroup = itemView.findViewById(R.id.file_container_basic_info)

        var defaultFileButton: CompoundButton = itemView.findViewById(R.id.default_file_button)
        var fileAlias: TextView = itemView.findViewById(R.id.file_alias)
        var fileInformationButton: ImageView = itemView.findViewById(R.id.file_information_button)

        var fileMainSwitcher: ViewSwitcher = itemView.findViewById(R.id.file_main_switcher)
        var fileAliasEdit: EditText = itemView.findViewById(R.id.file_alias_edit)
        var fileAliasCloseButton: ImageView = itemView.findViewById(R.id.file_alias_save)

        var fileExpandContainer: ViewGroup = itemView.findViewById(R.id.file_expand_container)
        var fileModifyButton: ImageView = itemView.findViewById(R.id.file_modify_button)
        var fileDeleteButton: ImageView = itemView.findViewById(R.id.file_delete_button)
        var filePath: TextView = itemView.findViewById(R.id.file_path)
        var fileModification: TextView = itemView.findViewById(R.id.file_modification)
        var fileSize: TextView = itemView.findViewById(R.id.file_size)
    }
}
