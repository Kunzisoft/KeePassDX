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
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.net.Uri
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
import com.kunzisoft.keepass.activities.lock.LockingActivity
import com.kunzisoft.keepass.adapters.NodeAdapter
import com.kunzisoft.keepass.adapters.SearchEntryCursorAdapter
import com.kunzisoft.keepass.app.App
import com.kunzisoft.keepass.autofill.AutofillHelper
import com.kunzisoft.keepass.database.SortNodeEnum
import com.kunzisoft.keepass.database.action.AssignPasswordInDatabaseRunnable
import com.kunzisoft.keepass.database.action.ProgressDialogRunnable
import com.kunzisoft.keepass.database.action.node.ActionNodeValues
import com.kunzisoft.keepass.database.action.node.AddGroupRunnable
import com.kunzisoft.keepass.database.action.node.AfterActionNodeFinishRunnable
import com.kunzisoft.keepass.database.action.node.CopyEntryRunnable
import com.kunzisoft.keepass.database.action.node.DeleteEntryRunnable
import com.kunzisoft.keepass.database.action.node.DeleteGroupRunnable
import com.kunzisoft.keepass.database.action.node.MoveEntryRunnable
import com.kunzisoft.keepass.database.action.node.MoveGroupRunnable
import com.kunzisoft.keepass.database.action.node.UpdateGroupRunnable
import com.kunzisoft.keepass.database.element.*
import com.kunzisoft.keepass.dialogs.AssignMasterKeyDialogFragment
import com.kunzisoft.keepass.dialogs.GroupEditDialogFragment
import com.kunzisoft.keepass.dialogs.IconPickerDialogFragment
import com.kunzisoft.keepass.dialogs.PasswordEncodingDialogHelper
import com.kunzisoft.keepass.dialogs.ReadOnlyDialog
import com.kunzisoft.keepass.dialogs.SortDialogFragment
import com.kunzisoft.keepass.education.GroupActivityEducation
import com.kunzisoft.keepass.magikeyboard.KeyboardEntryNotificationService
import com.kunzisoft.keepass.magikeyboard.KeyboardHelper
import com.kunzisoft.keepass.magikeyboard.MagikIME
import com.kunzisoft.keepass.model.Entry
import com.kunzisoft.keepass.model.Field
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.MenuUtil
import com.kunzisoft.keepass.view.AddNodeButtonView

import net.cachapa.expandablelayout.ExpandableLayout

class GroupActivity : LockingActivity(), GroupEditDialogFragment.EditGroupListener, IconPickerDialogFragment.IconPickerListener, NodeAdapter.NodeMenuListener, ListNodesFragment.OnScrollListener, AssignMasterKeyDialogFragment.AssignPasswordDialogListener, NodeAdapter.NodeClickCallback, SortDialogFragment.SortSelectionListener {

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

    private var listNodesFragment: ListNodesFragment? = null
    private var currentGroupIsASearch: Boolean = false

    // Nodes
    private var mRootGroup: GroupVersioned? = null
    private var mCurrentGroup: GroupVersioned? = null
    private var mOldGroupToUpdate: GroupVersioned? = null
    private var nodeToCopy: NodeVersioned? = null
    private var nodeToMove: NodeVersioned? = null

    private var searchSuggestionAdapter: SearchEntryCursorAdapter? = null

    private var iconColor: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isFinishing) {
            return
        }
        mDatabase = App.currentDatabase

        // Construct main view
        setContentView(layoutInflater.inflate(R.layout.list_nodes_with_add_button, null))

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
                nodeToCopy = savedInstanceState.getParcelable(NODE_TO_COPY_KEY)
                toolbarPaste?.setOnMenuItemClickListener(OnCopyMenuItemClickListener())
            } else if (savedInstanceState.containsKey(NODE_TO_MOVE_KEY)) {
                nodeToMove = savedInstanceState.getParcelable(NODE_TO_MOVE_KEY)
                toolbarPaste?.setOnMenuItemClickListener(OnMoveMenuItemClickListener())
            }
        }

        try {
            mRootGroup = mDatabase?.rootGroup
        } catch (e: NullPointerException) {
            Log.e(TAG, "Unable to get rootGroup")
        }

        mCurrentGroup = retrieveCurrentGroup(intent, savedInstanceState)
        currentGroupIsASearch = Intent.ACTION_SEARCH == intent.action

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
            nodeToCopy = null
            nodeToMove = null
        }

        // Retrieve the textColor to tint the icon
        iconColor = theme
                .obtainStyledAttributes(intArrayOf(R.attr.textColorInverse))
                .getColor(0, Color.WHITE)

        var fragmentTag = LIST_NODES_FRAGMENT_TAG
        if (currentGroupIsASearch)
            fragmentTag = SEARCH_FRAGMENT_TAG

        // Initialize the fragment with the list
        listNodesFragment = supportFragmentManager.findFragmentByTag(fragmentTag) as ListNodesFragment?
        if (listNodesFragment == null)
            listNodesFragment = ListNodesFragment.newInstance(mCurrentGroup, readOnly, currentGroupIsASearch)

        // Attach fragment to content view
        supportFragmentManager.beginTransaction().replace(
                R.id.nodes_list_fragment_container,
                listNodesFragment,
                fragmentTag)
                .commit()

        // Add listeners to the add buttons
        addNodeButtonView?.setAddGroupClickListener {
            GroupEditDialogFragment.build()
                    .show(supportFragmentManager,
                            GroupEditDialogFragment.TAG_CREATE_GROUP)
        }
        mCurrentGroup?.let { currentGroup ->
            addNodeButtonView?.setAddEntryClickListener {
                EntryEditActivity.launch(this@GroupActivity, currentGroup)
            }
        }

        // Search suggestion
        searchSuggestionAdapter = SearchEntryCursorAdapter(this, mDatabase)

        Log.i(TAG, "Finished creating tree")
    }

    override fun onNewIntent(intent: Intent) {
        Log.d(TAG, "setNewIntent: $intent")
        setIntent(intent)
        currentGroupIsASearch = if (Intent.ACTION_SEARCH == intent.action) {
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

            listNodesFragment = newListNodeFragment
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
        nodeToCopy?.let {
            outState.putParcelable(NODE_TO_COPY_KEY, it)
        }
        nodeToMove?.let {
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
        if (currentGroupIsASearch) {
            searchTitleView?.visibility = View.VISIBLE
        } else {
            searchTitleView?.visibility = View.GONE
        }

        // Assign icon
        if (currentGroupIsASearch) {
            if (toolbar != null) {
                toolbar?.navigationIcon = null
            }
            iconView?.visibility = View.GONE
        } else {
            // Assign the group icon depending of IconPack or custom icon
            iconView?.visibility = View.VISIBLE
            mCurrentGroup?.let {
                mDatabase?.drawFactory?.assignDatabaseIconTo(this, iconView, it.icon, iconColor)

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
            val addGroupEnabled = !readOnly && !currentGroupIsASearch
            var addEntryEnabled = !readOnly && !currentGroupIsASearch
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
                val entry = node as EntryVersioned
                EntrySelectionHelper.doEntrySelectionAction(intent,
                        {
                            EntryActivity.launch(this@GroupActivity, entry, readOnly)
                        },
                        {
                            MagikIME.setEntryKey(getEntry(entry))
                            // Show the notification if allowed in Preferences
                            if (PreferencesUtil.enableKeyboardNotificationEntry(this@GroupActivity)) {
                                startService(Intent(
                                        this@GroupActivity,
                                        KeyboardEntryNotificationService::class.java))
                            }
                            // Consume the selection mode
                            EntrySelectionHelper.removeEntrySelectionModeFromIntent(intent)
                            moveTaskToBack(true)
                        },
                        {
                            // Build response with the entry selected
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                AutofillHelper.buildResponseWhenEntrySelected(this@GroupActivity, entry)
                            }
                            finish()
                        })
            } catch (e: ClassCastException) {
                Log.e(TAG, "Node can't be cast in Entry")
            }
        }
    }

    // TODO Builder
    private fun getEntry(entry: EntryVersioned): Entry {
        val entryModel = Entry()
        entryModel.title = entry.title
        entryModel.username = entry.username
        entryModel.password = entry.password
        entryModel.url = entry.url
        if (entry.containsCustomFields()) {
            entry.fields
                    .doActionToAllCustomProtectedField { key, value ->
                        entryModel.addCustomField(
                                Field(key, value.toString()))
                    }
        }
        return entryModel
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
        nodeToCopy = node
        toolbarPaste?.setOnMenuItemClickListener(OnCopyMenuItemClickListener())
        return false
    }

    private inner class OnCopyMenuItemClickListener : Toolbar.OnMenuItemClickListener {
        override fun onMenuItemClick(item: MenuItem): Boolean {
            toolbarPasteExpandableLayout?.collapse()

            when (item.itemId) {
                R.id.menu_paste -> {
                    when (nodeToCopy?.type) {
                        Type.GROUP -> Log.e(TAG, "Copy not allowed for group")
                        Type.ENTRY -> {
                            mCurrentGroup?.let { currentGroup ->
                                copyEntry(nodeToCopy as EntryVersioned, currentGroup)
                            }
                        }
                    }
                    nodeToCopy = null
                    return true
                }
            }
            return true
        }
    }

    private fun copyEntry(entryToCopy: EntryVersioned, newParent: GroupVersioned) {
        Thread(CopyEntryRunnable(this,
                App.currentDatabase,
                entryToCopy,
                newParent,
                AfterAddNodeRunnable(),
                !readOnly)
        ).start()
    }

    override fun onMoveMenuClick(node: NodeVersioned): Boolean {
        toolbarPasteExpandableLayout?.expand()
        nodeToMove = node
        toolbarPaste?.setOnMenuItemClickListener(OnMoveMenuItemClickListener())
        return false
    }

    private inner class OnMoveMenuItemClickListener : Toolbar.OnMenuItemClickListener {
        override fun onMenuItemClick(item: MenuItem): Boolean {
            toolbarPasteExpandableLayout?.collapse()

            when (item.itemId) {
                R.id.menu_paste -> {
                    when (nodeToMove?.type) {
                        Type.GROUP -> {
                            mCurrentGroup?.let { currentGroup ->
                                moveGroup(nodeToMove as GroupVersioned, currentGroup)
                            }
                        }
                        Type.ENTRY -> {
                            mCurrentGroup?.let { currentGroup ->
                                moveEntry(nodeToMove as EntryVersioned, currentGroup)
                            }
                        }
                    }
                    nodeToMove = null
                    return true
                }
            }
            return true
        }
    }

    private fun moveGroup(groupToMove: GroupVersioned, newParent: GroupVersioned) {
        Thread(MoveGroupRunnable(
                this,
                App.currentDatabase,
                groupToMove,
                newParent,
                AfterAddNodeRunnable(),
                !readOnly)
        ).start()
    }

    private fun moveEntry(entryToMove: EntryVersioned, newParent: GroupVersioned) {
        Thread(MoveEntryRunnable(
                this,
                App.currentDatabase,
                entryToMove,
                newParent,
                AfterAddNodeRunnable(),
                !readOnly)
        ).start()
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
        Thread(DeleteGroupRunnable(
                this,
                App.currentDatabase,
                group,
                AfterDeleteNodeRunnable(),
                !readOnly)
        ).start()
    }

    private fun deleteEntry(entry: EntryVersioned) {
        Thread(DeleteEntryRunnable(
                this,
                App.currentDatabase,
                entry,
                AfterDeleteNodeRunnable(),
                !readOnly)
        ).start()
    }

    override fun onResume() {
        super.onResume()
        // Refresh the elements
        assignGroupViewElements()
        // Refresh suggestions to change preferences
        searchSuggestionAdapter?.reInit(this)
    }

    override fun onStop() {
        super.onStop()
        // Hide button
        addNodeButtonView?.hideButton()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        val inflater = menuInflater
        inflater.inflate(R.menu.search, menu)
        inflater.inflate(R.menu.database_lock, menu)
        if (!readOnly)
            inflater.inflate(R.menu.database_master_key, menu)
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
                suggestionsAdapter = searchSuggestionAdapter
                setOnSuggestionListener(object : SearchView.OnSuggestionListener {
                    override fun onSuggestionClick(position: Int): Boolean {
                        searchSuggestionAdapter?.let { searchAdapter ->
                            onNodeClick(searchAdapter.getEntryFromPosition(position))
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
        if (listNodesFragment != null
                && listNodesFragment!!.isEmpty
                && addNodeButtonView != null
                && addNodeButtonView!!.isEnable
                && groupActivityEducation.checkAndPerformedAddNodeButtonEducation(
                        addNodeButtonView!!,
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
            R.id.menu_change_master_key -> {
                setPassword()
                return true
            }
            else -> {
                // Check the time lock before launching settings
                MenuUtil.onDefaultMenuOptionsItemSelected(this, item, readOnly, true)
                return super.onOptionsItemSelected(item)
            }
        }
    }

    private fun setPassword() {
        AssignMasterKeyDialogFragment().show(supportFragmentManager, "passwordDialog")
    }

    override fun approveEditGroup(action: GroupEditDialogFragment.EditGroupDialogAction,
                                  name: String,
                                  icon: PwIcon) {
        val database = App.currentDatabase

        when (action) {
            GroupEditDialogFragment.EditGroupDialogAction.CREATION -> {
                // If group creation
                mCurrentGroup?.let { currentGroup ->
                    // Build the group
                    database.createGroup()?.let { newGroup->
                        newGroup.title = name
                        newGroup.icon = icon
                        // Not really needed here because added in runnable but safe
                        newGroup.parent = currentGroup

                        // If group created save it in the database
                        Thread(AddGroupRunnable(this,
                                App.currentDatabase,
                                newGroup,
                                currentGroup,
                                AfterAddNodeRunnable(),
                                !readOnly)
                        ).start()
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

                        listNodesFragment?.removeNode(oldGroupToUpdate)

                        // If group updated save it in the database
                        Thread(UpdateGroupRunnable(this,
                                App.currentDatabase,
                                oldGroupToUpdate,
                                updateGroup,
                                AfterUpdateNodeRunnable(),
                                !readOnly)
                        ).start()
                    }
                }
            else -> {}
        }
    }

    internal inner class AfterAddNodeRunnable : AfterActionNodeFinishRunnable() {
        override fun onActionNodeFinish(actionNodeValues: ActionNodeValues) {
            runOnUiThread {
                if (actionNodeValues.success) {
                    listNodesFragment?.addNode(actionNodeValues.newNode)
                }
            }
        }
    }

    internal inner class AfterUpdateNodeRunnable : AfterActionNodeFinishRunnable() {
        override fun onActionNodeFinish(actionNodeValues: ActionNodeValues) {
            runOnUiThread {
                if (actionNodeValues.success) {
                    listNodesFragment?.updateNode(actionNodeValues.oldNode, actionNodeValues.newNode)
                }
            }
        }
    }

    internal inner class AfterDeleteNodeRunnable : AfterActionNodeFinishRunnable() {
        override fun onActionNodeFinish(actionNodeValues: ActionNodeValues) {
            runOnUiThread {
                if (actionNodeValues.success) {
                    listNodesFragment?.removeNode(actionNodeValues.oldNode)

                    actionNodeValues.oldNode?.let { oldNode ->
                        val parent = oldNode.parent
                        val database = App.currentDatabase
                        if (database.isRecycleBinAvailable && database.isRecycleBinEnabled) {
                            val recycleBin = database.recycleBin
                            // Add trash if it doesn't exists
                            if (parent == recycleBin
                                    && mCurrentGroup != null
                                    && mCurrentGroup!!.parent == null
                                    && mCurrentGroup != recycleBin) {
                                listNodesFragment?.addNode(parent)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun cancelEditGroup(action: GroupEditDialogFragment.EditGroupDialogAction,
                                 name: String,
                                 iconId: PwIcon) {
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
        if (App.currentDatabase.isReadOnly) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            if (prefs.getBoolean(getString(R.string.show_read_only_warning), true)) {
                ReadOnlyDialog(this).show()
            }
        }
    }

    override fun onAssignKeyDialogPositiveClick(
            masterPasswordChecked: Boolean, masterPassword: String?,
            keyFileChecked: Boolean, keyFile: Uri?) {

        mDatabase?.let { database ->
            val taskThread = Thread(ProgressDialogRunnable(this,
                    R.string.saving_database
            ) {
                AssignPasswordInDatabaseRunnable(this@GroupActivity,
                        database,
                        masterPasswordChecked,
                        masterPassword,
                        keyFileChecked,
                        keyFile,
                        true) // TODO save
            })
            // Show the progress dialog now or after dialog confirmation
            if (database.validatePasswordEncoding(masterPassword!!)) {
                taskThread.start()
            } else {
                PasswordEncodingDialogHelper()
                        .show(this, DialogInterface.OnClickListener{ _, _ -> taskThread.start() })
            }
        }
    }

    override fun onAssignKeyDialogNegativeClick(
            masterPasswordChecked: Boolean, masterPassword: String?,
            keyFileChecked: Boolean, keyFile: Uri?) {

    }

    override fun onSortSelected(sortNodeEnum: SortNodeEnum, ascending: Boolean, groupsBefore: Boolean, recycleBinBottom: Boolean) {
        listNodesFragment?.onSortSelected(sortNodeEnum, ascending, groupsBefore, recycleBinBottom)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AutofillHelper.onActivityResultSetResultAndFinish(this, requestCode, resultCode, data)
        }

        // Not directly get the entry from intent data but from database
        // Is refresh from onResume()
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
            currentGroupIsASearch = false
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
                App.currentDatabase.closeAndClear(applicationContext)
                super.onBackPressed()
            } else {
                moveTaskToBack(true)
            }
        }

        listNodesFragment = supportFragmentManager.findFragmentByTag(LIST_NODES_FRAGMENT_TAG) as ListNodesFragment
        // to refresh fragment
        listNodesFragment?.rebuildList()
        mCurrentGroup = listNodesFragment?.mainGroup
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
            launch(activity, null, readOnly)
        }

        fun launch(activity: Activity, group: GroupVersioned?, readOnly: Boolean) {
            TimeoutHelper.recordTime(activity)
            buildAndLaunchIntent(activity, group, readOnly) { intent ->
                activity.startActivityForResult(intent, 0)
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
