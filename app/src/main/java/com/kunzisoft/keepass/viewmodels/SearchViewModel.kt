/*
 * Copyright 2026 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Tags
import com.kunzisoft.keepass.database.element.node.DefaultNodeFilter
import com.kunzisoft.keepass.database.element.node.EmptyNodeFilter
import com.kunzisoft.keepass.database.element.node.NodeFilter
import com.kunzisoft.keepass.database.helper.SearchHelper
import com.kunzisoft.keepass.database.helper.SearchHelper.getSearchParametersFromSearchInfo
import com.kunzisoft.keepass.database.search.SearchParameters
import com.kunzisoft.keepass.model.GroupInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.model.SortedNodeInfo
import com.kunzisoft.keepass.settings.PreferencesUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * ViewModel for managing search.
 */
class SearchViewModel(application: Application): AndroidViewModel(application) {

    private var mDatabase: ContextualDatabase? = null

    /*
     * Manage search state
     */
    private var mCurrentGroup: GroupInfo? = null
    private var mSearchState = SearchState(
        searchParameters = getDefaultSearchParameters(),
        autoSearch = false,
        tempSearchInfo = false,
        firstVisibleItem = 0,
        firstVisibleItemOffset = 0
    )
    // To mainly manage keyboard
    val autoSearch: Boolean
        get() = mSearchState.autoSearch
    private var mRequestStartupSearch = true

    private val _searchUIState = MutableStateFlow<SearchUIState>(SearchUIState())
    val searchUIState: StateFlow<SearchUIState> = _searchUIState.asStateFlow()

    private val _searchActivated = MutableSharedFlow<Boolean>(replay = 0)
    val searchActivated: SharedFlow<Boolean> = _searchActivated.asSharedFlow()
    var isSearchActivated: Boolean = false
        private set

    private var mAutoFocusSearch: Boolean = PreferencesUtil.automaticallyFocusSearch(getApplication())
    private var mShowExpiredEntries: Boolean = PreferencesUtil.showExpiredEntries(getApplication())
    private var mShowTemplates: Boolean = PreferencesUtil.showTemplates(getApplication())
    private var mNodeFilter: NodeFilter = EmptyNodeFilter()

    fun saveSearchParameters() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (!mSearchState.tempSearchInfo) {
                    PreferencesUtil.setDefaultSearchParameters(
                        context = getApplication(),
                        searchParameters = mSearchState.searchParameters
                    )
                }
            }
        }
    }

    fun getDefaultSearchParameters(): SearchParameters {
        return PreferencesUtil.getDefaultSearchParameters(getApplication()).copy()
    }

    fun onDatabaseLoaded(database: ContextualDatabase) {
        this.mDatabase = database
        this.mNodeFilter = DefaultNodeFilter(
            database = mDatabase,
            showExpired = mShowExpiredEntries,
            showTemplates = mShowTemplates
        )
        loadSearch()
    }

    fun onMainGroupLoaded(mainGroup: GroupInfo? = null) {
        mainGroup?.let {
            mCurrentGroup = it
        }
        loadSearch()
    }

    fun loadSearch() {
        viewModelScope.launch {
            mDatabase?.let { database ->
                withContext(Dispatchers.IO) {
                    _searchUIState.update { groupState ->
                        groupState.copy(loaded = false)
                    }
                    val searchParameters = mSearchState.searchParameters
                    val searchGroup = database.createSearchGroupInfo(
                        searchParameters = searchParameters,
                        fromGroup = mCurrentGroup?.nodeId,
                        max = SearchHelper.MAX_SEARCH_ENTRY
                    )
                    val allowAdvancedSearch = !mSearchState.tempSearchInfo
                    val title = if (mSearchState.tempSearchInfo)
                        searchGroup.title
                    else mCurrentGroup?.title
                        ?: getApplication<Application>().getString(R.string.search)
                    val numberResults = SearchHelper.showNumberOfSearchResults(searchGroup.numberOfSearchResults())
                    val selectableTags = database.tagPoolWithoutHistory
                    val tagsAvailable = database.allowTags()
                    val tagsEnabled = database.tagPoolWithoutHistory.isNotEmpty()
                    val otherFieldsAvailable = database.allowEntryCustomFields()
                    val applicationIdsAvailable = database.allowEntryCustomFields()
                    val searchableGroupAvailable = database.allowCustomSearchableGroup()
                    val templatesAvailable = database.allowTemplatesGroup
                    val enableTemplates = database.templatesGroup != null
                    val children = searchGroup.getSearchResults()
                    withContext(Dispatchers.Main) {
                        _searchUIState.update { state ->
                            state.copy(
                                loaded = true,
                                children = children,
                                advanceSearchAllowed = allowAdvancedSearch,
                                searchParameters = searchParameters,
                                title = title,
                                numberResults = numberResults,
                                selectableTags = selectableTags,
                                tagsAvailable = tagsAvailable,
                                tagsEnabled = tagsEnabled,
                                otherFieldsAvailable = otherFieldsAvailable,
                                applicationIdsAvailable = applicationIdsAvailable,
                                searchableGroupAvailable = searchableGroupAvailable,
                                templatesAvailable = templatesAvailable,
                                enableTemplates = enableTemplates
                            )
                        }
                    }
                }
            }
        }
    }

    fun processSearchData(
        searchInfo: SearchInfo?,
        searchQuery: String?
    ) {
        if (mRequestStartupSearch) {
            // To request search only one time
            mRequestStartupSearch = false
            if (searchInfo != null) {
                // Get search query from search info
                searchInfo.getSearchParametersFromSearchInfo(getApplication()) { searchParameters ->
                    mSearchState = SearchState(
                        searchParameters = searchParameters,
                        autoSearch = true,
                        tempSearchInfo = true,
                        firstVisibleItem = mSearchState.firstVisibleItem,
                        firstVisibleItemOffset = mSearchState.firstVisibleItemOffset
                    )
                    loadSearch()
                    activateSearch()
                }
            } else if (searchQuery != null) {
                // Get search query from default intent parameter
                mSearchState = SearchState(
                    searchParameters = getDefaultSearchParameters().apply {
                        this.searchQuery = searchQuery.trim { it <= ' ' }
                    },
                    autoSearch = true,
                    tempSearchInfo = false,
                    firstVisibleItem = mSearchState.firstVisibleItem,
                    firstVisibleItemOffset = mSearchState.firstVisibleItemOffset
                )
                loadSearch()
                activateSearch()
            } else if (mAutoFocusSearch) {
                // Expand the search view if defined in settings
                mSearchState = SearchState(
                    searchParameters = getDefaultSearchParameters(),
                    autoSearch = false,
                    tempSearchInfo = false,
                    firstVisibleItem = mSearchState.firstVisibleItem ?: 0,
                    firstVisibleItemOffset = mSearchState.firstVisibleItemOffset
                )
                loadSearch()
                activateSearch()
            }
        }
    }

    fun searchText(text: String?) {
        if (text != null) {
            mSearchState.searchParameters.searchQuery = text
            mSearchState.firstVisibleItem = 0
            mSearchState.firstVisibleItemOffset = 0
            loadSearch()
        }
    }

    fun searchWithParameters(searchParameters: SearchParameters) {
        searchParameters.searchQuery = mSearchState.searchParameters.searchQuery
        mSearchState.searchParameters = searchParameters
        mSearchState.firstVisibleItem = 0
        mSearchState.firstVisibleItemOffset = 0
        saveSearchParameters()
        loadSearch()
    }

    fun activateSearch() {
        isSearchActivated = true
        viewModelScope.launch {
            _searchActivated.emit(true)
        }
    }

    fun deactivateSearch() {
        isSearchActivated = false
        viewModelScope.launch {
            _searchActivated.emit(false)
        }
    }

    private fun clearSearchState() {
        mSearchState = SearchState(
            searchParameters = getDefaultSearchParameters(),
            autoSearch = false,
            tempSearchInfo = false,
            firstVisibleItem = mSearchState.firstVisibleItem ?: 0,
            firstVisibleItemOffset = mSearchState.firstVisibleItemOffset
        )
    }

    fun clearSearch() {
        clearSearchState()
        deactivateSearch()
    }

    private data class SearchState(
        var searchParameters: SearchParameters,
        val autoSearch: Boolean,
        val tempSearchInfo: Boolean,
        var firstVisibleItem: Int?,
        var firstVisibleItemOffset: Int = 0
    )

    data class SearchUIState(
        val loaded: Boolean = false,
        val children: List<SortedNodeInfo>? = null,
        val searchParameters: SearchParameters? = null,
        val title: String = "",
        val numberResults: String = "0",
        val advanceSearchAllowed: Boolean = false,
        val selectableTags: Tags = Tags(),
        val tagsAvailable: Boolean = false,
        val tagsEnabled: Boolean = false,
        val otherFieldsAvailable: Boolean = false,
        val applicationIdsAvailable: Boolean = false,
        val searchableGroupAvailable: Boolean = false,
        val templatesAvailable: Boolean = false,
        val enableTemplates: Boolean = false
    )

    companion object {
        private val TAG = SearchViewModel::class.java.name
    }
}