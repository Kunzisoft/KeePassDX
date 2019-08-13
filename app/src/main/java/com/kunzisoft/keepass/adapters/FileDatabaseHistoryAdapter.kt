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
import com.kunzisoft.keepass.fileselect.FileDatabaseModel
import com.kunzisoft.keepass.settings.PreferencesUtil

class FileDatabaseHistoryAdapter(private val context: Context, private val listFiles: List<String>)
    : RecyclerView.Adapter<FileDatabaseHistoryAdapter.FileDatabaseHistoryViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var fileItemOpenListener: FileItemOpenListener? = null
    private var fileSelectClearListener: FileSelectClearListener? = null
    private var fileInformationShowListener: FileInformationShowListener? = null

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
        val fileDatabaseModel = FileDatabaseModel(context, listFiles[position])
        // Context menu creation
        holder.fileContainer.setOnCreateContextMenuListener(ContextMenuBuilder(fileDatabaseModel))
        // Click item to open file
        if (fileItemOpenListener != null)
            holder.fileContainer.setOnClickListener(FileItemClickListener(position))
        // Assign file name
        if (PreferencesUtil.isFullFilePathEnable(context))
            holder.fileName.text = Uri.decode(fileDatabaseModel.fileUri.toString())
        else
            holder.fileName.text = fileDatabaseModel.fileName
        holder.fileName.textSize = PreferencesUtil.getListTextSize(context)
        // Click on information
        if (fileInformationShowListener != null)
            holder.fileInformation.setOnClickListener(FileInformationClickListener(fileDatabaseModel))
    }

    override fun getItemCount(): Int {
        return listFiles.size
    }

    fun setOnItemClickListener(fileItemOpenListener: FileItemOpenListener) {
        this.fileItemOpenListener = fileItemOpenListener
    }

    fun setFileSelectClearListener(fileSelectClearListener: FileSelectClearListener) {
        this.fileSelectClearListener = fileSelectClearListener
    }

    fun setFileInformationShowListener(fileInformationShowListener: FileInformationShowListener) {
        this.fileInformationShowListener = fileInformationShowListener
    }

    interface FileItemOpenListener {
        fun onFileItemOpenListener(itemPosition: Int)
    }

    interface FileSelectClearListener {
        fun onFileSelectClearListener(fileDatabaseModel: FileDatabaseModel): Boolean
    }

    interface FileInformationShowListener {
        fun onClickFileInformation(fileDatabaseModel: FileDatabaseModel)
    }

    private inner class FileItemClickListener(private val position: Int) : View.OnClickListener {

        override fun onClick(v: View) {
            fileItemOpenListener?.onFileItemOpenListener(position)
        }
    }

    private inner class FileInformationClickListener(private val fileDatabaseModel: FileDatabaseModel) : View.OnClickListener {

        override fun onClick(view: View) {
            fileInformationShowListener?.onClickFileInformation(fileDatabaseModel)
        }
    }

    private inner class ContextMenuBuilder(private val fileDatabaseModel: FileDatabaseModel) : View.OnCreateContextMenuListener {

        private val mOnMyActionClickListener = MenuItem.OnMenuItemClickListener { item ->
            if (fileSelectClearListener == null)
                return@OnMenuItemClickListener false
            when (item.itemId) {
                MENU_CLEAR -> fileSelectClearListener!!.onFileSelectClearListener(fileDatabaseModel)
                else -> false
            }
        }

        override fun onCreateContextMenu(contextMenu: ContextMenu?, view: View?, contextMenuInfo: ContextMenu.ContextMenuInfo?) {
            contextMenu?.add(Menu.NONE, MENU_CLEAR, Menu.NONE, R.string.remove_from_filelist)
                    ?.setOnMenuItemClickListener(mOnMyActionClickListener)
        }
    }

    inner class FileDatabaseHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var fileContainer: View = itemView.findViewById(R.id.file_container)
        var fileName: TextView = itemView.findViewById(R.id.file_filename)
        var fileInformation: ImageView = itemView.findViewById(R.id.file_information)
    }

    companion object {

        private const val MENU_CLEAR = 1
    }
}
