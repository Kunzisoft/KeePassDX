/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.kunzisoft.keepass.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.SearchManager
import android.app.assist.AssistStructure
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.support.annotation.RequiresApi
import android.support.v4.app.FragmentManager
import android.support.v7.widget.SearchView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.GroupEditDialogFragment
import com.kunzisoft.keepass.activities.dialogs.IconPickerDialogFragment
import com.kunzisoft.keepass.activities.dialogs.ReadOnlyDialog
import com.kunzisoft.keepass.activities.dialogs.SortDialogFragment
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.activities.helpers.ReadOnlyHelper
import com.kunzisoft.keepass.activities.lock.LockingActivity
import com.kunzisoft.keepass.adapters.NodeAdapter
import com.kunzisoft.keepass.adapters.SearchEntryCursorAdapter
import com.kunzisoft.keepass.autofill.AutofillHelper
import com.kunzisoft.keepass.database.SortNodeEnum
import com.kunzisoft.keepass.database.action.ProgressDialogSaveDatabaseThread
import com.kunzisoft.keepass.database.action.node.*
import com.kunzisoft.keepass.database.element.*
import com.kunzisoft.keepass.education.GroupActivityEducation
import com.kunzisoft.keepass.icons.assignDatabaseIcon
import com.kunzisoft.keepass.magikeyboard.KeyboardHelper
import com.kunzisoft.keepass.magikeyboard.MagikIME
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.LOCK_ACTION
import com.kunzisoft.keepass.utils.MenuUtil
import com.kunzisoft.keepass.view.AddNodeButtonView
import net.cachapa.expandablelayout.ExpandableLayout

class GroupActivity : LockingActivity(),
        GroupEditDialogFragment.EditGroupListener,
        IconPickerDialogFragment.IconPickerListener,
        NodeAdapter.NodeMenuListener,
        ListNodesFragment.OnScrollListener,
        NodeAdapter.NodeClickCallback,
        SortDialogFragment.SortSelectionListener {

    // Views
    private var toolbar: Toolbar? = null
    private var searchTitleView: View? = null
    private var toolbarPasteExpandableLayout: ExpandableLayout? = null
    private var toolbarPaste: Toolbar? = null
    private var iconView: ImageView? = null
    private var modeTitleView: TextView? = null
    private var addNodeButtonView: AddNodeButtonView? = null
    private var groupNameView: TextView? = null

    private var mDatabase: Database? = null

    private var mListNodesFragment: ListNodesFragment? = null
    private var mCurrentGroupIsASearch: Boolean = false

    // Nodes
    private var mRootGroup: GroupVersioned? = null
    private var mCurrentGroup: GroupVersioned? = null
    private var mOldGroupToUpdate: GroupVersioned? = null
    private var mNodeToCopy: NodeVersioned? = null
    private var mNodeToMove: NodeVersioned? = null

    private var mSearchSuggestionAdapter: SearchEntryCursorAdapter? = null

    private var mIconColor: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isFinishing) {
            return
        }
        mDatabase = Database.getInstance()

        // Construct main view
        setContentView(layoutInflater.inflate(R.layout.activity_group, null))

        // Initialize views
        iconView = findViewById(R.id.icon)
        addNodeButtonView = findViewById(R.id.add_node_button)
        toolbar = findViewById(R.id.toolbar)
        searchTitleView = findViewById(R.id.search_title)
        groupNameView = findViewById(R.id.group_name)
        toolbarPasteExpandableLayout = findViewById(R.id.expandable_toolbar_paste_layout)
        toolbarPaste = findViewById(R.id.toolbar_paste)
        modeTitleView = findViewById(R.id.mode_title_view)

        // Focus view to reinitialize timeout
        resetAppTimeoutWhenViewFocusedOrChanged(addNodeButtonView)

        // Retrieve elements after an orientation change
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(OLD_GROUP_TO_UPDATE_KEY))
                mOldGroupToUpdate = savedInstanceState.getParcelable(OLD_GROUP_TO_UPDATE_KEY)
            if (savedInstanceState.containsKey(NODE_TO_COPY_KEY)) {
                mNodeToCopy = savedInstanceState.getParcelable(NODE_TO_COPY_KEY)
                toolbarPaste?.setOnMenuItemClickListener(OnCopyMenuItemClickListener())
            } else if (savedInstanceState.containsKey(NODE_TO_MOVE_KEY)) {
                mNodeToMove = savedInstanceState.getParcelable(NODE_TO_MOVE_KEY)
                toolbarPaste?.setOnMenuItemClickListener(OnMoveMenuItemClickListener())
            }
        }

        try {
            mRootGroup = mDatabase?.rootGroup
        } catch (e: NullPointerException) {
            Log.e(TAG, "Unable to get rootGroup")
        }

        mCurrentGroup = retrieveCurrentGroup(intent, savedInstanceState)
        mCurrentGroupIsASearch = Intent.ACTION_SEARCH == intent.action

        Log.i(TAG, "Started creating tree")
        if (mCurrentGroup == null) {
            Log.w(TAG, "Group was null")
            return
        }

        // Update last access time.
        mCurrentGroup?.touch(modified = false, touchParents = false)

        toolbar?.title = ""
        setSupportActionBar(toolbar)

        toolbarPaste?.inflateMenu(R.menu.node_paste_menu)
        toolbarPaste?.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp)
        toolbarPaste?.setNavigationOnClickListener {
            toolbarPasteExpandableLayout?.collapse()
            mNodeToCopy = null
            mNodeToMove = null
        }

        // Retrieve the textColor to tint the icon
        val taTextColor = theme.obtainStyledAttributes(intArrayOf(R.attr.textColorInverse))
        mIconColor = taTextColor.getColor(0, Color.WHITE)
        taTextColor.recycle()

        var fragmentTag = LIST_NODES_FRAGMENT_TAG
        if (mCurrentGroupIsASearch)
            fragmentTag = SEARCH_FRAGMENT_TAG

        // Initialize the fragment with the list
        mListNodesFragment = supportFragmentManager.findFragmentByTag(fragmentTag) as ListNodesFragment?
        if (mListNodesFragment == null)
            mListNodesFragment = ListNodesFragment.newInstance(mCurrentGroup, readOnly, mCurrentGroupIsASearch)

        // Attach fragment to content view
        supportFragmentManager.beginTransaction().replace(
                R.id.nodes_list_fragment_container,
                mListNodesFragment,
                fragmentTag)
                .commit()

        // Add listeners to the add buttons
        addNodeButtonView?.setAddGroupClickListener(View.OnClickListener {
            GroupEditDialogFragment.build()
                    .show(supportFragmentManager,
                            GroupEditDialogFragment.TAG_CREATE_GROUP)
        })
        addNodeButtonView?.setAddEntryClickListener(View.OnClickListener {
            mCurrentGroup?.let { currentGroup ->
                EntryEditActivity.launch(this@GroupActivity, currentGroup)
            }
        })

        // Search suggestion
        mDatabase?.let { database ->
            mSearchSuggestionAdapter = SearchEntryCursorAdapter(this, database)
        }

        Log.i(TAG, "Finished creating tree")
    }

    override fun onNewIntent(intent: Intent) {
        Log.d(TAG, "setNewIntent: $intent")
        setIntent(intent)
        mCurrentGroupIsASearch = if (Intent.ACTION_SEARCH == intent.action) {
            // only one instance of search in backstack
            openSearchGroup(retrieveCurrentGroup(intent, null))
            true
        } else {
            false
        }
    }

    private fun openSearchGroup(group: GroupVersioned?) {
        // Delete the previous search fragment
        val searchFragment = supportFragmentManager.findFragmentByTag(SEARCH_FRAGMENT_TAG)
        if (searchFragment != null) {
            if (supportFragmentManager
                            .popBackStackImmediate(SEARCH_FRAGMENT_TAG,
                                    FragmentManager.POP_BACK_STACK_INCLUSIVE))
                supportFragmentManager.beginTransaction().remove(searchFragment).commit()
        }
        openGroup(group, true)
    }

    private fun openChildGroup(group: GroupVersioned) {
        openGroup(group, false)
    }

    private fun openGroup(group: GroupVersioned?, isASearch: Boolean) {
        // Check TimeoutHelper
        TimeoutHelper.checkTimeAndLockIfTimeoutOrResetTimeout(this) {
            // Open a group in a new fragment
            val newListNodeFragment = ListNodesFragment.newInstance(group, readOnly, isASearch)
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            // Different animation
            val fragmentTag: String
            fragmentTag = if (isASearch) {
                fragmentTransaction.setCustomAnimations(R.anim.slide_in_top, R.anim.slide_out_bottom,
                        R.anim.slide_in_bottom, R.anim.slide_out_top)
                SEARCH_FRAGMENT_TAG
            } else {
                fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left, R.anim.slide_out_right)
                LIST_NODES_FRAGMENT_TAG
            }

            fragmentTransaction.replace(R.id.nodes_list_fragment_container,
                    newListNodeFragment,
                    fragmentTag)
            fragmentTransaction.addToBackStack(fragmentTag)
            fragmentTransaction.commit()

            mListNodesFragment = newListNodeFragment
            mCurrentGroup = group
            assignGroupViewElements()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mCurrentGroup?.let {
            outState.putParcelable(GROUP_ID_KEY, it.nodeId)
        }
        mOldGroupToUpdate?.let {
            outState.putParcelable(OLD_GROUP_TO_UPDATE_KEY, it)
        }
        mNodeToCopy?.let {
            outState.putParcelable(NODE_TO_COPY_KEY, it)
        }
        mNodeToMove?.let {
            outState.putParcelable(NODE_TO_MOVE_KEY, it)
        }
        super.onSaveInstanceState(outState)
    }

    private fun retrieveCurrentGroup(intent: Intent, savedInstanceState: Bundle?): GroupVersioned? {

        // If it's a search
        if (Intent.ACTION_SEARCH == intent.action) {
            return mDatabase?.search(intent.getStringExtra(SearchManager.QUERY).trim { it <= ' ' })
        }
        // else a real group
        else {
            var pwGroupId: PwNodeId<*>? = null
            if (savedInstanceState != null && savedInstanceState.containsKey(GROUP_ID_KEY)) {
                pwGroupId = savedInstanceState.getParcelable(GROUP_ID_KEY)
            } else {
                if (getIntent() != null)
                    pwGroupId = intent.getParcelableExtra(GROUP_ID_KEY)
            }

            readOnly = mDatabase?.isReadOnly == true || readOnly // Force read only if the database is like that

            Log.w(TAG, "Creating tree view")
            val currentGroup: GroupVersioned?
            currentGroup = if (pwGroupId == null) {
                mRootGroup
            } else {
                mDatabase?.getGroupById(pwGroupId)
            }

            return currentGroup
        }
    }

    private fun assignGroupViewElements() {
        // Assign title
        if (mCurrentGroup != null) {
            val title = mCurrentGroup?.title
            if (title != null && title.isNotEmpty()) {
                if (groupNameView != null) {
                    groupNameView?.text = title
                    groupNameView?.invalidate()
                }
            } else {
                if (groupNameView != null) {
                    groupNameView?.text = getText(R.string.root)
                    groupNameView?.invalidate()
                }
            }
        }
        if (mCurrentGroupIsASearch) {
            searchTitleView?.visibility = View.VISIBLE
        } else {
            searchTitleView?.visibility = View.GONE
        }

        // Assign icon
        if (mCurrentGroupIsASearch) {
            if (toolbar != null) {
                toolbar?.navigationIcon = null
            }
            iconView?.visibility = View.GONE
        } else {
            // Assign the group icon depending of IconPack or custom icon
            iconView?.visibility = View.VISIBLE
            mCurrentGroup?.let {
                if (mDatabase?.drawFactory != null)
                    iconView?.assignDatabaseIcon(mDatabase?.drawFactory!!, it.icon, mIconColor)

                if (toolbar != null) {
                    if (mCurrentGroup?.containsParent() == true)
                        toolbar?.setNavigationIcon(R.drawable.ic_arrow_up_white_24dp)
                    else {
                        toolbar?.navigationIcon = null
                    }
                }
            }
        }

        // Show selection mode message if needed
        if (selectionMode) {
            modeTitleView?.visibility = View.VISIBLE
        } else {
            modeTitleView?.visibility = View.GONE
        }

        // Show button if allowed
        addNodeButtonView?.apply {

            // To enable add button
            val addGroupEnabled = !readOnly && !mCurrentGroupIsASearch
            var addEntryEnabled = !readOnly && !mCurrentGroupIsASearch
            mCurrentGroup?.let {
                val isRoot = it == mRootGroup
                if (!it.allowAddEntryIfIsRoot())
                    addEntryEnabled = !isRoot && addEntryEnabled
                if (isRoot) {
                    showWarnings()
                }
            }
            enableAddGroup(addGroupEnabled)
            enableAddEntry(addEntryEnabled)

            if (isEnable)
                showButton()
        }
    }

    override fun onScrolled(dy: Int) {
        addNodeButtonView?.hideButtonOnScrollListener(dy)
    }

    override fun onNodeClick(node: NodeVersioned) {
        when (node.type) {
            Type.GROUP -> try {
                openChildGroup(node as GroupVersioned)
            } catch (e: ClassCastException) {
                Log.e(TAG, "Node can't be cast in Group")
            }

            Type.ENTRY -> try {
                val entryVersioned = node as EntryVersioned
                EntrySelectionHelper.doEntrySelectionAction(intent,
                        {
                            EntryActivity.launch(this@GroupActivity, entryVersioned, readOnly)
                        },
                        {
                            // Populate Magikeyboard with entry
                            mDatabase?.let { database ->
                                MagikIME.addEntryAndLaunchNotificationIfAllowed(this@GroupActivity,
                                        entryVersioned.getEntryInfo(database))
                            }
                            // Consume the selection mode
                            EntrySelectionHelper.removeEntrySelectionModeFromIntent(intent)
                            moveTaskToBack(true)
                        },
                        {
                            // Build response with the entry selected
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mDatabase != null) {
                                AutofillHelper.buildResponseWhenEntrySelected(this@GroupActivity,
                                        entryVersioned.getEntryInfo(mDatabase!!))
                            }
                            finish()
                        })
            } catch (e: ClassCastException) {
                Log.e(TAG, "Node can't be cast in Entry")
            }
        }
    }

    override fun onOpenMenuClick(node: NodeVersioned): Boolean {
        onNodeClick(node)
        return true
    }

    override fun onEditMenuClick(node: NodeVersioned): Boolean {
        when (node.type) {
            Type.GROUP -> {
                mOldGroupToUpdate = node as GroupVersioned
                GroupEditDialogFragment.build(mOldGroupToUpdate!!)
                        .show(supportFragmentManager,
                                GroupEditDialogFragment.TAG_CREATE_GROUP)
            }
            Type.ENTRY -> EntryEditActivity.launch(this@GroupActivity, node as EntryVersioned)
        }
        return true
    }

    override fun onCopyMenuClick(node: NodeVersioned): Boolean {
        toolbarPasteExpandableLayout?.expand()
        mNodeToCopy = node
        toolbarPaste?.setOnMenuItemClickListener(OnCopyMenuItemClickListener())
        return false
    }

    private inner class OnCopyMenuItemClickListener : Toolbar.OnMenuItemClickListener {
        override fun onMenuItemClick(item: MenuItem): Boolean {
            toolbarPasteExpandableLayout?.collapse()

            when (item.itemId) {
                R.id.menu_paste -> {
                    when (mNodeToCopy?.type) {
                        Type.GROUP -> Log.e(TAG, "Copy not allowed for group")
                        Type.ENTRY -> {
                            mCurrentGroup?.let { currentGroup ->
                                copyEntry(mNodeToCopy as EntryVersioned, currentGroup)
                            }
                        }
                    }
                    mNodeToCopy = null
                    return true
                }
            }
            return true
        }
    }

    private fun copyEntry(entryToCopy: EntryVersioned, newParent: GroupVersioned) {
        ProgressDialogSaveDatabaseThread(this) {
            CopyEntryRunnable(this,
                    Database.getInstance(),
                    entryToCopy,
                    newParent,
                    AfterAddNodeRunnable(),
                    !readOnly)
        }.start()
    }

    override fun onMoveMenuClick(node: NodeVersioned): Boolean {
        toolbarPasteExpandableLayout?.expand()
        mNodeToMove = node
        toolbarPaste?.setOnMenuItemClickListener(OnMoveMenuItemClickListener())
        return false
    }

    private inner class OnMoveMenuItemClickListener : Toolbar.OnMenuItemClickListener {
        override fun onMenuItemClick(item: MenuItem): Boolean {
            toolbarPasteExpandableLayout?.collapse()

            when (item.itemId) {
                R.id.menu_paste -> {
                    when (mNodeToMove?.type) {
                        Type.GROUP -> {
                            mCurrentGroup?.let { currentGroup ->
                                moveGroup(mNodeToMove as GroupVersioned, currentGroup)
                            }
                        }
                        Type.ENTRY -> {
                            mCurrentGroup?.let { currentGroup ->
                                moveEntry(mNodeToMove as EntryVersioned, currentGroup)
                            }
                        }
                    }
                    mNodeToMove = null
                    return true
                }
            }
            return true
        }
    }

    private fun moveGroup(groupToMove: GroupVersioned, newParent: GroupVersioned) {
        ProgressDialogSaveDatabaseThread(this) {
            MoveGroupRunnable(
                this,
                    Database.getInstance(),
                    groupToMove,
                    newParent,
                    AfterAddNodeRunnable(),
                    !readOnly)
        }.start()
    }

    private fun moveEntry(entryToMove: EntryVersioned, newParent: GroupVersioned) {
        ProgressDialogSaveDatabaseThread(this) {
            MoveEntryRunnable(
                    this,
                    Database.getInstance(),
                    entryToMove,
                    newParent,
                    AfterAddNodeRunnable(),
                    !readOnly)
        }.start()
    }

    override fun onDeleteMenuClick(node: NodeVersioned): Boolean {
        when (node.type) {
            Type.GROUP -> deleteGroup(node as GroupVersioned)
            Type.ENTRY -> deleteEntry(node as EntryVersioned)
        }
        return true
    }

    private fun deleteGroup(group: GroupVersioned) {
        //TODO Verify trash recycle bin
        ProgressDialogSaveDatabaseThread(this) {
            DeleteGroupRunnable(
                    this,
                    Database.getInstance(),
                    group,
                    AfterDeleteNodeRunnable(),
                    !readOnly)
        }.start()
    }

    private fun deleteEntry(entry: EntryVersioned) {
        ProgressDialogSaveDatabaseThread(this) {
            DeleteEntryRunnable(
                    this,
                    Database.getInstance(),
                    entry,
                    AfterDeleteNodeRunnable(),
                    !readOnly)
        }.start()
    }

    override fun onResume() {
        super.onResume()
        // Refresh the elements
        assignGroupViewElements()
        // Refresh suggestions to change preferences
        mSearchSuggestionAdapter?.reInit(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        val inflater = menuInflater
        inflater.inflate(R.menu.search, menu)
        inflater.inflate(R.menu.database_lock, menu)
        if (!selectionMode) {
            inflater.inflate(R.menu.default_menu, menu)
            MenuUtil.contributionMenuInflater(inflater, menu)
        }

        // Get the SearchView and set the searchable configuration
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager

        menu.findItem(R.id.menu_search)?.let {
            val searchView = it.actionView as SearchView?
            searchView?.apply {
                setSearchableInfo(searchManager.getSearchableInfo(
                        ComponentName(this@GroupActivity, GroupActivity::class.java)))
                setIconifiedByDefault(false) // Do not iconify the widget; expand it by default
                suggestionsAdapter = mSearchSuggestionAdapter
                setOnSuggestionListener(object : SearchView.OnSuggestionListener {
                    override fun onSuggestionClick(position: Int): Boolean {
                        mSearchSuggestionAdapter?.let { searchAdapter ->
                            searchAdapter.getEntryFromPosition(position)?.let { entry ->
                                onNodeClick(entry)
                            }
                        }
                        return true
                    }

                    override fun onSuggestionSelect(position: Int): Boolean {
                        return true
                    }
                })
            }
        }

        super.onCreateOptionsMenu(menu)

        // Launch education screen
        Handler().post { performedNextEducation(GroupActivityEducation(this), menu) }

        return true
    }

    private fun performedNextEducation(groupActivityEducation: GroupActivityEducation,
                                       menu: Menu) {
        // If no node, show education to add new one
        if (mListNodesFragment != null
                && mListNodesFragment!!.isEmpty
                && addNodeButtonView?.addButtonView != null
                && addNodeButtonView!!.isEnable
                && groupActivityEducation.checkAndPerformedAddNodeButtonEducation(
                        addNodeButtonView?.addButtonView!!,
                        {
                            addNodeButtonView?.openButtonIfClose()
                        },
                        {
                            performedNextEducation(groupActivityEducation, menu)
                        }
                ))
        else if (toolbar != null
                && toolbar!!.findViewById<View>(R.id.menu_search) != null
                && groupActivityEducation.checkAndPerformedSearchMenuEducation(
                        toolbar!!.findViewById(R.id.menu_search),
                        {
                            menu.findItem(R.id.menu_search).expandActionView()
                        },
                        {
                            performedNextEducation(groupActivityEducation, menu)
                        }))
        else if (toolbar != null
                && toolbar!!.findViewById<View>(R.id.menu_sort) != null
                && groupActivityEducation.checkAndPerformedSortMenuEducation(
                        toolbar!!.findViewById(R.id.menu_sort),
                        {
                            onOptionsItemSelected(menu.findItem(R.id.menu_sort))
                        },
                        {
                            performedNextEducation(groupActivityEducation, menu)
                        }))
        else if (toolbar != null
                && toolbar!!.findViewById<View>(R.id.menu_lock) != null
                && groupActivityEducation.checkAndPerformedLockMenuEducation(
                        toolbar!!.findViewById(R.id.menu_lock),
                        {
                            onOptionsItemSelected(menu.findItem(R.id.menu_lock))
                        },
                        {
                            performedNextEducation(groupActivityEducation, menu)
                        }))
        ;
    }

    override fun startActivity(intent: Intent) {

        // Get the intent, verify the action and get the query
        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            // manually launch the real search activity
            val searchIntent = Intent(applicationContext, GroupActivity::class.java)
            // add query to the Intent Extras
            searchIntent.action = Intent.ACTION_SEARCH
            searchIntent.putExtra(SearchManager.QUERY, query)

            EntrySelectionHelper.doEntrySelectionAction(intent,
                    {
                        super@GroupActivity.startActivity(intent)
                    },
                    {
                        KeyboardHelper.startActivityForKeyboardSelection(
                                this@GroupActivity,
                                searchIntent)
                        finish()
                    },
                    { assistStructure ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            AutofillHelper.startActivityForAutofillResult(
                                    this@GroupActivity,
                                    searchIntent,
                                    assistStructure)
                        }
                    })
        } else {
            super.startActivity(intent)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.menu_search ->
                //onSearchRequested();
                return true
            R.id.menu_lock -> {
                lockAndExit()
                return true
            }
            else -> {
                // Check the time lock before launching settings
                MenuUtil.onDefaultMenuOptionsItemSelected(this, item, readOnly, true)
                return super.onOptionsItemSelected(item)
            }
        }
    }

    override fun approveEditGroup(action: GroupEditDialogFragment.EditGroupDialogAction?,
                                  name: String?,
                                  icon: PwIcon?) {
        val database = Database.getInstance()

        if (name != null && name.isNotEmpty() && icon != null) {
            when (action) {
                GroupEditDialogFragment.EditGroupDialogAction.CREATION -> {
                    // If group creation
                    mCurrentGroup?.let { currentGroup ->
                        // Build the group
                        database.createGroup()?.let { newGroup ->
                            newGroup.title = name
                            newGroup.icon = icon
                            // Not really needed here because added in runnable but safe
                            newGroup.parent = currentGroup

                            // If group created save it in the database
                            ProgressDialogSaveDatabaseThread(this) {
                                AddGroupRunnable(this,
                                        Database.getInstance(),
                                        newGroup,
                                        currentGroup,
                                        AfterAddNodeRunnable(),
                                        !readOnly)
                            }.start()
                        }
                    }
                }
                GroupEditDialogFragment.EditGroupDialogAction.UPDATE ->
                    // If update add new elements
                    mOldGroupToUpdate?.let { oldGroupToUpdate ->
                        GroupVersioned(oldGroupToUpdate).let { updateGroup ->
                            updateGroup.title = name
                            // TODO custom icon
                            updateGroup.icon = icon

                            mListNodesFragment?.removeNode(oldGroupToUpdate)

                            // If group updated save it in the database
                            ProgressDialogSaveDatabaseThread(this) {
                                UpdateGroupRunnable(this,
                                        Database.getInstance(),
                                        oldGroupToUpdate,
                                        updateGroup,
                                        AfterUpdateNodeRunnable(),
                                        !readOnly)
                            }.start()
                        }
                    }
                else -> {
                }
            }
        }
    }

    internal inner class AfterAddNodeRunnable : AfterActionNodeFinishRunnable() {
        override fun onActionNodeFinish(actionNodeValues: ActionNodeValues) {
            runOnUiThread {
                if (actionNodeValues.result.isSuccess) {
                    if (actionNodeValues.newNode != null)
                        mListNodesFragment?.addNode(actionNodeValues.newNode)
                }
            }
        }
    }

    internal inner class AfterUpdateNodeRunnable : AfterActionNodeFinishRunnable() {
        override fun onActionNodeFinish(actionNodeValues: ActionNodeValues) {
            runOnUiThread {
                if (actionNodeValues.result.isSuccess) {
                    if (actionNodeValues.oldNode!= null && actionNodeValues.newNode != null)
                        mListNodesFragment?.updateNode(actionNodeValues.oldNode, actionNodeValues.newNode)
                }
            }
        }
    }

    internal inner class AfterDeleteNodeRunnable : AfterActionNodeFinishRunnable() {
        override fun onActionNodeFinish(actionNodeValues: ActionNodeValues) {
            runOnUiThread {
                if (actionNodeValues.result.isSuccess) {
                    actionNodeValues.oldNode?.let { oldNode ->

                        mListNodesFragment?.removeNode(oldNode)

                        // Add trash in views list if it doesn't exists
                        val database = Database.getInstance()
                        if (database.isRecycleBinEnabled) {
                            val recycleBin = database.recycleBin
                            if (mCurrentGroup != null && recycleBin != null
                                && mCurrentGroup!!.parent == null
                                && mCurrentGroup != recycleBin) {
                                if (mListNodesFragment?.contains(recycleBin) == true)
                                    mListNodesFragment?.updateNode(recycleBin)
                                else
                                    mListNodesFragment?.addNode(recycleBin)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun cancelEditGroup(action: GroupEditDialogFragment.EditGroupDialogAction?,
                                 name: String?,
                                 icon: PwIcon?) {
        // Do nothing here
    }

    override// For icon in create tree dialog
    fun iconPicked(bundle: Bundle) {
        (supportFragmentManager
                .findFragmentByTag(GroupEditDialogFragment.TAG_CREATE_GROUP) as GroupEditDialogFragment)
                .iconPicked(bundle)
    }

    private fun showWarnings() {
        // TODO Preferences
        if (Database.getInstance().isReadOnly) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            if (prefs.getBoolean(getString(R.string.show_read_only_warning), true)) {
                ReadOnlyDialog(this).show()
            }
        }
    }

    override fun onSortSelected(sortNodeEnum: SortNodeEnum, ascending: Boolean, groupsBefore: Boolean, recycleBinBottom: Boolean) {
        mListNodesFragment?.onSortSelected(sortNodeEnum, ascending, groupsBefore, recycleBinBottom)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AutofillHelper.onActivityResultSetResultAndFinish(this, requestCode, resultCode, data)
        }

        // Not directly get the entry from intent data but from database
        mListNodesFragment?.rebuildList()
    }

    @SuppressLint("RestrictedApi")
    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        /*
         * ACTION_SEARCH automatically forces a new task. This occurs when you open a kdb file in
         * another app such as Files or GoogleDrive and then Search for an entry. Here we remove the
         * FLAG_ACTIVITY_NEW_TASK flag bit allowing search to open it's activity in the current task.
         */
        if (Intent.ACTION_SEARCH == intent.action) {
            var flags = intent.flags
            flags = flags and Intent.FLAG_ACTIVITY_NEW_TASK.inv()
            intent.flags = flags
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            super.startActivityForResult(intent, requestCode, options)
        }
    }

    private fun removeSearchInIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            mCurrentGroupIsASearch = false
            intent.action = Intent.ACTION_DEFAULT
            intent.removeExtra(SearchManager.QUERY)
        }
    }

    override fun onBackPressed() {
        // Normal way when we are not in root
        if (mRootGroup != null && mRootGroup != mCurrentGroup)
            super.onBackPressed()
        // Else lock if needed
        else {
            if (PreferencesUtil.isLockDatabaseWhenBackButtonOnRootClicked(this)) {
                lockAndExit()
                super.onBackPressed()
            } else {
                moveTaskToBack(true)
            }
        }

        mListNodesFragment = supportFragmentManager.findFragmentByTag(LIST_NODES_FRAGMENT_TAG) as ListNodesFragment
        // to refresh fragment
        mListNodesFragment?.rebuildList()
        mCurrentGroup = mListNodesFragment?.mainGroup
        removeSearchInIntent(intent)
        assignGroupViewElements()
    }

    companion object {

        private val TAG = GroupActivity::class.java.name

        private const val GROUP_ID_KEY = "GROUP_ID_KEY"
        private const val LIST_NODES_FRAGMENT_TAG = "LIST_NODES_FRAGMENT_TAG"
        private const val SEARCH_FRAGMENT_TAG = "SEARCH_FRAGMENT_TAG"
        private const val OLD_GROUP_TO_UPDATE_KEY = "OLD_GROUP_TO_UPDATE_KEY"
        private const val NODE_TO_COPY_KEY = "NODE_TO_COPY_KEY"
        private const val NODE_TO_MOVE_KEY = "NODE_TO_MOVE_KEY"

        private fun buildAndLaunchIntent(activity: Activity, group: GroupVersioned?, readOnly: Boolean,
                                         intentBuildLauncher: (Intent) -> Unit) {
            if (TimeoutHelper.checkTimeAndLockIfTimeout(activity)) {
                val intent = Intent(activity, GroupActivity::class.java)
                if (group != null) {
                    intent.putExtra(GROUP_ID_KEY, group.nodeId)
                }
                ReadOnlyHelper.putReadOnlyInIntent(intent, readOnly)
                intentBuildLauncher.invoke(intent)
            }
        }

        /*
         * -------------------------
         * 		Standard Launch
         * -------------------------
         */

        @JvmOverloads
        fun launch(activity: Activity, readOnly: Boolean = PreferencesUtil.enableReadOnlyDatabase(activity)) {
            TimeoutHelper.recordTime(activity)
            buildAndLaunchIntent(activity, null, readOnly) { intent ->
                activity.startActivity(intent)
            }
        }

        /*
         * -------------------------
         * 		Keyboard Launch
         * -------------------------
         */
        // TODO implement pre search to directly open the direct group

        fun launchForKeyboardSelection(activity: Activity, readOnly: Boolean) {
            TimeoutHelper.recordTime(activity)
            buildAndLaunchIntent(activity, null, readOnly) { intent ->
                KeyboardHelper.startActivityForKeyboardSelection(activity, intent)
            }
        }

        /*
         * -------------------------
         * 		Autofill Launch
         * -------------------------
         */
        // TODO implement pre search to directly open the direct group

        @RequiresApi(api = Build.VERSION_CODES.O)
        fun launchForAutofillResult(activity: Activity, assistStructure: AssistStructure, readOnly: Boolean) {
            TimeoutHelper.recordTime(activity)
            buildAndLaunchIntent(activity, null, readOnly) { intent ->
                AutofillHelper.startActivityForAutofillResult(activity, intent, assistStructure)
            }
        }
    }
}
