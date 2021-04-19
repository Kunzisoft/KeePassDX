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
import com.kunzisoft.keepass.database.element.entry.EntryKDB
import com.kunzisoft.keepass.database.element.entry.EntryKDBX
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.otp.OtpEntryFields
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.StringUtil.removeDiacriticalMarks

class SearchHelper {

    private var incrementEntry = 0

    fun createVirtualGroupWithSearchResult(database: Database,
                                           searchParameters: SearchParameters,
                                           omitBackup: Boolean,
                                           max: Int): Group? {

        val searchGroup = database.createGroup()
        searchGroup?.isVirtual = true
        searchGroup?.title = "\"" + searchParameters.searchString + "\""

        // Search all entries
        incrementEntry = 0
        database.rootGroup?.doForEachChild(
                object : NodeHandler<Entry>() {
                    override fun operate(node: Entry): Boolean {
                        if (incrementEntry >= max)
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

        return searchGroup
    }

    private fun entryContainsString(database: Database,
                                    entry: Entry,
                                    searchParameters: SearchParameters): Boolean {
        val searchQuery = searchParameters.searchString
        // Entry don't contains string if the search string is empty
        if (searchQuery.isEmpty())
            return false

        var searchFound = false

        database.startManageEntry(entry)
        // Search all strings in the entry
        entry.entryKDB?.let { entryKDB ->
            searchFound = searchInEntry(entryKDB, searchParameters)
        }
        entry.entryKDBX?.let { entryKDBX ->
            searchFound = searchInEntry(entryKDBX,  searchParameters)
        }
        database.stopManageEntry(entry)

        return searchFound
    }

    companion object {
        const val MAX_SEARCH_ENTRY = 10

        /**
         * Utility method to perform actions if item is found or not after an auto search in [database]
         */
        fun checkAutoSearchInfo(context: Context,
                                database: Database,
                                searchInfo: SearchInfo?,
                                onItemsFound: (items: List<EntryInfo>) -> Unit,
                                onItemNotFound: () -> Unit,
                                onDatabaseClosed: () -> Unit) {
            if (database.loaded && TimeoutHelper.checkTime(context)) {
                var searchWithoutUI = false
                if (PreferencesUtil.isAutofillAutoSearchEnable(context)
                        && searchInfo != null
                        && !searchInfo.containsOnlyNullValues()) {
                    // If search provide results
                    database.createVirtualGroupFromSearchInfo(
                            searchInfo.toString(),
                            PreferencesUtil.omitBackup(context),
                            MAX_SEARCH_ENTRY
                    )?.let { searchGroup ->
                        if (searchGroup.getNumberOfChildEntries() > 0) {
                            searchWithoutUI = true
                            onItemsFound.invoke(
                                    searchGroup.getChildEntriesInfo(database))
                        }
                    }
                }
                if (!searchWithoutUI) {
                    onItemNotFound.invoke()
                }
            } else {
                onDatabaseClosed.invoke()
            }
        }

        private fun checkString(stringToCheck: String, searchParameters: SearchParameters): Boolean {
            if (stringToCheck.isNotEmpty()
                    && stringToCheck
                            .removeDiacriticalMarks()
                            .contains(searchParameters.searchString
                                    .removeDiacriticalMarks(),
                                    searchParameters.ignoreCase)) {
                return true
            }
            return false
        }

        fun searchInEntry(entryKDB: EntryKDB,
                          searchParameters: SearchParameters): Boolean {
            // Search all strings in the KDBX entry
            when {
                searchParameters.searchInTitles -> {
                    if (checkString(entryKDB.title, searchParameters))
                        return true
                }
                searchParameters.searchInUrls -> {
                    if (checkString(entryKDB.url, searchParameters))
                        return true
                }
                searchParameters.searchInUserNames -> {
                    if (checkString(entryKDB.username, searchParameters))
                        return true
                }
                searchParameters.searchInNotes -> {
                    if (checkString(entryKDB.notes, searchParameters))
                        return true
                }
            }
            return false
        }

        fun searchInEntry(entryKDBX: EntryKDBX,
                          searchParameters: SearchParameters): Boolean {
            var searchFound = false
            // Search all strings in the KDBX entry
            EntryFieldsLoop@ for((key, value) in entryKDBX.fields) {
                if (entryKDBXKeyIsAllowedToSearch(key, searchParameters)) {
                    val currentString = value.toString()
                    if (checkString(currentString, searchParameters)) {
                        searchFound = true
                        break@EntryFieldsLoop
                    }
                }
            }
            return searchFound
        }

        private fun entryKDBXKeyIsAllowedToSearch(key: String, searchParameters: SearchParameters): Boolean {
            return when (key) {
                EntryKDBX.STR_TITLE -> searchParameters.searchInTitles
                EntryKDBX.STR_USERNAME -> searchParameters.searchInUserNames
                EntryKDBX.STR_PASSWORD -> searchParameters.searchInPasswords
                EntryKDBX.STR_URL -> searchParameters.searchInUrls
                EntryKDBX.STR_NOTES -> searchParameters.searchInNotes
                OtpEntryFields.OTP_FIELD -> searchParameters.searchInOTP
                else -> searchParameters.searchInOther
            }
        }
    }
}
