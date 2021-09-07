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

import android.content.Context
import com.kunzisoft.keepass.database.action.node.NodeHandler
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.otp.OtpEntryFields.OTP_FIELD
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.UuidUtil

class SearchHelper {

    private var incrementEntry = 0

    fun createVirtualGroupWithSearchResult(database: Database,
                                           searchParameters: SearchParameters,
                                           omitBackup: Boolean,
                                           max: Int): Group? {

        val searchGroup = database.createGroup()
        searchGroup?.isVirtual = true
        searchGroup?.title = "\"" + searchParameters.searchQuery + "\""

        // Search all entries
        incrementEntry = 0
        database.rootGroup?.doForEachChild(
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
                        return when {
                            incrementEntry >= max -> false
                            database.isGroupSearchable(node, omitBackup) -> true
                            else -> false
                        }
                    }
                },
                false)

        searchGroup?.refreshNumberOfChildEntries()
        return searchGroup
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
        const val MAX_SEARCH_ENTRY = 10

        /**
         * Utility method to perform actions if item is found or not after an auto search in [database]
         */
        fun checkAutoSearchInfo(context: Context,
                                database: Database?,
                                searchInfo: SearchInfo?,
                                onItemsFound: (openedDatabase: Database,
                                               items: List<EntryInfo>) -> Unit,
                                onItemNotFound: (openedDatabase: Database) -> Unit,
                                onDatabaseClosed: () -> Unit) {
            if (database == null || !database.loaded) {
                onDatabaseClosed.invoke()
            } else if (TimeoutHelper.checkTime(context)) {
                var searchWithoutUI = false
                if (PreferencesUtil.isAutofillAutoSearchEnable(context)
                        && searchInfo != null && !searchInfo.manualSelection
                        && !searchInfo.containsOnlyNullValues()) {
                    // If search provide results
                    database.createVirtualGroupFromSearchInfo(
                            searchInfo.toString(),
                            PreferencesUtil.omitBackup(context),
                            MAX_SEARCH_ENTRY
                    )?.let { searchGroup ->
                        if (searchGroup.numberOfChildEntries > 0) {
                            searchWithoutUI = true
                            onItemsFound.invoke(database,
                                    searchGroup.getChildEntriesInfo(database))
                        }
                    }
                }
                if (!searchWithoutUI) {
                    onItemNotFound.invoke(database)
                }
            }
        }

        /**
         * Return true if the search query in search parameters is found in available parameters
         */
        fun searchInEntry(entry: Entry,
                          searchParameters: SearchParameters): Boolean {
            val searchQuery = searchParameters.searchQuery
            // Entry don't contains string if the search string is empty
            if (searchQuery.isEmpty())
                return false

            // Search all strings in the KDBX entry
            if (searchParameters.searchInTitles) {
                if (checkSearchQuery(entry.title, searchParameters))
                    return true
            }
            if (searchParameters.searchInUserNames) {
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
                val hexString = UuidUtil.toHexString(entry.nodeId.id)
                if (hexString != null && hexString.contains(searchQuery, true))
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
            return false
        }

        private fun checkSearchQuery(stringToCheck: String, searchParameters: SearchParameters): Boolean {
            /*
            // TODO Search settings
            var regularExpression = false
            var ignoreCase = true
            var removeAccents = true <- Too much time, to study
            var excludeExpired = false
            var searchOnlyInCurrentGroup = false
            */
            return stringToCheck.isNotEmpty()
                    && stringToCheck.contains(
                        searchParameters.searchQuery, true)
        }
    }
}
