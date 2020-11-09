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
package com.kunzisoft.keepass.database.search.iterator

import com.kunzisoft.keepass.database.element.entry.EntryKDBX
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.database.search.SearchParameters
import com.kunzisoft.keepass.otp.OtpEntryFields
import java.util.*
import kotlin.collections.Map.Entry

class EntrySearchStringIteratorKDBX(
        entry: EntryKDBX,
        private val mSearchParameters: SearchParameters)
    : Iterator<String> {

    private var mCurrent: String? = null
    private var mSetIterator: Iterator<Entry<String, ProtectedString>>? = null

    init {
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
            EntryKDBX.STR_TITLE -> mSearchParameters.searchInTitles
            EntryKDBX.STR_USERNAME -> mSearchParameters.searchInUserNames
            EntryKDBX.STR_PASSWORD -> mSearchParameters.searchInPasswords
            EntryKDBX.STR_URL -> mSearchParameters.searchInUrls
            EntryKDBX.STR_NOTES -> mSearchParameters.searchInNotes
            OtpEntryFields.OTP_FIELD -> mSearchParameters.searchInOTP
            else -> mSearchParameters.searchInOther
        }
    }

}
