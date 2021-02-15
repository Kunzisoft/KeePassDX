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

import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.getBundleFromListNodes

class EmptyRecycleBinDialogFragment : DeleteNodesDialogFragment() {

    override fun retrieveMessage(): String {
        return getString(R.string.warning_empty_recycle_bin)
    }

    companion object {
        fun getInstance(nodesToDelete: List<Node>): EmptyRecycleBinDialogFragment {
            return EmptyRecycleBinDialogFragment().apply {
                arguments = getBundleFromListNodes(nodesToDelete)
            }
        }
    }
}
