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
import com.kunzisoft.keepass.viewmodels.EntryEditViewModel

/**
 * Dialog to warn the user that data will be overwritten.
 */
class OverwriteDataDialogFragment : DatabaseDialogFragment() {

    private val entryEditViewModel: EntryEditViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.warning_overwrite_data_title)
            .setMessage(R.string.warning_overwrite_data_description)
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                entryEditViewModel.backPressedAlreadyApproved = true
                entryEditViewModel.askToClose(EntryEditViewModel.CloseType.CANCEL_SPECIAL_MODE)
            }
            .setPositiveButton(android.R.string.ok) { _, _ -> }
            .create()
    }

    companion object {
        /**
         * Create a new instance of the fragment.
         */
        fun newInstance(): OverwriteDataDialogFragment {
            return OverwriteDataDialogFragment()
        }
    }
}
