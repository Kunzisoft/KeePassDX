package com.kunzisoft.keepass.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup

import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.adapters.NodeAdapter
import com.kunzisoft.keepass.database.SortNodeEnum
import com.kunzisoft.keepass.database.element.GroupVersioned
import com.kunzisoft.keepass.database.element.NodeVersioned
import com.kunzisoft.keepass.activities.dialogs.SortDialogFragment
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.activities.stylish.StylishFragment
import com.kunzisoft.keepass.activities.helpers.ReadOnlyHelper
import com.kunzisoft.keepass.database.element.Database

class ListNodesFragment : StylishFragment(), SortDialogFragment.SortSelectionListener {

    private var nodeClickCallback: NodeAdapter.NodeClickCallback? = null
    private var nodeMenuListener: NodeAdapter.NodeMenuListener? = null
    private var onScrollListener: OnScrollListener? = null

    private var listView: RecyclerView? = null
    var mainGroup: GroupVersioned? = null
        private set
    private var mAdapter: NodeAdapter? = null

    private var notFoundView: View? = null
    private var isASearchResult: Boolean = false

    // Preferences for sorting
    private var prefs: SharedPreferences? = null

    private var readOnly: Boolean = false
        get() {
            return field || selectionMode
        }
    private var selectionMode: Boolean = false

    val isEmpty: Boolean
        get() = mAdapter == null || mAdapter?.itemCount?:0 <= 0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            nodeClickCallback = context as NodeAdapter.NodeClickCallback
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException(context.toString()
                    + " must implement " + NodeAdapter.NodeClickCallback::class.java.name)
        }

        try {
            nodeMenuListener = context as NodeAdapter.NodeMenuListener
        } catch (e: ClassCastException) {
            nodeMenuListener = null
            // Context menu can be omit
            Log.w(TAG, context.toString()
                    + " must implement " + NodeAdapter.NodeMenuListener::class.java.name)
        }

        try {
            onScrollListener = context as OnScrollListener
        } catch (e: ClassCastException) {
            onScrollListener = null
            // Context menu can be omit
            Log.w(TAG, context.toString()
                    + " must implement " + RecyclerView.OnScrollListener::class.java.name)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.let { currentActivity ->
            setHasOptionsMenu(true)

            readOnly = ReadOnlyHelper.retrieveReadOnlyFromInstanceStateOrArguments(savedInstanceState, arguments)

            arguments?.let { args ->
                // Contains all the group in element
                if (args.containsKey(GROUP_KEY)) {
                    mainGroup = args.getParcelable(GROUP_KEY)
                }
                if (args.containsKey(IS_SEARCH)) {
                    isASearchResult = args.getBoolean(IS_SEARCH)
                }
            }

            contextThemed?.let { context ->
                mAdapter = NodeAdapter(context, currentActivity.menuInflater)
                mAdapter?.apply {
                    setReadOnly(readOnly)
                    setIsASearchResult(isASearchResult)
                    setOnNodeClickListener(nodeClickCallback)
                    setActivateContextMenu(true)
                    setNodeMenuListener(nodeMenuListener)
                }
            }
            prefs = PreferenceManager.getDefaultSharedPreferences(context)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        ReadOnlyHelper.onSaveInstanceState(outState, readOnly)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        // To apply theme
        val rootView = inflater.cloneInContext(contextThemed)
                .inflate(R.layout.fragment_list_nodes, container, false)
        listView = rootView.findViewById(R.id.nodes_list)
        notFoundView = rootView.findViewById(R.id.not_found_container)

        onScrollListener?.let { onScrollListener ->
            listView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    onScrollListener.onScrolled(dy)
                }
            })
        }

        rebuildList()

        return rootView
    }

    override fun onResume() {
        super.onResume()

        activity?.intent?.let {
            selectionMode = EntrySelectionHelper.retrieveEntrySelectionModeFromIntent(it)
        }
        // Force read only mode if selection mode
        mAdapter?.apply {
            setReadOnly(readOnly)
        }

        // Refresh data
        mAdapter?.notifyDataSetChanged()

        if (isASearchResult && mAdapter!= null && mAdapter!!.isEmpty) {
            // To show the " no search entry found "
            listView?.visibility = View.GONE
            notFoundView?.visibility = View.VISIBLE
        } else {
            listView?.visibility = View.VISIBLE
            notFoundView?.visibility = View.GONE
        }
    }

    fun rebuildList() {
        // Add elements to the list
        mainGroup?.let { mainGroup ->
            mAdapter?.rebuildList(mainGroup)
        }
        listView?.apply {
            scrollBarStyle = View.SCROLLBARS_INSIDE_INSET
            layoutManager = LinearLayoutManager(context)
            adapter = mAdapter
        }
    }

    override fun onSortSelected(sortNodeEnum: SortNodeEnum, ascending: Boolean, groupsBefore: Boolean, recycleBinBottom: Boolean) {
        // Toggle setting
        prefs?.edit()?.apply {
            putString(getString(R.string.sort_node_key), sortNodeEnum.name)
            putBoolean(getString(R.string.sort_ascending_key), ascending)
            putBoolean(getString(R.string.sort_group_before_key), groupsBefore)
            putBoolean(getString(R.string.sort_recycle_bin_bottom_key), recycleBinBottom)
            apply()
        }

        // Tell the adapter to refresh it's list
        mAdapter?.notifyChangeSort(sortNodeEnum, ascending, groupsBefore)
        mainGroup?.let { mainGroup ->
            mAdapter?.rebuildList(mainGroup)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.tree, menu)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.menu_sort -> {
                context?.let { context ->
                    val sortDialogFragment: SortDialogFragment =
                            if (Database.getInstance().isRecycleBinAvailable
                                    && Database.getInstance().isRecycleBinEnabled) {
                                SortDialogFragment.getInstance(
                                        PreferencesUtil.getListSort(context),
                                        PreferencesUtil.getAscendingSort(context),
                                        PreferencesUtil.getGroupsBeforeSort(context),
                                        PreferencesUtil.getRecycleBinBottomSort(context))
                            } else {
                                SortDialogFragment.getInstance(
                                        PreferencesUtil.getListSort(context),
                                        PreferencesUtil.getAscendingSort(context),
                                        PreferencesUtil.getGroupsBeforeSort(context))
                            }

                    sortDialogFragment.show(childFragmentManager, "sortDialog")
                }
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            EntryEditActivity.ADD_OR_UPDATE_ENTRY_REQUEST_CODE -> {
                if (resultCode == EntryEditActivity.ADD_ENTRY_RESULT_CODE
                        || resultCode == EntryEditActivity.UPDATE_ENTRY_RESULT_CODE) {
                    data?.getParcelableExtra<NodeVersioned>(EntryEditActivity.ADD_OR_UPDATE_ENTRY_KEY)?.let { newNode ->
                        if (resultCode == EntryEditActivity.ADD_ENTRY_RESULT_CODE)
                            mAdapter?.addNode(newNode)
                        if (resultCode == EntryEditActivity.UPDATE_ENTRY_RESULT_CODE) {
                            //mAdapter.updateLastNodeRegister(newNode);
                            mainGroup?.let { mainGroup ->
                                mAdapter?.rebuildList(mainGroup)
                            }
                        }
                    } ?: Log.e(this.javaClass.name, "New node can be retrieve in Activity Result")
                }
            }
        }
    }

    fun contains(node: NodeVersioned): Boolean {
        return mAdapter?.contains(node) ?: false
    }

    fun addNode(newNode: NodeVersioned) {
        mAdapter?.addNode(newNode)
    }

    fun updateNode(oldNode: NodeVersioned, newNode: NodeVersioned? = null) {
        mAdapter?.updateNode(oldNode, newNode ?: oldNode)
    }

    fun removeNode(pwNode: NodeVersioned) {
        mAdapter?.removeNode(pwNode)
    }

    fun removeNodeAt(position: Int) {
        mAdapter?.removeNodeAt(position)
    }

    interface OnScrollListener {

        /**
         * Callback method to be invoked when the RecyclerView has been scrolled. This will be
         * called after the scroll has completed.
         *
         * @param dy The amount of vertical scroll.
         */
        fun onScrolled(dy: Int)
    }

    companion object {

        private val TAG = ListNodesFragment::class.java.name

        private const val GROUP_KEY = "GROUP_KEY"
        private const val IS_SEARCH = "IS_SEARCH"

        fun newInstance(group: GroupVersioned?, readOnly: Boolean, isASearch: Boolean): ListNodesFragment {
            val bundle = Bundle()
            if (group != null) {
                bundle.putParcelable(GROUP_KEY, group)
            }
            bundle.putBoolean(IS_SEARCH, isASearch)
            ReadOnlyHelper.putReadOnlyInBundle(bundle, readOnly)
            val listNodesFragment = ListNodesFragment()
            listNodesFragment.arguments = bundle
            return listNodesFragment
        }
    }
}
