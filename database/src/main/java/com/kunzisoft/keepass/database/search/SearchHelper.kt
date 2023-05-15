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
package com.kunzisoft.keepass.database.search

import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.node.NodeHandler
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.otp.OtpEntryFields.OTP_FIELD
import com.kunzisoft.keepass.utils.UuidUtil

class SearchHelper {

    private var incrementEntry = 0

    fun createVirtualGroupWithSearchResult(database: Database,
                                           searchParameters: SearchParameters,
                                           fromGroup: NodeId<*>? = null,
                                           max: Int): Group? {

        val searchGroup = database.createGroup(virtual = true)
        searchGroup?.title = "\"" + searchParameters.searchQuery + "\""

        // Search all entries
        incrementEntry = 0

        val allowCustomSearchable = database.allowCustomSearchableGroup()
        val startGroup = if (searchParameters.searchInCurrentGroup && fromGroup != null) {
            database.getGroupById(fromGroup) ?: database.rootGroup
        } else {
            database.rootGroup
        }
        if (groupConditions(database, startGroup, searchParameters, allowCustomSearchable, max)) {
            startGroup?.doForEachChild(
                object : NodeHandler<Entry>() {
                    override fun operate(node: Entry): Boolean {
                        if (incrementEntry >= max)
                            return false
                        if (database.entryIsTemplate(node) && !searchParameters.searchInTemplates)
                            return false
                        if (entryContainsString(database, node, searchParameters)) {
                            searchGroup?.addChildEntry(node)
                            incrementEntry++
                        }
                        // Stop searching when we have max entries
                        return incrementEntry < max
                    }
                },
                object : NodeHandler<Group>() {
                    override fun operate(node: Group): Boolean {
                        return groupConditions(database,
                            node,
                            searchParameters,
                            allowCustomSearchable,
                            max
                        )
                    }
                },
                false
            )
        }

        searchGroup?.refreshNumberOfChildEntries()
        return searchGroup
    }

    private fun groupConditions(database: Database,
                                group: Group?,
                                searchParameters: SearchParameters,
                                allowCustomSearchable: Boolean,
                                max: Int): Boolean {
        return if (group == null)
            false
        else if (incrementEntry >= max)
            false
        else if (database.groupIsInRecycleBin(group))
            searchParameters.searchInRecycleBin
        else if (database.groupIsInTemplates(group))
            searchParameters.searchInTemplates
        else if (!allowCustomSearchable)
            true
        else if (searchParameters.searchInSearchableGroup)
            group.isSearchable()
        else
            true
    }

    private fun entryContainsString(database: Database,
                                    entry: Entry,
                                    searchParameters: SearchParameters): Boolean {
        // To search in field references
        database.startManageEntry(entry)
        // Search all strings in the entry
        val searchFound = searchInEntry(entry, searchParameters)
        database.stopManageEntry(entry)

        return searchFound
    }

    companion object {

        /**
         * Return true if the search query in search parameters is found in available parameters
         */
        fun searchInEntry(entry: Entry,
                          searchParameters: SearchParameters): Boolean {
            val searchQuery = searchParameters.searchQuery

            // Not found if the search string is empty
            if (searchQuery.isEmpty())
                return false

            // Exclude entry expired
            if (!searchParameters.searchInExpired) {
                if (entry.isCurrentlyExpires)
                    return false
            }

            // Search all strings in the KDBX entry
            if (searchParameters.searchInTitles) {
                if (checkSearchQuery(entry.title, searchParameters))
                    return true
            }
            if (searchParameters.searchInUsernames) {
                if (checkSearchQuery(entry.username, searchParameters))
                    return true
            }
            if (searchParameters.searchInPasswords) {
                if (checkSearchQuery(entry.password, searchParameters))
                    return true
            }
            if (searchParameters.searchInUrls) {
                if (checkSearchQuery(entry.url, searchParameters))
                    return true
            }
            if (searchParameters.searchInNotes) {
                if (checkSearchQuery(entry.notes, searchParameters))
                    return true
            }
            if (searchParameters.searchInUUIDs) {
                val hexString = UuidUtil.toHexString(entry.nodeId.id) ?: ""
                if (checkSearchQuery(hexString, searchParameters))
                    return true
            }
            if (searchParameters.searchInOther) {
                entry.getExtraFields().forEach { field ->
                    if (field.name != OTP_FIELD
                            || (field.name == OTP_FIELD && searchParameters.searchInOTP)) {
                        if (checkSearchQuery(field.protectedValue.toString(), searchParameters))
                            return true
                    }
                }
            }
            if (searchParameters.searchInTags) {
                if (checkSearchQuery(entry.tags.toString(), searchParameters))
                    return true
            }
            return false
        }

        private fun checkSearchQuery(stringToCheck: String, searchParameters: SearchParameters): Boolean {
            /*
            // TODO Search settings
            var removeAccents = true <- Too much time, to study
            */
            if (stringToCheck.isEmpty())
                return false
            return if (searchParameters.isRegex) {
                val regex = if (searchParameters.caseSensitive) {
                    searchParameters.searchQuery
                        .toRegex(RegexOption.DOT_MATCHES_ALL)
                } else {
                    searchParameters.searchQuery
                        .toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
                }
                regex.matches(stringToCheck)
            } else {
                var searchFound = true
                searchParameters.searchQuery.split(" ").forEach { word ->
                    searchFound = searchFound
                            && stringToCheck.contains(word, !searchParameters.caseSensitive)
                }
                searchFound
            }
        }
    }
}
