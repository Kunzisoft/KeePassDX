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
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.utils.UuidUtil
import java.util.*

class FieldReferencesEngine(private val mDatabase: DatabaseKDBX) {

    private var refsCache: MutableMap<String, String> = HashMap()

    fun compile(textReference: String, recursionLevel: Int = 0): String {
        return if (recursionLevel >= MAX_RECURSION_DEPTH) {
            ""
        } else
            fillRefPlaceholders(textReference, recursionLevel)
    }

    private fun fillRefPlaceholders(textReference: String, recursionLevel: Int): String {
        var textValue = textReference

        var offset = 0
        var numberInlineRef = 0
        while (textValue.contains(STR_REF_START)
                && numberInlineRef <= MAX_INLINE_REF) {
            numberInlineRef++

            textValue = fillRefsUsingCache(textValue)

            val start = textValue.indexOf(STR_REF_START, offset, true)
            if (start < 0) {
                break
            }
            val end = textValue.indexOf(STR_REF_END, start + 1, true)
            if (end <= start) {
                break
            }

            val fullRef = textValue.substring(start, end + 1)
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
                    val innerContent = compile(data, recursionLevel + 1)
                    if (!refsCache.containsKey(fullRef)) {
                        refsCache[fullRef] = innerContent
                    }
                    textValue = fillRefsUsingCache(textValue)
                } else {
                    offset = start + 1
                }
            }
        }
        return textValue
    }

    private fun fillRefsUsingCache(text: String): String {
        var newText = text
        for ((key, value) in refsCache) {
            newText = text.replace(key, value, true)
        }
        return newText
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

        val searchIn = Character.toUpperCase(ref[2])
        val wantedField = Character.toUpperCase(ref[0])
        val searchQuery = ref.substring(4)
        val entrySearch: EntryKDBX? = when (searchIn) {
            'T' -> mDatabase.getEntryByTitle(searchQuery)
            'U' -> mDatabase.getEntryByUsername(searchQuery)
            'A' -> mDatabase.getEntryByURL(searchQuery)
            'P' -> mDatabase.getEntryByPassword(searchQuery)
            'N' -> mDatabase.getEntryByNotes(searchQuery)
            'I' -> {
                UuidUtil.fromHexString(searchQuery)?.let { uuid ->
                    mDatabase.getEntryById(NodeIdUUID(uuid))
                }
            }
            'O' -> mDatabase.getEntryByCustomData(searchQuery)
            else -> return null
        }
        return if (entrySearch != null) {
            TargetResult(entrySearch, wantedField)
        } else null
    }
    private data class TargetResult(var entry: EntryKDBX?, var wanted: Char)

    companion object {
        private const val MAX_RECURSION_DEPTH = 12
        private const val MAX_INLINE_REF = 5
        private const val STR_REF_START = "{REF:"
        private const val STR_REF_END = "}"
    }
}
