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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.ViewSwitcher
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
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

    private var mDefaultDatabase: DatabaseFile? = null
    private var mExpandedDatabaseFile: SuperDatabaseFile? = null
    private var mPreviousExpandedDatabaseFile: SuperDatabaseFile? = null

    private val mListPosition = mutableListOf<SuperDatabaseFile>()
    private val mSortedListDatabaseFiles = SortedList(SuperDatabaseFile::class.java,
        object: SortedListAdapterCallback<SuperDatabaseFile>(this) {
            override fun compare(item1: SuperDatabaseFile, item2: SuperDatabaseFile): Int {
                val indexItem1 = mListPosition.indexOf(item1)
                val indexItem2 = mListPosition.indexOf(item2)
                return if (indexItem1 == -1 && indexItem2 == -1)
                    -1
                else if (indexItem1 < indexItem2)
                    -1
                else if (indexItem1 > indexItem2)
                    1
                else
                    0
            }

            override fun areContentsTheSame(oldItem: SuperDatabaseFile, newItem: SuperDatabaseFile): Boolean {
                val oldDatabaseFile = oldItem.databaseFile
                val newDatabaseFile = newItem.databaseFile
                return oldDatabaseFile.databaseUri == newDatabaseFile.databaseUri
                        && oldDatabaseFile.databaseDecodedPath == newDatabaseFile.databaseDecodedPath
                        && oldDatabaseFile.databaseAlias == newDatabaseFile.databaseAlias
                        && oldDatabaseFile.databaseFileExists == newDatabaseFile.databaseFileExists
                        && oldDatabaseFile.databaseLastModified == newDatabaseFile.databaseLastModified
                        && oldDatabaseFile.databaseSize == newDatabaseFile.databaseSize
                        && oldItem.default == newItem.default
            }

            override fun areItemsTheSame(item1: SuperDatabaseFile, item2: SuperDatabaseFile): Boolean {
                return item1.databaseFile == item2.databaseFile
            }
        }
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileDatabaseHistoryViewHolder {
        val view = inflater.inflate(R.layout.item_file_info, parent, false)
        return FileDatabaseHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileDatabaseHistoryViewHolder, position: Int) {
        // Get info from position
        val superDatabaseFile = mSortedListDatabaseFiles[position]
        val databaseFile = superDatabaseFile.databaseFile

        // Click item to open file
        holder.fileContainer.setOnClickListener {
            fileItemOpenListener?.invoke(databaseFile)
        }

        // Default database
        holder.defaultFileButton.apply {
            this.isChecked = superDatabaseFile.default
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
            holder.fileModificationContainer.visibility = View.VISIBLE
        } ?: run {
            holder.fileModificationContainer.visibility = View.GONE
        }

        // Size
        databaseFile.databaseSize?.let {
            holder.fileSize.text = it
            holder.fileSize.visibility = View.VISIBLE
        } ?: run {
            holder.fileSize.visibility = View.GONE
        }

        // Click on information
        val isExpanded = superDatabaseFile == mExpandedDatabaseFile
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
            mPreviousExpandedDatabaseFile = superDatabaseFile
        }
        holder.fileInformationButton.apply {
            animate().rotation(if (isExpanded) 180F else 0F).start()
            setOnClickListener {
                mExpandedDatabaseFile = if (isExpanded) null else superDatabaseFile
                // Notify change
                val previousExpandedPosition = mListPosition.indexOf(mPreviousExpandedDatabaseFile)
                notifyItemChanged(previousExpandedPosition)
                val expandedPosition = mListPosition.indexOf(mExpandedDatabaseFile)
                notifyItemChanged(expandedPosition)
            }
        }

        // Refresh View / Close alias modification if not contains fileAlias
        if (holder.fileMainSwitcher.currentView.findViewById<View>(R.id.file_alias)
                != holder.fileAlias)
            holder.fileMainSwitcher.showPrevious()
    }

    override fun getItemCount(): Int {
        return mSortedListDatabaseFiles.size()
    }

    fun clearDatabaseFileHistoryList() {
        mListPosition.clear()
        mSortedListDatabaseFiles.clear()
    }

    fun addDatabaseFileHistory(fileDatabaseHistoryToAdd: DatabaseFile) {
        val superToAdd = SuperDatabaseFile(fileDatabaseHistoryToAdd)
        mListPosition.add(0, superToAdd)
        mSortedListDatabaseFiles.add(superToAdd)
    }

    fun updateDatabaseFileHistory(fileDatabaseHistoryToUpdate: DatabaseFile) {
        val superToUpdate = SuperDatabaseFile(fileDatabaseHistoryToUpdate)
        val index = mListPosition.indexOf(superToUpdate)
        if (mListPosition.remove(superToUpdate)) {
            mListPosition.add(index, superToUpdate)
        }
        mSortedListDatabaseFiles.updateItemAt(index, superToUpdate)
    }

    fun deleteDatabaseFileHistory(fileDatabaseHistoryToDelete: DatabaseFile) {
        val superToDelete = SuperDatabaseFile(fileDatabaseHistoryToDelete)
        val index = mListPosition.indexOf(superToDelete)
        mListPosition.remove(superToDelete)
        mSortedListDatabaseFiles.removeItemAt(index)
    }

    fun replaceAllDatabaseFileHistoryList(listFileDatabaseHistoryToAdd: List<DatabaseFile>) {
        val superMapToReplace = listFileDatabaseHistoryToAdd.map {
            SuperDatabaseFile(it)
        }
        mListPosition.clear()
        mListPosition.addAll(superMapToReplace)
        mSortedListDatabaseFiles.replaceAll(superMapToReplace)
    }

    fun setDefaultDatabase(databaseUri: Uri?) {
        // Remove default from last item
        val oldDefaultDatabasePosition = mListPosition.indexOfFirst {
            it.default
        }
        if (oldDefaultDatabasePosition >= 0) {
            val oldDefaultDatabase = mListPosition[oldDefaultDatabasePosition].apply {
                default = false
            }
            mSortedListDatabaseFiles.updateItemAt(oldDefaultDatabasePosition, oldDefaultDatabase)
        }
        // Add default to new item
        val newDefaultDatabaseFilePosition = mListPosition.indexOfFirst {
            it.databaseFile.databaseUri == databaseUri
        }
        if (newDefaultDatabaseFilePosition >= 0) {
            val newDefaultDatabase = mListPosition[newDefaultDatabaseFilePosition].apply {
                default = true
            }
            mDefaultDatabase = newDefaultDatabase.databaseFile
            mSortedListDatabaseFiles.updateItemAt(newDefaultDatabaseFilePosition, newDefaultDatabase)
        }
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

    private inner class SuperDatabaseFile(
        var databaseFile: DatabaseFile,
        var default: Boolean = false
    ) {

        init {
            if (mDefaultDatabase == databaseFile)
                this.default = true
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SuperDatabaseFile) return false

            if (databaseFile != other.databaseFile) return false

            return true
        }

        override fun hashCode(): Int {
            return databaseFile.hashCode()
        }
    }

    class FileDatabaseHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

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
        var fileModificationContainer: ViewGroup = itemView.findViewById(R.id.file_modification_container)
        var fileModification: TextView = itemView.findViewById(R.id.file_modification)
        var fileSize: TextView = itemView.findViewById(R.id.file_size)
    }
}
