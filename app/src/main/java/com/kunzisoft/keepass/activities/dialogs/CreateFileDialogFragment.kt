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

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.kunzisoft.keepass.R


class CreateFileDialogFragment : DialogFragment() {

    private var pathFileNameView: TextView? = null
    private var positiveButton: Button? = null
    private var negativeButton: Button? = null

    private var mDefinePathDialogListener: DefinePathDialogListener? = null

    private var mUriPath: Uri? = null

    interface DefinePathDialogListener {
        fun onDefinePathDialogPositiveClick(pathFile: Uri?): Boolean
        fun onDefinePathDialogNegativeClick(pathFile: Uri?): Boolean
    }

    override fun onAttach(activity: Context?) {
        super.onAttach(activity)
        try {
            mDefinePathDialogListener = activity as DefinePathDialogListener?
        } catch (e: ClassCastException) {
            throw ClassCastException(activity?.toString()
                    + " must implement " + DefinePathDialogListener::class.java.name)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            val builder = AlertDialog.Builder(activity)
            val inflater = activity.layoutInflater

            val rootView = inflater.inflate(R.layout.fragment_file_creation, null)
            builder.setView(rootView)
                    .setTitle(R.string.create_keepass_file)
                    // Add action buttons
                    .setPositiveButton(android.R.string.ok) { _, _ -> }
                    .setNegativeButton(R.string.cancel) { _, _ -> }

            // Folder selection
            pathFileNameView = rootView.findViewById(R.id.create_document_path)

            rootView.findViewById<View>(R.id.container_create_document)
                    .setOnClickListener { createNewFile() }

            // Init path
            mUriPath = null

            val dialog = builder.create()

            dialog.setOnShowListener { _ ->
                positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
                positiveButton?.setOnClickListener { _ ->
                    mDefinePathDialogListener?.let {
                        if (it.onDefinePathDialogPositiveClick(mUriPath))
                            dismiss()
                    }
                }
                negativeButton?.setOnClickListener { _->
                    mDefinePathDialogListener?.let {
                        if (it.onDefinePathDialogNegativeClick(mUriPath)) {
                            dismiss()
                        }
                    }
                }
            }
            return dialog
        }
        return super.onCreateDialog(savedInstanceState)
    }

    /**
     * Create a new file by calling the content provider
     */
    private fun createNewFile() {
        startActivityForResult(Intent(
                // TODO pre kitkat
                // TODO orientation change
                Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/x-keepass"
                    putExtra(Intent.EXTRA_TITLE, getString(R.string.database_file_name_default) +
                            getString(R.string.database_file_extension_default))
                },
                CREATE_FILE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CREATE_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            mUriPath = data?.data
            mUriPath?.let {
                val file = data?.data
                if (file != null) {
                    pathFileNameView?.text = file.path
                }
            }
        }
    }

    companion object {
        private const val CREATE_FILE_REQUEST_CODE = 3853
    }
}
