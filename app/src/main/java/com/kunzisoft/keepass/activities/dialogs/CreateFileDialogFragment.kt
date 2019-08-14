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
import android.os.Environment
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView

import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.stylish.FilePickerStylishActivity
import com.kunzisoft.keepass.utils.UriUtil
import com.nononsenseapps.filepicker.FilePickerActivity
import com.nononsenseapps.filepicker.Utils

class CreateFileDialogFragment : DialogFragment(), AdapterView.OnItemSelectedListener {

    private val FILE_CODE = 3853

    private var folderPathView: EditText? = null
    private var fileNameView: EditText? = null
    private var positiveButton: Button? = null
    private var negativeButton: Button? = null

    private var mDefinePathDialogListener: DefinePathDialogListener? = null

    private var mDatabaseFileExtension: String? = null
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

            // To prevent crash issue #69 https://github.com/Kunzisoft/KeePassDX/issues/69
            val actionCopyBarCallback = object : ActionMode.Callback {

                override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                    positiveButton?.isEnabled = false
                    negativeButton?.isEnabled = false
                    return true
                }

                override fun onDestroyActionMode(mode: ActionMode) {
                    positiveButton?.isEnabled = true
                    negativeButton?.isEnabled = true
                }

                override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                    return true
                }

                override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                    return true
                }
            }

            // Folder selection
            val browseView = rootView.findViewById<View>(R.id.browse_button)
            folderPathView = rootView.findViewById(R.id.folder_path)
            folderPathView?.customSelectionActionModeCallback = actionCopyBarCallback
            fileNameView = rootView.findViewById(R.id.filename)
            fileNameView?.customSelectionActionModeCallback = actionCopyBarCallback

            val defaultPath = Environment.getExternalStorageDirectory().path + getString(R.string.database_file_path_default)
            folderPathView?.setText(defaultPath)
            browseView.setOnClickListener { _ ->
                Intent(context, FilePickerStylishActivity::class.java).apply {
                    putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)
                    putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true)
                    putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR)
                    putExtra(FilePickerActivity.EXTRA_START_PATH,
                            Environment.getExternalStorageDirectory().path)
                    startActivityForResult(this, FILE_CODE)
                }
            }

            // Init path
            mUriPath = null

            // Extension
            mDatabaseFileExtension = getString(R.string.database_file_extension_default)
            val spinner = rootView.findViewById<Spinner>(R.id.file_types)
            spinner.onItemSelectedListener = this

            // Spinner Drop down elements
            val fileTypes = resources.getStringArray(R.array.file_types)
            val dataAdapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, fileTypes)
            dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = dataAdapter
            // Or text if only one item https://github.com/Kunzisoft/KeePassDX/issues/105
            if (fileTypes.size == 1) {
                val params = spinner.layoutParams
                spinner.visibility = View.GONE
                val extensionTextView = TextView(context)
                extensionTextView.text = mDatabaseFileExtension
                extensionTextView.layoutParams = params
                val parentView = spinner.parent as ViewGroup
                parentView.addView(extensionTextView)
            }

            val dialog = builder.create()

            dialog.setOnShowListener { _ ->
                positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
                positiveButton?.setOnClickListener { _ ->
                    mDefinePathDialogListener?.let {
                        if (it.onDefinePathDialogPositiveClick(buildPath()))
                            dismiss()
                    }
                }
                negativeButton?.setOnClickListener { _->
                    mDefinePathDialogListener?.let {
                        if (it.onDefinePathDialogNegativeClick(buildPath())) {
                            dismiss()
                        }
                    }
                }
            }
            return dialog
        }
        return super.onCreateDialog(savedInstanceState)
    }

    private fun buildPath(): Uri? {
        if (folderPathView != null && fileNameView != null && mDatabaseFileExtension != null) {
            var path = Uri.Builder().path(folderPathView!!.text.toString())
                    .appendPath(fileNameView!!.text.toString() + mDatabaseFileExtension!!)
                    .build()
            context?.let { context ->
                path = UriUtil.translateUri(context, path)
            }
            return path
        }
        return null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_CODE && resultCode == Activity.RESULT_OK) {
            mUriPath = data?.data
            mUriPath?.let {
                val file = Utils.getFileForUri(it)
                folderPathView?.setText(file.path)
            }
        }
    }

    override fun onItemSelected(adapterView: AdapterView<*>, view: View, position: Int, id: Long) {
        mDatabaseFileExtension = adapterView.getItemAtPosition(position).toString()
    }

    override fun onNothingSelected(adapterView: AdapterView<*>) {
        // Do nothing
    }
}
