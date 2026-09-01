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
import com.kunzisoft.keepass.utils.CharArrayUtil.clear
import com.kunzisoft.keepass.utils.CharArrayUtil.contains
import com.kunzisoft.keepass.utils.CharArrayUtil.indexOf
import com.kunzisoft.keepass.utils.CharArrayUtil.replace
import com.kunzisoft.keepass.utils.UUIDUtils.asHexCharArray
import com.kunzisoft.keepass.utils.UUIDUtils.asUUID
import java.util.concurrent.ConcurrentHashMap

class FieldReferencesEngine(private val mDatabase: DatabaseKDBX) {

    // Key : <WantedField>@<SearchIn>:<Text>
    // Value : content
    private var refsCache = ConcurrentHashMap<String, CharArray>()

    fun clear() {
        refsCache.values.forEach { it.clear() }
        refsCache.clear()
    }

    fun compile(entry: EntryKDBX, textReference: CharArray, recursionLevel: Int): CharArray {
        return if (recursionLevel >= MAX_RECURSION_DEPTH) {
            CharArray(0)
        } else {
            fillReferencesPlaceholders(entry, textReference, recursionLevel)
        }
    }

    /**
     * Manage placeholders with {REF:<WantedField>@<SearchIn>:<Text>}
     */
    private fun fillReferencesPlaceholders(
        currentEntry: EntryKDBX,
        textReference: CharArray,
        recursionLevel: Int
    ): CharArray {
        var textValue = textReference

        var offset = 0
        var numberInlineRef = 0
        while ((textValue.contains(STR_SELF_REF_START) || textValue.contains(STR_REF_START))
            && numberInlineRef <= MAX_INLINE_REF
        ) {
            val selfReference = textValue.contains(STR_SELF_REF_START)
            numberInlineRef++

            try {
                val nextTextValue = fillReferencesUsingCache(currentEntry, textValue)
                if (textValue !== textReference) {
                    textValue.clear()
                }
                textValue = nextTextValue

                val startingDelimiter = if (selfReference) STR_SELF_REF_START else STR_REF_START
                val endingDelimiter = STR_REF_END

                val start = textValue.indexOf(startingDelimiter, offset, ignoreCase = true)
                if (start < 0) {
                    break
                }
                val end = textValue.indexOf(endingDelimiter, offset, ignoreCase = true)
                if (end <= start) {
                    break
                }

                val reference = String(textValue, start + startingDelimiter.length, end - (start + startingDelimiter.length))
                val fullReference = "$startingDelimiter$reference$endingDelimiter".let {
                    if (selfReference) it + "@I:${currentEntry.id}"
                    else it
                }

                if (!refsCache.containsKey(fullReference)) {
                    val newRecursionLevel = recursionLevel + 1
                    val data: CharArray? = if (selfReference) {
                        reference.split(":")
                            .let { currentEntry
                                .getCustomFieldValue(it.last(), newRecursionLevel)
                            }
                    } else {
                        with(findReferenceTarget(reference, newRecursionLevel)) {
                            when (wanted) {
                                'T' -> entry?.decodeTitleKey(newRecursionLevel)
                                'U' -> entry?.decodeUsernameKey(newRecursionLevel)
                                'A' -> entry?.decodeUrlKey(newRecursionLevel)
                                'P' -> entry?.decodePasswordKey(newRecursionLevel)
                                'N' -> entry?.decodeNotesKey(newRecursionLevel)
                                'I' -> entry?.nodeId?.id?.asHexCharArray()
                                else -> null
                            }
                        }
                    }
                    refsCache[fullReference] = data?.copyOf() ?: CharArray(0)
                    
                    val updatedTextValue = fillReferencesUsingCache(currentEntry, textValue)
                    if (textValue !== textReference) {
                        textValue.clear()
                    }
                    textValue = updatedTextValue
                }

                offset = end
            } catch (e: Exception) {
                Log.e(TAG, "Error when fill placeholders by reference", e)
                break
            }
        }
        return textValue
    }

    private fun fillReferencesUsingCache(entry: EntryKDBX, text: CharArray): CharArray {
        var result = text
        refsCache.keys.forEach { key ->
            val placeholder = key.takeIf { it.startsWith(STR_SELF_REF_START, true) }
                ?.split("@")?.takeIf { it.last() == "I:${entry.id}" }?.first()
                ?: key

            val replacement = refsCache[key]
            if (replacement != null) {
                val nextResult = result.replace(placeholder, replacement, ignoreCase = true)
                if (nextResult !== result && result !== text) {
                    result.clear()
                }
                result = nextResult
            }
        }
        return result
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
            'P' -> mDatabase.getEntryByPassword(searchQuery.toCharArray(), recursionLevel)
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
