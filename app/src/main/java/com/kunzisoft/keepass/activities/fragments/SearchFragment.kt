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
package com.kunzisoft.keepass.activities.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.SortDialogFragment
import com.kunzisoft.keepass.adapters.NodesAdapter
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.SortNodeEnum
import com.kunzisoft.keepass.database.search.SearchParameters
import com.kunzisoft.keepass.model.SortedNodeInfo
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.BACK_PREVIOUS_KEYBOARD_ACTION
import com.kunzisoft.keepass.utils.KeyboardUtil.showKeyboard
import com.kunzisoft.keepass.view.SearchFiltersView
import com.kunzisoft.keepass.viewmodels.GroupViewModel
import com.kunzisoft.keepass.viewmodels.SearchViewModel
import kotlinx.coroutines.launch

class SearchFragment: DatabaseFragment() {

    private var searchBar: ViewGroup? = null
    private var searchView: SearchView? = null
    private var searchTitle: TextView? = null
    private var searchNumbers: TextView? = null
    private var searchFiltersView: SearchFiltersView? = null
    private var searchSort: ImageView? = null
    private var searchClose: ImageView? = null
    private var searchResult: RecyclerView? = null
    private var notFoundView: View? = null


    private var mLayoutManager: LinearLayoutManager? = null
    private var mAdapter: NodesAdapter? = null

    private val mSearchViewModel: SearchViewModel by activityViewModels()
    private val mGroupViewModel: GroupViewModel by activityViewModels()

    private val mOnSearchQueryTextListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
            onQueryTextChange(query)
            // Collapse the search filters
            searchFiltersView?.closeAdvancedFilters()
            // Close the keyboard
            mGroupViewModel.hideKeyboard()
            return true
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            mSearchViewModel.searchText(newText)
            return true
        }
    }

    private val mOnSearchTextFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
        if (!mSearchViewModel.autoSearch && hasFocus) {
            if (PreferencesUtil.isKeyboardPreviousSearchEnable(requireContext())) {
                // Change to the previous keyboard and show it
                context?.sendBroadcast(Intent(BACK_PREVIOUS_KEYBOARD_ACTION))
            }
            view.showKeyboard()
        }
    }

    private val mOnSearchFiltersChangeListener = object : ((SearchParameters) -> Unit) {
        override fun invoke(searchParameters: SearchParameters) {
            mSearchViewModel.searchWithParameters(searchParameters)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        searchBar = view.findViewById(R.id.search_bar)
        searchView = view.findViewById(R.id.search_view)
        searchTitle = view.findViewById(R.id.search_title)
        searchNumbers = view.findViewById(R.id.search_numbers)
        searchFiltersView = view.findViewById(R.id.search_filters)
        searchSort = view.findViewById(R.id.search_sort)
        searchClose = view.findViewById(R.id.search_close)
        searchResult = view.findViewById(R.id.search_result)
        notFoundView = view.findViewById(R.id.not_found_container)

        mLayoutManager = LinearLayoutManager(context)
        searchResult?.apply {
            scrollBarStyle = View.SCROLLBARS_INSIDE_INSET
            layoutManager = mLayoutManager
            adapter = mAdapter
        }

        searchSort?.setOnClickListener {
            SortDialogFragment.getInstance()
                .show(childFragmentManager, "sortDialog")
        }
        searchClose?.setOnClickListener {
            activity?.onBackPressed()
        }

        searchView?.onActionViewExpanded()

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mSearchViewModel.searchUIState.collect { searchState ->
                        searchState.searchParameters?.let { searchParameters ->
                            val query = searchParameters.searchQuery
                            if (searchState.advanceSearchAllowed) {
                                searchTitle?.visibility = View.GONE
                                searchView?.visibility = View.VISIBLE
                                // Do not rerun cycles if it is the same request
                                if (searchView?.query?.toString() != query) {
                                    searchView?.setQuery(query, false)
                                }
                                /*
                                searchView?.post {
                                    searchView?.requestFocus()
                                }*/
                                searchFiltersView?.apply {
                                    setCurrentGroupText(searchState.title)
                                    allowAdvancedSearch(true)
                                    setSelectableTags(searchState.selectableTags)
                                    availableTags(searchState.tagsAvailable)
                                    enableTags(searchState.tagsEnabled)
                                    availableOther(searchState.otherFieldsAvailable)
                                    availableApplicationIds(searchState.applicationIdsAvailable)
                                    availableSearchableGroup(searchState.searchableGroupAvailable)
                                    availableTemplates(searchState.templatesAvailable)
                                    enableTemplates(mDatabase?.templatesGroup != null)
                                }
                                searchFiltersView?.visibility = View.VISIBLE
                            } else {
                                searchView?.visibility = View.GONE
                                searchTitle?.visibility = View.VISIBLE
                                searchTitle?.text = query
                                searchFiltersView?.visibility = View.GONE
                            }
                            searchFiltersView?.searchParameters = searchParameters
                        }
                        searchNumbers?.text = searchState.numberResults

                        val children = searchState.children
                        if (children != null) {
                            mAdapter?.rebuildList(
                                nodes = children,
                                isSearch = true
                            )
                        }
                        // To show the " no search entry found "
                        notFoundView?.isVisible = children.isNullOrEmpty()
                    }
                }
                launch {
                    mGroupViewModel.viewEvent.collect { event ->
                        when (event) {
                            is GroupViewModel.GroupEvent.SortSelected -> {
                                // Tell the search group adapter to refresh its list
                                try {
                                    mAdapter?.notifyChangeSort(
                                        sortNodeEnum = event.sortNode.sortNodeEnum,
                                        sortNodeParameters = event.sortNode.sortNodeParameters,
                                        sortDatabaseParameters = event.sortNode.sortDatabaseParameters
                                    )
                                    mSearchViewModel.loadSearch()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Unable to sort the list", e)
                                }
                            }
                            else -> {}
                        }
                    }
                }
                launch {
                    mGroupViewModel.actionsNodes.collect { actionsNodes ->
                        mAdapter?.setActionNodes(actionsNodes)
                    }
                }
            }
        }
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase) {
        context?.let { context ->
            mAdapter = NodesAdapter(
                context,
                database.iconDrawableFactory,
                SortNodeEnum.SortDatabaseParameters(
                    recycleBinEnabled = database.isRecycleBinEnabled,
                    recycleBinId = database.recycleBin?.nodeId
                )
            ).apply {
                setOnNodeClickListener(object : NodesAdapter.NodeClickCallback {
                    override fun onNodeClick(node: SortedNodeInfo) {
                        mGroupViewModel.performNodeClick(node)
                    }
                    override fun onNodeLongClick(node: SortedNodeInfo): Boolean {
                        mGroupViewModel.performLongNodeClick(node)
                        return true
                    }
                })
                setActionNodes(mGroupViewModel.actionsNodes.value)
            }
            searchResult?.adapter = mAdapter
        }

        mSearchViewModel.onDatabaseLoaded(database)
    }

    override fun onResume() {
        super.onResume()
        searchView?.setOnQueryTextListener(mOnSearchQueryTextListener)
        searchView?.onFocusChangeListener = mOnSearchTextFocusChangeListener
        searchFiltersView?.onParametersChangeListener = mOnSearchFiltersChangeListener
    }

    override fun onPause() {
        super.onPause()

        searchFiltersView?.onParametersChangeListener = null
        searchView?.onFocusChangeListener = null
        searchView?.setOnQueryTextListener(null)
    }

    companion object {
        private val TAG = SearchFragment::class.simpleName
    }
}