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

import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.group.GroupKDBX
import com.kunzisoft.keepass.database.search.SearchHelper
import com.kunzisoft.keepass.database.search.SearchParameters
import java.util.*

class FieldReferencesEngine(private val mRoot: GroupKDBX) {

    private var refsCache: MutableMap<String, String> = HashMap()

    fun compile(text: String): String {
        return compileInternal(text, 0)
    }

    private fun compileInternal(text: String, recursionLevel: Int): String {
        return if (recursionLevel >= MAX_RECURSION_DEPTH) {
            ""
        } else
            fillRefPlaceholders(text, recursionLevel)
    }

    private fun fillRefPlaceholders(textReference: String, recursionLevel: Int): String {
        var text = textReference

        var offset = 0
        for (i in 0..MAX_RECURSION_DEPTH) {
            text = fillRefsUsingCache(text)

            val start = text.indexOf(STR_REF_START, offset, true)
            if (start < 0) {
                break
            }
            val end = text.indexOf(STR_REF_END, start + 1, true)
            if (end <= start) {
                break
            }

            val fullRef = text.substring(start, end + 1)
            val result = findRefTarget(fullRef)

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
                    val innerContent = compileInternal(data, recursionLevel + 1)
                    if (!refsCache.containsKey(fullRef)) {
                        refsCache[fullRef] = innerContent
                    }
                    text = fillRefsUsingCache(text)
                } else {
                    offset = start + 1
                }
            }

        }

        return text
    }

    private fun findRefTarget(fullReference: String?): TargetResult? {
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

        val searchParameters = SearchParameters().apply {
            searchInTitles = false
            searchInUserNames = false
            searchInPasswords = false
            searchInUrls = false
            searchInNotes = false
            searchInOTP = false
            searchInOther = false
            searchInUUIDs = false
            searchInTags = false
        }
        searchParameters.searchQuery = ref.substring(4)
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

        val entrySearch: EntryKDBX? = mRoot.searchChildEntry { entry ->
            SearchHelper.searchInEntry(Entry(entry), searchParameters)
        }
        return if (entrySearch != null) {
            TargetResult(entrySearch, wanted)
        } else null
    }

    private fun fillRefsUsingCache(text: String): String {
        var newText = text
        for ((key, value) in refsCache) {
            newText = text.replace(key, value, true)
        }
        return newText
    }

    private data class TargetResult(var entry: EntryKDBX?, var wanted: Char)

    companion object {
        private const val MAX_RECURSION_DEPTH = 12
        private const val STR_REF_START = "{REF:"
        private const val STR_REF_END = "}"
    }
}
