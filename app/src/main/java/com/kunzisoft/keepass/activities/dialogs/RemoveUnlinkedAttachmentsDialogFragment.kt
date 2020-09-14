/*
 * Copyright 2020 Jeremy Jamet / Kunzisoft.
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
import android.os.Bundle
import android.text.SpannableStringBuilder
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.kunzisoft.keepass.R

/**
 * Custom Dialog to confirm big file to upload
 */
class RemoveUnlinkedAttachmentsDialogFragment : DialogFragment() {

    private var mActionChooseListener: ActionChooseListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        mActionChooseListener = try {
            context as ActionChooseListener
        } catch (e: ClassCastException) {
            try {
                targetFragment as ActionChooseListener
            } catch (e: ClassCastException) {
                throw ClassCastException(context.toString()
                        + " must implement " + ActionChooseListener::class.java.name)
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        mActionChooseListener = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(activity)
            builder.setMessage(SpannableStringBuilder().apply {
                append(getString(R.string.warning_remove_unlinked_attachment))
                append("\n\n")
                append(getString(R.string.warning_sure_remove_data))
            })
            builder.setPositiveButton(android.R.string.yes) { _, _ ->
                mActionChooseListener?.onValidateRemoveUnlinkedAttachments()
            }
            builder.setNegativeButton(android.R.string.no) { _, _ ->
                dismiss()
            }
            // Create the AlertDialog object and return it
            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }

    interface ActionChooseListener {
        fun onValidateRemoveUnlinkedAttachments()
    }

    companion object {
        fun build(): RemoveUnlinkedAttachmentsDialogFragment {
            val fragment = RemoveUnlinkedAttachmentsDialogFragment()
            fragment.arguments = Bundle().apply {}
            return fragment
        }
    }
}
