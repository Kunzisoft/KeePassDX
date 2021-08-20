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
package com.kunzisoft.keepass.activities.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.viewmodels.NodesViewModel

class DeleteNodesDialogFragment : DatabaseDialogFragment() {

    private var mNodesToDelete: List<Node> = listOf()
    private val mNodesViewModel: NodesViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        mNodesViewModel.nodesToDelete.observe(this) { nodes ->
            this.mNodesToDelete = nodes
        }
        var recycleBin = false
        arguments?.apply {
            if (containsKey(RECYCLE_BIN_TAG)) {
                recycleBin = this.getBoolean(RECYCLE_BIN_TAG)
            }
        }
        activity?.let { activity ->
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(activity)

            builder.setMessage(if (recycleBin)
                getString(R.string.warning_empty_recycle_bin)
            else
                getString(R.string.warning_permanently_delete_nodes))
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                mNodesViewModel.permanentlyDeleteNodes(mNodesToDelete)
            }
            builder.setNegativeButton(android.R.string.cancel) { _, _ -> dismiss() }
            // Create the AlertDialog object and return it
            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }

    companion object {
        private const val RECYCLE_BIN_TAG = "RECYCLE_BIN_TAG"

        fun getInstance(recycleBin: Boolean): DeleteNodesDialogFragment {
            return DeleteNodesDialogFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(RECYCLE_BIN_TAG, recycleBin)
                }
            }
        }
    }
}
