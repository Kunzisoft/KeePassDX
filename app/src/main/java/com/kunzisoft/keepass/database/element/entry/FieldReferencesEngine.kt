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

    // Key : <WantedField>@<SearchIn>:<Text>
    // Value : content
    private var refsCache: MutableMap<String, String?> = HashMap()

    fun clear() {
        refsCache.clear()
    }

    fun compile(textReference: String, recursionLevel: Int): String {
        return if (recursionLevel >= MAX_RECURSION_DEPTH) {
            ""
        } else
            fillReferencesPlaceholders(textReference, recursionLevel)
    }

    /**
     * Manage placeholders with {REF:<WantedField>@<SearchIn>:<Text>}
     */
    private fun fillReferencesPlaceholders(textReference: String, recursionLevel: Int): String {
        var textValue = textReference

        var offset = 0
        var numberInlineRef = 0
        while (textValue.contains(STR_REF_START)
                && numberInlineRef <= MAX_INLINE_REF) {
            numberInlineRef++

            textValue = fillReferencesUsingCache(textValue)

            val start = textValue.indexOf(STR_REF_START, offset, true)
            if (start < 0) {
                break
            }
            val end = textValue.indexOf(STR_REF_END, offset, true)
            if (end <= start) {
                break
            }

            val reference = textValue.substring(start + STR_REF_START.length, end)
            val fullReference = "$STR_REF_START$reference$STR_REF_END"

            if (!refsCache.containsKey(fullReference)) {
                val result = findReferenceTarget(reference)
                val entryFound = result.entry
                val newRecursionLevel = recursionLevel + 1
                val data: String? = when (result.wanted) {
                    'T' -> entryFound?.decodeTitleKey(newRecursionLevel)
                    'U' -> entryFound?.decodeUsernameKey(newRecursionLevel)
                    'A' -> entryFound?.decodeUrlKey(newRecursionLevel)
                    'P' -> entryFound?.decodePasswordKey(newRecursionLevel)
                    'N' -> entryFound?.decodeNotesKey(newRecursionLevel)
                    'I' -> UuidUtil.toHexString(entryFound?.nodeId?.id)
                    else -> null
                }
                refsCache[fullReference] = data
                textValue = fillReferencesUsingCache(textValue)
            }

            offset = end
        }
        return textValue
    }

    private fun fillReferencesUsingCache(text: String): String {
        var newText = text
        for ((key, value) in refsCache) {
            // Replace by key if value not found
            newText = newText.replace(key, value ?: key, true)
        }
        return newText
    }

    private fun findReferenceTarget(reference: String): TargetResult {

        val targetResult = TargetResult(null, 'J')

        if (reference.length <= 4) {
            return targetResult
        }
        if (reference[1] != '@') {
            return targetResult
        }
        if (reference[3] != ':') {
            return targetResult
        }

        targetResult.wanted = Character.toUpperCase(reference[0])
        val searchIn = Character.toUpperCase(reference[2])
        val searchQuery = reference.substring(4)
        targetResult.entry = when (searchIn) {
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
            else -> null
        }
        return targetResult
    }

    private data class TargetResult(var entry: EntryKDBX?, var wanted: Char)

    companion object {
        private const val MAX_RECURSION_DEPTH = 10
        private const val MAX_INLINE_REF = 10
        private const val STR_REF_START = "{REF:"
        private const val STR_REF_END = "}"
    }
}
