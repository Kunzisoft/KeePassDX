/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.search.iterator

import com.kunzisoft.keepass.database.element.entry.EntryKDBX
import com.kunzisoft.keepass.database.search.SearchParametersKDBX
import com.kunzisoft.keepass.database.element.security.ProtectedString
import java.util.*
import kotlin.collections.Map.Entry

class EntrySearchStringIteratorKDBX : EntrySearchStringIterator {

    private var mCurrent: String? = null
    private var mSetIterator: Iterator<Entry<String, ProtectedString>>? = null
    private var mSearchParametersV4: SearchParametersKDBX

    constructor(entry: EntryKDBX) {
        this.mSearchParametersV4 = SearchParametersKDBX()
        mSetIterator = entry.fields.entries.iterator()
        advance()
    }

    constructor(entry: EntryKDBX, searchParametersV4: SearchParametersKDBX) {
        this.mSearchParametersV4 = searchParametersV4
        mSetIterator = entry.fields.entries.iterator()
        advance()
    }

    override fun hasNext(): Boolean {
        return mCurrent != null
    }

    override fun next(): String {
        if (mCurrent == null) {
            throw NoSuchElementException("Past the end of the list.")
        }

        val next:String = mCurrent!!
        advance()
        return next
    }

    private fun advance() {
        mSetIterator?.let {
            while (it.hasNext()) {
                val entry = it.next()
                val key = entry.key

                if (searchInField(key)) {
                    mCurrent = entry.value.toString()
                    return
                }
            }
        }

        mCurrent = null
    }

    private fun searchInField(key: String): Boolean {
        return when (key) {
            EntryKDBX.STR_TITLE -> mSearchParametersV4.searchInTitles
            EntryKDBX.STR_USERNAME -> mSearchParametersV4.searchInUserNames
            EntryKDBX.STR_PASSWORD -> mSearchParametersV4.searchInPasswords
            EntryKDBX.STR_URL -> mSearchParametersV4.searchInUrls
            EntryKDBX.STR_NOTES -> mSearchParametersV4.searchInNotes
            else -> mSearchParametersV4.searchInOther
        }
    }

}
