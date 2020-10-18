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
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.getBundleFromListNodes
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.getListNodesFromBundle

open class DeleteNodesDialogFragment : DialogFragment() {

    private var mNodesToDelete: List<Node> = ArrayList()
    private var mListener: DeleteNodeListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            mListener = context as DeleteNodeListener
        } catch (e: ClassCastException) {
            throw ClassCastException(context.toString()
                    + " must implement " + DeleteNodeListener::class.java.name)
        }
    }

    override fun onDetach() {
        mListener = null
        super.onDetach()
    }

    protected open fun retrieveMessage(): String {
        return getString(R.string.warning_permanently_delete_nodes)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        arguments?.apply {
            if (containsKey(DatabaseTaskNotificationService.GROUPS_ID_KEY)
                    && containsKey(DatabaseTaskNotificationService.ENTRIES_ID_KEY)) {
                mNodesToDelete = getListNodesFromBundle(Database.getInstance(), this)
            }
        } ?: savedInstanceState?.apply {
            if (containsKey(DatabaseTaskNotificationService.GROUPS_ID_KEY)
                    && containsKey(DatabaseTaskNotificationService.ENTRIES_ID_KEY)) {
                mNodesToDelete = getListNodesFromBundle(Database.getInstance(), savedInstanceState)
            }
        }
        activity?.let { activity ->
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(activity)

            builder.setMessage(retrieveMessage())
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                mListener?.permanentlyDeleteNodes(mNodesToDelete)
            }
            builder.setNegativeButton(android.R.string.cancel) { _, _ -> dismiss() }
            // Create the AlertDialog object and return it
            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putAll(getBundleFromListNodes(mNodesToDelete))
    }

    interface DeleteNodeListener {
        fun permanentlyDeleteNodes(nodes: List<Node>)
    }

    companion object {
        fun getInstance(nodesToDelete: List<Node>): DeleteNodesDialogFragment {
            return DeleteNodesDialogFragment().apply {
                arguments = getBundleFromListNodes(nodesToDelete)
            }
        }
    }
}
