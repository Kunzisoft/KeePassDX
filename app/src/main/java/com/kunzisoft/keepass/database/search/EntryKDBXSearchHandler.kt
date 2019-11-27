/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.search

import com.kunzisoft.keepass.database.NodeHandler
import com.kunzisoft.keepass.database.element.EntryKDBX
import com.kunzisoft.keepass.database.search.iterator.EntrySearchStringIteratorKDBX
import com.kunzisoft.keepass.utils.StringUtil

import java.util.Locale

class EntryKDBXSearchHandler(private val mSearchParametersKDBX: SearchParametersKDBX, private val mListStorage: MutableList<EntryKDBX>) : NodeHandler<EntryKDBX>() {

    override fun operate(node: EntryKDBX): Boolean {

        if (mSearchParametersKDBX.excludeExpired
                && node.isCurrentlyExpires) {
            return true
        }

        var term = mSearchParametersKDBX.searchString
        if (mSearchParametersKDBX.ignoreCase) {
            term = term.toLowerCase()
        }

        if (searchStrings(node, term)) {
            mListStorage.add(node)
            return true
        }

        if (mSearchParametersKDBX.searchInGroupNames) {
            val parent = node.parent
            if (parent != null) {
                var groupName = parent.title
                if (mSearchParametersKDBX.ignoreCase) {
                    groupName = groupName.toLowerCase()
                }

                if (groupName.contains(term)) {
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
            return StringUtil.indexOfIgnoreCase(hex, mSearchParametersKDBX.searchString, Locale.ENGLISH) >= 0
        }

        return false
    }

    private fun searchStrings(entry: EntryKDBX, term: String): Boolean {
        val iterator = EntrySearchStringIteratorKDBX(entry, mSearchParametersKDBX)
        while (iterator.hasNext()) {
            var str = iterator.next()
            if (str.isNotEmpty()) {
                if (mSearchParametersKDBX.ignoreCase) {
                    str = str.toLowerCase()
                }

                if (str.contains(term)) {
                    return true
                }
            }
        }

        return false
    }
}
