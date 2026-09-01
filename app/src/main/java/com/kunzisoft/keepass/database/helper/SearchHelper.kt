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

/**
 * Utility object for search-related operations in the app module.
 * Provides methods for displaying result counts and performing automated searches.
 */
object SearchHelper {

    /** Maximum number of search entries to process. */
    const val MAX_SEARCH_ENTRY = 1000

    /**
     * Formats the number of search results for display.
     * @param number The number of results.
     * @return A string representation, appended with "+" if the max is reached.
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
        val searchSubDomains = searchSubDomains(context)
        if (webDomain != null) {
            // Warning, the web domain may contain an IP, if so, do not crop it
            if (searchSubDomains
                || Regex(SearchInfo.WEB_IP_REGEX).matches(webDomain)) {
                concreteWebDomain.invoke(searchSubDomains, webDomain)
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    val publicSuffixList = PublicSuffixList(context)
                    val publicSuffix = publicSuffixList
                        .getPublicSuffixPlusOne(webDomain).await()
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
     * Asynchronously creates [SearchParameters] from a [SearchInfo] object.
     * @param context The application context.
     * @param callback Callback function that receives the generated [SearchParameters].
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
                    searchInTags = isTagSearch
                    tagsToSearch = tags
                    searchInTitles = false
                    searchInUsernames = false
                    searchInPasswords = false
                    searchInAppIds = isAppIdSearch
                    searchInUrls = isDomainSearch
                    searchByDomain = true
                    searchBySubDomain = searchSubDomains
                    searchInRelyingParty = isPasskeySearch
                    credentialIds = optionsString()
                    searchInNotes = false
                    searchInOTP = isOTPSearch
                    searchInOther = false
                    searchInUUIDs = false
                    searchInCurrentGroup = false
                    searchInSearchableGroup = true
                    searchInRecycleBin = false
                    searchInTemplates = false
                }
            )
        }
    }

    /**
     * Performs an automatic search based on [SearchInfo] and triggers callbacks based on results.
     * @param context The application context.
     * @param database The database to search in.
     * @param searchInfo Information defining the search.
     * @param onItemsFound Callback triggered when matches are found.
     * @param onItemNotFound Callback triggered when no matches are found.
     * @param onDatabaseClosed Callback triggered if the database is closed or unavailable.
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
                    database.createSearchGroupInfo(
                        searchParameters = searchParameters,
                        max = MAX_SEARCH_ENTRY
                    ).let { searchGroup ->
                        if (searchGroup.numberOfSearchResults() > 0) {
                            onItemsFound.invoke(
                                database,
                                searchGroup.getSearchResults()
                            )
                        } else
                            onItemNotFound.invoke(database)
                    }
                }
            } else
                onItemNotFound.invoke(database)
        }
    }
}
