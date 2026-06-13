/*
 * Copyright 2024 Jeremy Jamet / Kunzisoft.
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
 */

package com.kunzisoft.keepass.activities.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.utils.getEnum
import com.kunzisoft.keepass.utils.putEnum
import com.kunzisoft.keepass.viewmodels.EntryEditViewModel

/**
 * Dialog to ask the user if they want to discard changes.
 */
class DiscardChangesDialogFragment : DatabaseDialogFragment() {

    private val entryEditViewModel: EntryEditViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val closeType = arguments?.getEnum<EntryEditViewModel.CloseType>(ARG_CLOSE_TYPE)

        return AlertDialog.Builder(requireContext())
            .setMessage(R.string.discard_changes)
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .setPositiveButton(R.string.discard) { _, _ ->
                closeType?.let {
                    entryEditViewModel.approveDiscardChanges(it)
                }
            }
            .create()
    }

    companion object {
        private const val ARG_CLOSE_TYPE = "ARG_CLOSE_TYPE"

        /**
         * Create a new instance of the fragment.
         * @param closeType The type of close action.
         */
        fun newInstance(closeType: EntryEditViewModel.CloseType): DiscardChangesDialogFragment {
            return DiscardChangesDialogFragment().apply {
                arguments = Bundle().apply {
                    putEnum(ARG_CLOSE_TYPE, closeType)
                }
            }
        }
    }
}
