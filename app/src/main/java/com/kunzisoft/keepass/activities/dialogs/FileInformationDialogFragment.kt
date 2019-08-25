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
package com.kunzisoft.keepass.activities.dialogs

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.TextView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.adapters.FileInfo
import java.text.DateFormat

class FileInformationDialogFragment : DialogFragment() {

    private var fileSizeContainerView: View? = null
    private var fileModificationContainerView: View? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            val builder = AlertDialog.Builder(activity)
            val inflater = activity.layoutInflater
            val root = inflater.inflate(R.layout.fragment_file_selection_information, null)
            val fileNameView = root.findViewById<TextView>(R.id.file_filename)
            val filePathView = root.findViewById<TextView>(R.id.file_path)
            fileSizeContainerView = root.findViewById(R.id.file_size_container)
            val fileSizeView = root.findViewById<TextView>(R.id.file_size)
            fileModificationContainerView = root.findViewById(R.id.file_modification_container)
            val fileModificationView = root.findViewById<TextView>(R.id.file_modification)

            arguments?.apply {
                if (containsKey(FILE_SELECT_BEEN_ARG)) {
                    (getSerializable(FILE_SELECT_BEEN_ARG) as FileInfo?)?.let { fileDatabaseModel ->
                        fileDatabaseModel.fileUri?.let { fileUri ->
                            filePathView.text = Uri.decode(fileUri.toString())
                        }
                        fileNameView.text = fileDatabaseModel.fileName

                        if (fileDatabaseModel.notFound()) {
                            hideFileInfo()
                        } else {
                            showFileInfo()
                            fileSizeView.text = fileDatabaseModel.size.toString()
                            fileModificationView.text = DateFormat.getDateTimeInstance()
                                    .format(fileDatabaseModel.lastModification)
                        }
                    } ?: hideFileInfo()
                }
            }

            builder.setView(root)
            builder.setPositiveButton(android.R.string.ok) { _, _ -> }
            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }

    private fun showFileInfo() {
        fileSizeContainerView?.visibility = View.VISIBLE
        fileModificationContainerView?.visibility = View.VISIBLE
    }

    private fun hideFileInfo() {
        fileSizeContainerView?.visibility = View.GONE
        fileModificationContainerView?.visibility = View.GONE
    }

    companion object {

        private const val FILE_SELECT_BEEN_ARG = "FILE_SELECT_BEEN_ARG"

        fun newInstance(fileInfo: FileInfo): FileInformationDialogFragment {
            val fileInformationDialogFragment = FileInformationDialogFragment()
            val args = Bundle()
            args.putSerializable(FILE_SELECT_BEEN_ARG, fileInfo)
            fileInformationDialogFragment.arguments = args
            return fileInformationDialogFragment
        }
    }
}
