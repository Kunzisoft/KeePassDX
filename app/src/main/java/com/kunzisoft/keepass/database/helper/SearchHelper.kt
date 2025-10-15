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
package com.kunzisoft.keepass.database.helper

import android.content.Context
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.search.SearchParameters
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.settings.PreferencesUtil.searchSubDomains
import com.kunzisoft.keepass.timeout.TimeoutHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.lib.publicsuffixlist.PublicSuffixList

object SearchHelper {

    const val MAX_SEARCH_ENTRY = 1000

    /**
     * Method to show the number of search results with max results
     */
    fun showNumberOfSearchResults(number: Int): String {
        return if (number >= MAX_SEARCH_ENTRY) {
            (MAX_SEARCH_ENTRY -1).toString() + "+"
        } else {
            number.toString()
        }
    }

    /**
     * Get the concrete web domain AKA without sub domain if needed
     */
    private fun getConcreteWebDomain(
        context: Context,
        webDomain: String?,
        concreteWebDomain: (searchSubDomains: Boolean, concreteWebDomain: String?) -> Unit
    ) {
        val domain = webDomain
        val searchSubDomains = searchSubDomains(context)
        if (domain != null) {
            // Warning, web domain can contains IP, don't crop in this case
            if (searchSubDomains
                || Regex(SearchInfo.WEB_IP_REGEX).matches(domain)) {
                concreteWebDomain.invoke(searchSubDomains, webDomain)
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    val publicSuffixList = PublicSuffixList(context)
                    val publicSuffix = publicSuffixList
                        .getPublicSuffixPlusOne(domain).await()
                    withContext(Dispatchers.Main) {
                        concreteWebDomain.invoke(false, publicSuffix)
                    }
                }
            }
        } else {
            concreteWebDomain.invoke(searchSubDomains, null)
        }
    }

    /**
     * Create search parameters asynchronously from [SearchInfo]
     */
    fun SearchInfo.getSearchParametersFromSearchInfo(
        context: Context,
        callback: (SearchParameters) -> Unit
    ) {
        getConcreteWebDomain(
            context,
            webDomain
        ) { searchSubDomains, concreteDomain ->
            var query = this.toString()
            if (isDomainSearch && concreteDomain != null)
                query = concreteDomain
            callback.invoke(
                SearchParameters().apply {
                    searchQuery = query
                    allowEmptyQuery = false
                    searchInTitles = false
                    searchInUsernames = false
                    searchInPasswords = false
                    searchInAppIds = isAppIdSearch
                    searchInUrls = isDomainSearch
                    searchByDomain = true
                    searchBySubDomain = searchSubDomains
                    searchInRelyingParty = isPasskeySearch
                    searchInNotes = false
                    searchInOTP = isOTPSearch
                    searchInOther = false
                    searchInUUIDs = false
                    searchInTags = isTagSearch
                    searchInCurrentGroup = false
                    searchInSearchableGroup = true
                    searchInRecycleBin = false
                    searchInTemplates = false
                }
            )
        }
    }

    /**
     * Utility method to perform actions if item is found or not after an auto search in [database]
     */
    fun checkAutoSearchInfo(
        context: Context,
        database: ContextualDatabase?,
        searchInfo: SearchInfo?,
        onItemsFound: (openedDatabase: ContextualDatabase,
                       items: List<EntryInfo>) -> Unit,
        onItemNotFound: (openedDatabase: ContextualDatabase) -> Unit,
        onDatabaseClosed: () -> Unit
    ) {
        // Do not place coroutine at start, bug in Passkey implementation
        if (database == null || !database.loaded) {
            onDatabaseClosed.invoke()
        } else if (TimeoutHelper.checkTime(context)) {
            if (searchInfo != null
                && !searchInfo.manualSelection
                && !searchInfo.containsOnlyNullValues()
            ) {
                searchInfo.getSearchParametersFromSearchInfo(context) { searchParameters ->
                    // If search provide results
                    database.createVirtualGroupFromSearchInfo(
                        searchParameters = searchParameters,
                        max = MAX_SEARCH_ENTRY
                    )?.let { searchGroup ->
                        if (searchGroup.numberOfChildEntries > 0) {
                            onItemsFound.invoke(
                                database,
                                searchGroup.getChildEntriesInfo(database)
                            )
                        } else
                            onItemNotFound.invoke(database)
                    } ?: onItemNotFound.invoke(database)
                }
            } else
                onItemNotFound.invoke(database)
        }
    }
}
