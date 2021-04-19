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

import com.kunzisoft.keepass.database.action.node.NodeHandler
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.search.SearchHelper
import com.kunzisoft.keepass.database.search.SearchParameters
import com.kunzisoft.keepass.database.search.UuidUtil
import java.util.*

class FieldReferencesEngine {

    fun compile(text: String, entry: EntryKDBX, database: DatabaseKDBX): String {
        return compileInternal(text, SprContextKDBX(database, entry), 0)
    }

    private fun compileInternal(text: String?, sprContextKDBX: SprContextKDBX?, recursionLevel: Int): String {
        if (text == null) {
            return ""
        }
        if (sprContextKDBX == null) {
            return ""
        }
        return if (recursionLevel >= MAX_RECURSION_DEPTH) {
            ""
        } else fillRefPlaceholders(text, sprContextKDBX, recursionLevel)

    }

    private fun fillRefPlaceholders(textReference: String, contextKDBX: SprContextKDBX, recursionLevel: Int): String {
        var text = textReference

        if (contextKDBX.databaseKDBX == null) {
            return text
        }

        var offset = 0
        for (i in 0..19) {
            text = fillRefsUsingCache(text, contextKDBX)

            val start = text.indexOf(STR_REF_START, offset, true)
            if (start < 0) {
                break
            }
            val end = text.indexOf(STR_REF_END, start + 1, true)
            if (end <= start) {
                break
            }

            val fullRef = text.substring(start, end + 1)
            val result = findRefTarget(fullRef, contextKDBX)

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
                    val subCtx = SprContextKDBX(contextKDBX)
                    subCtx.entryKDBX = found

                    val innerContent = compileInternal(data, subCtx, recursionLevel + 1)
                    addRefsToCache(fullRef, innerContent, contextKDBX)
                    text = fillRefsUsingCache(text, contextKDBX)
                } else {
                    offset = start + 1
                }
            }

        }

        return text
    }

    private fun findRefTarget(fullReference: String?, contextKDBX: SprContextKDBX): TargetResult? {
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
        searchEntries(contextKDBX, searchParameters, list)

        return if (list.size > 0) {
            TargetResult(list[0], wanted)
        } else null

    }

    private fun addRefsToCache(ref: String?, value: String?, ctx: SprContextKDBX?) {
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

    private fun fillRefsUsingCache(text: String, sprContextKDBX: SprContextKDBX): String {
        var newText = text
        for ((key, value) in sprContextKDBX.refsCache) {
            newText = text.replace(key, value, true)
        }
        return newText
    }

    private fun searchEntries(contextKDBX: SprContextKDBX, searchParameters: SearchParameters?, listStorage: MutableList<EntryKDBX>?) {

        val root = contextKDBX.databaseKDBX?.rootGroup
        if (searchParameters == null) {
            return
        }
        if (listStorage == null) {
            return
        }

        val terms = splitStringTerms(searchParameters.searchString)
        if (terms.size <= 1 || searchParameters.regularExpression) {
            root!!.doForEachChild(EntryKDBXSearchHandler(contextKDBX, searchParameters, listStorage), null)
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

            if (!root.doForEachChild(EntryKDBXSearchHandler(contextKDBX, searchParameters, pgNew), null)) {
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

    inner class TargetResult(var entry: EntryKDBX?, var wanted: Char)

    private inner class SprContextKDBX {

        var databaseKDBX: DatabaseKDBX? = null
        var entryKDBX: EntryKDBX
        var refsCache: MutableMap<String, String> = HashMap()

        constructor(databaseKDBX: DatabaseKDBX, entry: EntryKDBX) {
            this.databaseKDBX = databaseKDBX
            this.entryKDBX = entry
        }

        constructor(source: SprContextKDBX) {
            this.databaseKDBX = source.databaseKDBX
            this.entryKDBX = source.entryKDBX
            this.refsCache = source.refsCache
        }
    }

    private class EntryKDBXSearchHandler(private val contextKDBX: SprContextKDBX,
                                         private val mSearchParametersKDBX: SearchParameters,
                                         private val mListStorage: MutableList<EntryKDBX>)
        : NodeHandler<EntryKDBX>() {

        override fun operate(node: EntryKDBX): Boolean {
            contextKDBX.databaseKDBX?.let {
                node.startToManageFieldReferences(it)
            }
            if (mSearchParametersKDBX.excludeExpired
                    && node.isCurrentlyExpires) {
                node.stopToManageFieldReferences()
                return true
            }
            if (searchStrings(node)) {
                mListStorage.add(node)
                node.stopToManageFieldReferences()
                return true
            }
            if (searchInGroupNames(node)) {
                mListStorage.add(node)
                node.stopToManageFieldReferences()
                return true
            }
            if (searchInUUID(node)) {
                mListStorage.add(node)
                node.stopToManageFieldReferences()
                return true
            }
            node.stopToManageFieldReferences()
            return true
        }

        private fun searchStrings(entry: EntryKDBX): Boolean {
            return SearchHelper.searchInEntry(entry, mSearchParametersKDBX)
        }

        private fun searchInGroupNames(entry: EntryKDBX): Boolean {
            if (mSearchParametersKDBX.searchInGroupNames) {
                val parent = entry.parent
                if (parent != null) {
                    return parent.title
                            .contains(mSearchParametersKDBX.searchString,
                                    mSearchParametersKDBX.ignoreCase)
                }
            }
            return false
        }

        private fun searchInUUID(entry: EntryKDBX): Boolean {
            if (mSearchParametersKDBX.searchInUUIDs) {
                return UuidUtil.toHexString(entry.id)
                        .contains(mSearchParametersKDBX.searchString, true)
            }
            return false
        }
    }

    companion object {
        private const val MAX_RECURSION_DEPTH = 12
        private const val STR_REF_START = "{REF:"
        private const val STR_REF_END = "}"
    }
}
