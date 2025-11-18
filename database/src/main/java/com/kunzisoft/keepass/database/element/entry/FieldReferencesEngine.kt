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

import android.util.Log
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.utils.UUIDUtils.asHexString
import com.kunzisoft.keepass.utils.UUIDUtils.asUUID
import java.util.concurrent.ConcurrentHashMap

class FieldReferencesEngine(private val mDatabase: DatabaseKDBX) {

    // Key : <WantedField>@<SearchIn>:<Text>
    // Value : content
    private var refsCache = ConcurrentHashMap<String, String?>()

    fun clear() {
        refsCache.clear()
    }

    fun compile(entry: EntryKDBX, textReference: String, recursionLevel: Int): String {
        return if (recursionLevel >= MAX_RECURSION_DEPTH) {
            ""
        } else {
            fillReferencesPlaceholders(entry, textReference, recursionLevel)
        }
    }

    /**
     * Manage placeholders with {REF:<WantedField>@<SearchIn>:<Text>}
     */
    private fun fillReferencesPlaceholders(
        currentEntry: EntryKDBX,
        textReference: String,
        recursionLevel: Int
    ): String {
        var textValue = textReference

        var offset = 0
        var numberInlineRef = 0
        while ((textValue.contains(STR_SELF_REF_START) || textValue.contains(STR_REF_START))
            && numberInlineRef <= MAX_INLINE_REF
        ) {
            val selfReference = textValue.contains(STR_SELF_REF_START)
            numberInlineRef++

            try {
                textValue = fillReferencesUsingCache(currentEntry, textValue)

                val startingDelimiter = if (selfReference) STR_SELF_REF_START else STR_REF_START
                val endingDelimiter = STR_REF_END

                val start = textValue.indexOf(startingDelimiter, offset, true)
                if (start < 0) {
                    break
                }
                val end = textValue.indexOf(endingDelimiter, offset, true)
                if (end <= start) {
                    break
                }

                val reference = textValue.substring(start + startingDelimiter.length, end)
                val fullReference = "$startingDelimiter$reference$endingDelimiter".let {
                    if (selfReference) it + "@I:${currentEntry.id}"
                    else it
                }

                if (!refsCache.containsKey(fullReference)) {
                    val newRecursionLevel = recursionLevel + 1
                    val data: String? = if (selfReference) {
                        reference.split(":")
                            .let { currentEntry.getCustomFieldValue(it.last(), newRecursionLevel) }
                    } else {
                        with(findReferenceTarget(reference, newRecursionLevel)) {
                            when (wanted) {
                                'T' -> entry?.decodeTitleKey(newRecursionLevel)
                                'U' -> entry?.decodeUsernameKey(newRecursionLevel)
                                'A' -> entry?.decodeUrlKey(newRecursionLevel)
                                'P' -> entry?.decodePasswordKey(newRecursionLevel)
                                'N' -> entry?.decodeNotesKey(newRecursionLevel)
                                'I' -> entry?.nodeId?.id?.asHexString()
                                else -> null
                            }
                        }
                    }
                    refsCache[fullReference] = data
                    textValue = fillReferencesUsingCache(currentEntry, textValue)
                }

                offset = end
            } catch (e: Exception) {
                Log.e(TAG, "Error when fill placeholders by reference", e)
            }
        }
        return textValue
    }

    private fun fillReferencesUsingCache(entry: EntryKDBX, text: String): String =
        refsCache.keys.fold(text) { expandedText, key ->
            // Since the cache is global, self-references are adjusted to include the id of the entry
            // as well, using the format <placeholder>@<entry-id>.
            // This removes the ID part, leaving only the expected placeholder and ensures that
            // the cached value matches the provided entry.
            val placeholder = key.takeIf { it.startsWith(STR_SELF_REF_START, true) }
                ?.split("@")?.takeIf { it.last() == "I:${entry.id}" }?.first()
                ?: key

            // Replace by original placeholder if value not found or entry id doesn't match
            expandedText.replace(placeholder, refsCache[key] ?: placeholder, true)
        }

    private fun findReferenceTarget(reference: String, recursionLevel: Int): TargetResult {

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
            'T' -> mDatabase.getEntryByTitle(searchQuery, recursionLevel)
            'U' -> mDatabase.getEntryByUsername(searchQuery, recursionLevel)
            'A' -> mDatabase.getEntryByURL(searchQuery, recursionLevel)
            'P' -> mDatabase.getEntryByPassword(searchQuery, recursionLevel)
            'N' -> mDatabase.getEntryByNotes(searchQuery, recursionLevel)
            'I' -> {
                searchQuery.asUUID()?.let { uuid ->
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
        private const val STR_SELF_REF_START = "{S:"
        private const val STR_REF_END = "}"

        private val TAG = FieldReferencesEngine::class.java.name
    }
}
