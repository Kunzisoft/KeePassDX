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
package com.kunzisoft.keepass.database.element.entry

import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.element.group.GroupKDBX
import com.kunzisoft.keepass.database.search.EntryKDBXSearchHandler
import com.kunzisoft.keepass.database.search.SearchParameters
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

            val start = text.indexOf(STR_REF_START, offset, true)
            if (start < 0) {
                break
            }
            val end = text.indexOf(STR_REF_END, start + 1, true)
            if (end <= start) {
                break
            }

            val fullRef = text.substring(start, end + 1)
            val result = findRefTarget(fullRef, contextV4)

            if (result != null) {
                val found = result.entry
                found?.stopToManageFieldReferences()
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

        val ref = fullRef.substring(STR_REF_START.length, fullRef.length - STR_REF_END.length)
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

        val searchParameters = SearchParameters()
        searchParameters.setupNone()

        searchParameters.searchString = ref.substring(4)
        when (scan) {
            'T' -> searchParameters.searchInTitles = true
            'U' -> searchParameters.searchInUserNames = true
            'A' -> searchParameters.searchInUrls = true
            'P' -> searchParameters.searchInPasswords = true
            'N' -> searchParameters.searchInNotes = true
            'I' -> searchParameters.searchInUUIDs = true
            'O' -> searchParameters.searchInOther = true
            else -> return null
        }

        val list = ArrayList<EntryKDBX>()
        searchEntries(contextV4.databaseV4?.rootGroup, searchParameters, list)

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
            newText = text.replace(key, value, true)
        }
        return newText
    }

    private fun searchEntries(root: GroupKDBX?, searchParameters: SearchParameters?, listStorage: MutableList<EntryKDBX>?) {
        if (searchParameters == null) {
            return
        }
        if (listStorage == null) {
            return
        }

        val terms = splitStringTerms(searchParameters.searchString)
        if (terms.size <= 1 || searchParameters.regularExpression) {
            root!!.doForEachChild(EntryKDBXSearchHandler(searchParameters, listStorage), null)
            return
        }

        // Search longest term first
        val stringLengthComparator = Comparator<String> { lhs, rhs -> lhs.length - rhs.length }
        Collections.sort(terms, stringLengthComparator)

        val fullSearch = searchParameters.searchString
        var childEntries: List<EntryKDBX>? = root!!.getChildEntries()
        for (i in terms.indices) {
            val pgNew = ArrayList<EntryKDBX>()

            searchParameters.searchString = terms[i]

            var negate = false
            if (searchParameters.searchString.startsWith("-")) {
                searchParameters.searchString = searchParameters.searchString.substring(1)
                negate = searchParameters.searchString.isNotEmpty()
            }

            if (!root.doForEachChild(EntryKDBXSearchHandler(searchParameters, pgNew), null)) {
                childEntries = null
                break
            }

            childEntries = if (negate) {
                val complement = ArrayList<EntryKDBX>()
                for (entry in childEntries!!) {
                    if (!pgNew.contains(entry)) {
                        complement.add(entry)
                    }
                }
                complement
            } else {
                pgNew
            }
        }

        if (childEntries != null) {
            listStorage.addAll(childEntries)
        }
        searchParameters.searchString = fullSearch
    }

    /**
     * Create a list of String by split text when ' ', '\t', '\r' or '\n' is found
     */
    private fun splitStringTerms(text: String?): List<String> {
        val list = ArrayList<String>()
        if (text == null) {
            return list
        }

        val stringBuilder = StringBuilder()
        var quoted = false

        for (element in text) {

            if ((element == ' ' || element == '\t' || element == '\r' || element == '\n') && !quoted) {

                val len = stringBuilder.length
                when {
                    len > 0 -> {
                        list.add(stringBuilder.toString())
                        stringBuilder.delete(0, len)
                    }
                    element == '\"' -> quoted = !quoted
                    else -> stringBuilder.append(element)
                }
            }
        }

        if (stringBuilder.isNotEmpty()) {
            list.add(stringBuilder.toString())
        }

        return list
    }

    companion object {
        private const val MAX_RECURSION_DEPTH = 12
        private const val STR_REF_START = "{REF:"
        private const val STR_REF_END = "}"
    }
}
