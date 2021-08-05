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
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.viewmodels.NodesViewModel

open class DeleteNodesDialogFragment : DialogFragment() {

    private var mNodesToDelete: List<Node> = ArrayList()
    private val mNodesViewModel: NodesViewModel by activityViewModels()

    protected open fun retrieveMessage(): String {
        return getString(R.string.warning_permanently_delete_nodes)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        mNodesViewModel.nodesToDelete.observe(this) {
            this.mNodesToDelete = it
        }

        activity?.let { activity ->
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(activity)

            builder.setMessage(retrieveMessage())
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                mNodesViewModel.permanentlyDeleteNodes(mNodesToDelete)
            }
            builder.setNegativeButton(android.R.string.cancel) { _, _ -> dismiss() }
            // Create the AlertDialog object and return it
            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }
}
