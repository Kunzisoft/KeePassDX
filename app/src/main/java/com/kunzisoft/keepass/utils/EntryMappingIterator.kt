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

package com.kunzisoft.keepass.utils

import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Entry

/**
 * Lazily produces [Entry] objects from a record [reader] using a column-to-field [mapping].
 * Entry creation and field assignment happen on whichever thread calls [hasNext] / [next],
 * so the iterator should be advanced on a background thread.
 *
 * Password fields are stored as [CharArray] copies; the reader's scratch memory may be zeroed
 * as iteration advances, depending on the [reader] implementation. Call [close] (or exhaust
 * the iterator) to release the underlying [reader].
 */
class EntryMappingIterator(
    private val reader: CloseableIterator<Array<CharArray>>,
    private val mapping: Map<Int, FieldType>,
    private val database: ContextualDatabase,
) : CloseableIterator<Entry> {

    enum class FieldType { IGNORE, TITLE, USERNAME, PASSWORD, URL, NOTES }

    private var lookahead: Entry? = null
    private var primed = false

    override fun hasNext(): Boolean {
        prime()
        return lookahead != null
    }

    override fun next(): Entry {
        prime()
        val entry = lookahead ?: throw NoSuchElementException()
        lookahead = advance()
        return entry
    }

    override fun close() = reader.close()

    private fun prime() {
        if (!primed) {
            primed = true
            lookahead = advance()
        }
    }

    private fun advance(): Entry? {
        while (reader.hasNext()) {
            val record = reader.next()
            val entry = database.createEntry() ?: continue
            record.forEachIndexed { index, field ->
                when (mapping[index] ?: FieldType.IGNORE) {
                    FieldType.TITLE    -> entry.title    = String(field)
                    FieldType.USERNAME -> entry.username = String(field)
                    FieldType.PASSWORD -> entry.password = field.copyOf()
                    FieldType.URL      -> entry.url      = String(field)
                    FieldType.NOTES    -> entry.notes    = String(field)
                    FieldType.IGNORE   -> {}
                }
            }
            if (entry.title.isEmpty() && entry.url.isNotEmpty()) {
                entry.title = entry.url
            }
            if (entry.title.isNotEmpty() || entry.username.isNotEmpty()) {
                return entry
            }
        }
        return null
    }
}
