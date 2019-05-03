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
package com.kunzisoft.keepass.database.iterator

import com.kunzisoft.keepass.database.element.PwEntryV4
import com.kunzisoft.keepass.database.search.SearchParametersV4
import com.kunzisoft.keepass.database.security.ProtectedString
import java.util.*
import kotlin.collections.Map.Entry

class EntrySearchStringIteratorV4 : EntrySearchStringIterator {

    private var current: String? = null
    private var setIterator: Iterator<Entry<String, ProtectedString>>? = null
    private var sp: SearchParametersV4? = null

    constructor(entry: PwEntryV4) {
        this.sp = SearchParametersV4.DEFAULT
        setIterator = entry.fields.listOfAllFields.entries.iterator()
        advance()
    }

    constructor(entry: PwEntryV4, sp: SearchParametersV4) {
        this.sp = sp
        setIterator = entry.fields.listOfAllFields.entries.iterator()
        advance()
    }

    override fun hasNext(): Boolean {
        return current != null
    }

    override fun next(): String {
        if (current == null) {
            throw NoSuchElementException("Past the end of the list.")
        }

        val next = current
        advance()
        return next!!
    }

    private fun advance() {
        while (setIterator!!.hasNext()) {
            val entry = setIterator!!.next()

            val key = entry.key

            if (searchInField(key)) {
                current = entry.value.toString()
                return
            }

        }

        current = null
    }

    private fun searchInField(key: String): Boolean {
        when (key) {
            PwEntryV4.STR_TITLE -> return sp!!.searchInTitles
            PwEntryV4.STR_USERNAME -> return sp!!.searchInUserNames
            PwEntryV4.STR_PASSWORD -> return sp!!.searchInPasswords
            PwEntryV4.STR_URL -> return sp!!.searchInUrls
            PwEntryV4.STR_NOTES -> return sp!!.searchInNotes
            else -> return sp!!.searchInOther
        }
    }

}
