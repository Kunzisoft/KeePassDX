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
package com.kunzisoft.keepass.database.search

import com.kunzisoft.keepass.database.action.node.NodeHandler
import com.kunzisoft.keepass.database.element.entry.EntryKDBX
import com.kunzisoft.keepass.database.search.iterator.EntrySearchStringIteratorKDBX

class EntryKDBXSearchHandler(private val mSearchParametersKDBX: SearchParameters,
                             private val mListStorage: MutableList<EntryKDBX>)
    : NodeHandler<EntryKDBX>() {

    override fun operate(node: EntryKDBX): Boolean {

        if (mSearchParametersKDBX.excludeExpired
                && node.isCurrentlyExpires) {
            return true
        }

        if (searchStrings(node)) {
            mListStorage.add(node)
            return true
        }

        if (mSearchParametersKDBX.searchInGroupNames) {
            val parent = node.parent
            if (parent != null) {
                if (parent.title.contains(mSearchParametersKDBX.searchString, mSearchParametersKDBX.ignoreCase)) {
                    mListStorage.add(node)
                    return true
                }
            }
        }

        if (searchID(node)) {
            mListStorage.add(node)
            return true
        }

        return true
    }

    private fun searchID(entry: EntryKDBX): Boolean {
        if (mSearchParametersKDBX.searchInUUIDs) {
            val hex = UuidUtil.toHexString(entry.id)
            return hex.indexOf(mSearchParametersKDBX.searchString, 0, true) >= 0
        }

        return false
    }

    private fun searchStrings(entry: EntryKDBX): Boolean {
        val iterator = EntrySearchStringIteratorKDBX(entry, mSearchParametersKDBX)
        while (iterator.hasNext()) {
            val stringValue = iterator.next()
            if (stringValue.isNotEmpty()) {
                if (stringValue.contains(mSearchParametersKDBX.searchString, mSearchParametersKDBX.ignoreCase)) {
                    return true
                }
            }
        }

        return false
    }
}
