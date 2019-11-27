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
package com.kunzisoft.keepass.database.element.entry

import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.element.group.GroupKDBX
import com.kunzisoft.keepass.database.search.EntryKDBXSearchHandler
import com.kunzisoft.keepass.database.search.SearchParametersKDBX
import com.kunzisoft.keepass.utils.StringUtil
import java.util.*

class FieldReferencesEngine {

    inner class TargetResult(var entry: EntryKDBX?, var wanted: Char)

    private inner class SprContextV4 {

        var databaseV4: DatabaseKDBX? = null
        var entry: EntryKDBX
        var refsCache: MutableMap<String, String> = HashMap()

        internal constructor(db: DatabaseKDBX, entry: EntryKDBX) {
            this.databaseV4 = db
            this.entry = entry
        }

        internal constructor(source: SprContextV4) {
            this.databaseV4 = source.databaseV4
            this.entry = source.entry
            this.refsCache = source.refsCache
        }
    }

    fun compile(text: String, entry: EntryKDBX, database: DatabaseKDBX): String {
        return compileInternal(text, SprContextV4(database, entry), 0)
    }

    private fun compileInternal(text: String?, sprContextV4: SprContextV4?, recursionLevel: Int): String {
        if (text == null) {
            return ""
        }
        if (sprContextV4 == null) {
            return ""
        }
        return if (recursionLevel >= MAX_RECURSION_DEPTH) {
            ""
        } else fillRefPlaceholders(text, sprContextV4, recursionLevel)

    }

    private fun fillRefPlaceholders(textReference: String, contextV4: SprContextV4, recursionLevel: Int): String {
        var text = textReference

        if (contextV4.databaseV4 == null) {
            return text
        }

        var offset = 0
        for (i in 0..19) {
            text = fillRefsUsingCache(text, contextV4)

            val start = StringUtil.indexOfIgnoreCase(text, STR_REF_START, offset, Locale.ENGLISH)
            if (start < 0) {
                break
            }
            val end = StringUtil.indexOfIgnoreCase(text, STR_REF_END, start + 1, Locale.ENGLISH)
            if (end <= start) {
                break
            }

            val fullRef = text.substring(start, end - start + 1)
            val result = findRefTarget(fullRef, contextV4)

            if (result != null) {
                val found = result.entry
                val wanted = result.wanted

                var data: String? = null
                when (wanted) {
                    'T' -> data = found?.title
                    'U' -> data = found?.username
                    'A' -> data = found?.url
                    'P' -> data = found?.password
                    'N' -> data = found?.notes
                    'I' -> data = found?.nodeId.toString()
                }

                if (data != null && found != null) {
                    val subCtx = SprContextV4(contextV4)
                    subCtx.entry = found

                    val innerContent = compileInternal(data, subCtx, recursionLevel + 1)
                    addRefsToCache(fullRef, innerContent, contextV4)
                    text = fillRefsUsingCache(text, contextV4)
                } else {
                    offset = start + 1
                }
            }

        }

        return text
    }

    private fun findRefTarget(fullReference: String?, contextV4: SprContextV4): TargetResult? {
        var fullRef: String? = fullReference ?: return null

        fullRef = fullRef!!.toUpperCase(Locale.ENGLISH)
        if (!fullRef.startsWith(STR_REF_START) || !fullRef.endsWith(STR_REF_END)) {
            return null
        }

        val ref = fullRef.substring(STR_REF_START.length, fullRef.length - STR_REF_START.length - STR_REF_END.length)
        if (ref.length <= 4) {
            return null
        }
        if (ref[1] != '@') {
            return null
        }
        if (ref[3] != ':') {
            return null
        }

        val scan = Character.toUpperCase(ref[2])
        val wanted = Character.toUpperCase(ref[0])

        val searchParametersV4 = SearchParametersKDBX()
        searchParametersV4.setupNone()

        searchParametersV4.searchString = ref.substring(4)
        if (scan == 'T') {
            searchParametersV4.searchInTitles = true
        } else if (scan == 'U') {
            searchParametersV4.searchInUserNames = true
        } else if (scan == 'A') {
            searchParametersV4.searchInUrls = true
        } else if (scan == 'P') {
            searchParametersV4.searchInPasswords = true
        } else if (scan == 'N') {
            searchParametersV4.searchInNotes = true
        } else if (scan == 'I') {
            searchParametersV4.searchInUUIDs = true
        } else if (scan == 'O') {
            searchParametersV4.searchInOther = true
        } else {
            return null
        }

        val list = ArrayList<EntryKDBX>()
        // TODO type parameter
        searchEntries(contextV4.databaseV4!!.rootGroup, searchParametersV4, list)

        return if (list.size > 0) {
            TargetResult(list[0], wanted)
        } else null

    }

    private fun addRefsToCache(ref: String?, value: String?, ctx: SprContextV4?) {
        if (ref == null) {
            return
        }
        if (value == null) {
            return
        }
        if (ctx == null) {
            return
        }

        if (!ctx.refsCache.containsKey(ref)) {
            ctx.refsCache[ref] = value
        }
    }

    private fun fillRefsUsingCache(text: String, sprContextV4: SprContextV4): String {
        var newText = text
        for ((key, value) in sprContextV4.refsCache) {
            newText = StringUtil.replaceAllIgnoresCase(text, key, value, Locale.ENGLISH)
        }
        return newText
    }

    private fun searchEntries(root: GroupKDBX?, searchParametersV4: SearchParametersKDBX?, listStorage: MutableList<EntryKDBX>?) {
        if (searchParametersV4 == null) {
            return
        }
        if (listStorage == null) {
            return
        }

        val terms = StringUtil.splitStringTerms(searchParametersV4.searchString)
        if (terms.size <= 1 || searchParametersV4.regularExpression) {
            root!!.doForEachChild(EntryKDBXSearchHandler(searchParametersV4, listStorage), null)
            return
        }

        // Search longest term first
        val stringLengthComparator = Comparator<String> { lhs, rhs -> lhs.length - rhs.length }
        Collections.sort(terms, stringLengthComparator)

        val fullSearch = searchParametersV4.searchString
        var childEntries: List<EntryKDBX>? = root!!.getChildEntries()
        for (i in terms.indices) {
            val pgNew = ArrayList<EntryKDBX>()

            searchParametersV4.searchString = terms[i]

            var negate = false
            if (searchParametersV4.searchString.startsWith("-")) {
                searchParametersV4.searchString = searchParametersV4.searchString.substring(1)
                negate = searchParametersV4.searchString.isNotEmpty()
            }

            if (!root.doForEachChild(EntryKDBXSearchHandler(searchParametersV4, pgNew), null)) {
                childEntries = null
                break
            }

            val complement = ArrayList<EntryKDBX>()
            if (negate) {
                for (entry in childEntries!!) {
                    if (!pgNew.contains(entry)) {
                        complement.add(entry)
                    }
                }
                childEntries = complement
            } else {
                childEntries = pgNew
            }
        }

        if (childEntries != null) {
            listStorage.addAll(childEntries)
        }
        searchParametersV4.searchString = fullSearch
    }

    companion object {
        private const val MAX_RECURSION_DEPTH = 12
        private const val STR_REF_START = "{REF:"
        private const val STR_REF_END = "}"
    }
}
