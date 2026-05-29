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
package com.kunzisoft.keepass.activities.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.SortDialogFragment
import com.kunzisoft.keepass.adapters.NodesAdapter
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.retrieveSpecialMode
import com.kunzisoft.keepass.credentialprovider.SpecialMode
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.SortNodeEnum
import com.kunzisoft.keepass.model.SearchGroupInfo
import com.kunzisoft.keepass.model.SortedNodeInfo
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.viewmodels.GroupViewModel
import kotlinx.coroutines.launch

class GroupFragment : DatabaseFragment(), SortDialogFragment.SortSelectionListener {

    private var mNodesRecyclerView: RecyclerView? = null
    private var mLayoutManager: LinearLayoutManager? = null

    private var notFoundView: View? = null
    private var mAdapter: NodesAdapter? = null

    private val mGroupViewModel: GroupViewModel by activityViewModels()

    private var specialMode: SpecialMode = SpecialMode.DEFAULT

    private var mRecycleViewScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == SCROLL_STATE_IDLE) {
                mGroupViewModel.assignPosition(getFirstVisiblePosition())
            }
        }
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            mGroupViewModel.scrollTo(dy)
        }
    }

    private val menuProvider: MenuProvider = object: MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.tree, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menu_sort -> {
                    context?.let { context ->
                        val sortDialogFragment: SortDialogFragment =
                            SortDialogFragment.getInstance(
                                PreferencesUtil.getListSort(context),
                                PreferencesUtil.getAscendingSort(context),
                                PreferencesUtil.getGroupsBeforeSort(context),
                                if (mDatabase?.isRecycleBinEnabled == true) {
                                    PreferencesUtil.getRecycleBinBottomSort(context)
                                } else null
                            )
                        sortDialogFragment.show(childFragmentManager, "sortDialog")
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase) {
        context?.let { context ->
            mAdapter = NodesAdapter(context, database).apply {
                setOnNodeClickListener(object : NodesAdapter.NodeClickCallback {
                    override fun onNodeClick(database: ContextualDatabase, node: SortedNodeInfo) {
                        mGroupViewModel.performNodeClick(database, node)
                    }

                    override fun onNodeLongClick(database: ContextualDatabase, node: SortedNodeInfo): Boolean {
                        mGroupViewModel.performLongNodeClick(database, node)
                        return true
                    }
                })
                setActionNodes(mGroupViewModel.actionsNodes.value)
            }
            mNodesRecyclerView?.adapter = mAdapter
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        // To apply theme
        return inflater.inflate(R.layout.fragment_nodes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.addMenuProvider(menuProvider, viewLifecycleOwner)

        mNodesRecyclerView = view.findViewById(R.id.nodes_list)
        notFoundView = view.findViewById(R.id.not_found_container)

        mLayoutManager = LinearLayoutManager(context)
        mNodesRecyclerView?.apply {
            scrollBarStyle = View.SCROLLBARS_INSIDE_INSET
            layoutManager = mLayoutManager
            adapter = mAdapter
        }
        resetAppTimeoutWhenViewFocusedOrChanged(view)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mGroupViewModel.groupUIState.collect { groupUIState ->
                        try {
                            groupUIState.group?.let { currentGroup ->
                                groupUIState.children?.let { children ->
                                    mAdapter?.rebuildList(
                                        nodes = children,
                                        isSearch = currentGroup is SearchGroupInfo
                                    )
                                }
                                // Direct action node selection after rebuild
                                mAdapter?.setActionNodes(mGroupViewModel.actionsNodes.value)
                                if (currentGroup is SearchGroupInfo
                                    && mAdapter != null
                                    && mAdapter!!.isEmpty) {
                                    // To show the " no search entry found "
                                    notFoundView?.visibility = View.VISIBLE
                                } else {
                                    notFoundView?.visibility = View.GONE
                                }
                            }
                        } catch (e:Exception) {
                            Log.e(TAG, "Unable to rebuild the list", e)
                        }
                    }
                }
                launch {
                    mGroupViewModel.showPosition.collect { position ->
                        mNodesRecyclerView?.scrollToPosition(position)
                    }
                }
                launch {
                    mGroupViewModel.actionsNodes.collect { actionsNodes ->
                        mAdapter?.setActionNodes(actionsNodes)
                    }
                }
                launch {
                    mGroupViewModel.removeNodeAction.collect {
                        mAdapter?.unselectActionNodes()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        mNodesRecyclerView?.addOnScrollListener(mRecycleViewScrollListener)
        activity?.intent?.let {
            specialMode = it.retrieveSpecialMode()
        }
    }

    override fun onPause() {

        mNodesRecyclerView?.removeOnScrollListener(mRecycleViewScrollListener)
        super.onPause()
    }

    fun getFirstVisiblePosition(): Int {
        return mLayoutManager?.findFirstVisibleItemPosition() ?: 0
    }

    override fun onSortSelected(
        sortNodeEnum: SortNodeEnum,
        sortNodeParameters: SortNodeEnum.SortNodeParameters
    ) {
        // Save setting
        context?.let {
            PreferencesUtil.saveNodeSort(it, sortNodeEnum, sortNodeParameters)
        }

        // Tell the adapter to refresh its list
        try {
            mAdapter?.notifyChangeSort(sortNodeEnum, sortNodeParameters)
            mGroupViewModel.loadGroup()
        } catch (e:Exception) {
            Log.e(TAG, "Unable to sort the list", e)
        }
    }

    companion object {
        private val TAG = GroupFragment::class.java.name
    }
}
