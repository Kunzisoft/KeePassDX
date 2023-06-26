/*
 * Copyright 2022 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.activities.dialogs

import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.helpers.ExternalFileHelper
import com.kunzisoft.keepass.database.MainCredential
import com.kunzisoft.keepass.utils.UriUtil.getDocumentFile
import com.kunzisoft.keepass.utils.getParcelableCompat
import com.kunzisoft.keepass.view.MainCredentialView

class MainCredentialDialogFragment : DatabaseDialogFragment() {

    private var mainCredentialView: MainCredentialView? = null

    private var mListener: AskMainCredentialDialogListener? = null

    private var mExternalFileHelper: ExternalFileHelper? = null

    interface AskMainCredentialDialogListener {
        fun onAskMainCredentialDialogPositiveClick(databaseUri: Uri?, mainCredential: MainCredential)
        fun onAskMainCredentialDialogNegativeClick(databaseUri: Uri?, mainCredential: MainCredential)
    }

    override fun onAttach(activity: Context) {
        super.onAttach(activity)
        try {
            mListener = activity as AskMainCredentialDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException(activity.toString()
                    + " must implement " + AskMainCredentialDialogListener::class.java.name)
        }
    }

    override fun onDetach() {
        mListener = null
        super.onDetach()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->

            var databaseUri: Uri? = null
            arguments?.apply {
                if (containsKey(KEY_ASK_CREDENTIAL_URI))
                    databaseUri = getParcelableCompat(KEY_ASK_CREDENTIAL_URI)
            }

            val builder = AlertDialog.Builder(activity)

            val root = activity.layoutInflater.inflate(R.layout.fragment_main_credential, null)
            mainCredentialView = root.findViewById(R.id.main_credential_view)
            databaseUri?.let {
                root.findViewById<TextView>(R.id.title_database)?.text =
                    it.getDocumentFile(requireContext())?.name
            }
            builder.setView(root)
                    // Add action buttons
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        mListener?.onAskMainCredentialDialogPositiveClick(
                            databaseUri,
                            retrieveMainCredential()
                        )
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        mListener?.onAskMainCredentialDialogNegativeClick(
                            databaseUri,
                            retrieveMainCredential()
                        )
                    }


            mExternalFileHelper = ExternalFileHelper(this)
            mExternalFileHelper?.buildOpenDocument { uri ->
                if (uri != null) {
                    mainCredentialView?.populateKeyFileView(uri)
                }
            }
            mainCredentialView?.setOpenKeyfileClickListener(mExternalFileHelper)

            return builder.create()
        }

        return super.onCreateDialog(savedInstanceState)
    }

    private fun retrieveMainCredential(): MainCredential {
        return mainCredentialView?.getMainCredential() ?: MainCredential()
    }

    companion object {

        private const val KEY_ASK_CREDENTIAL_URI = "KEY_ASK_CREDENTIAL_URI"
        const val TAG_ASK_MAIN_CREDENTIAL = "TAG_ASK_MAIN_CREDENTIAL"

        fun getInstance(uri: Uri?): MainCredentialDialogFragment {
            val fragment = MainCredentialDialogFragment()
            val args = Bundle()
            args.putParcelable(KEY_ASK_CREDENTIAL_URI, uri)
            fragment.arguments = args
            return fragment
        }
    }
}
