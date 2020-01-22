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
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.FragmentManager
import com.google.android.material.snackbar.Snackbar
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.*
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.activities.helpers.ReadOnlyHelper
import com.kunzisoft.keepass.activities.lock.LockingActivity
import com.kunzisoft.keepass.adapters.SearchEntryCursorAdapter
import com.kunzisoft.keepass.autofill.AutofillHelper
import com.kunzisoft.keepass.database.element.SortNodeEnum
import com.kunzisoft.keepass.database.element.*
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.Type
import com.kunzisoft.keepass.education.GroupActivityEducation
import com.kunzisoft.keepass.icons.assignDatabaseIcon
import com.kunzisoft.keepass.magikeyboard.MagikIME
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_COPY_NODES_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_CREATE_GROUP_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_DELETE_NODES_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_MOVE_NODES_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_GROUP_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.NEW_NODES_KEY
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.OLD_NODES_KEY
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.getListNodesFromBundle
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.MenuUtil
import com.kunzisoft.keepass.view.AddNodeButtonView
import com.kunzisoft.keepass.view.ToolbarAction
import com.kunzisoft.keepass.view.asError

class GroupActivity : LockingActivity(),
        GroupEditDialogFragment.EditGroupListener,
        IconPickerDialogFragment.IconPickerListener,
        ListNodesFragment.NodeClickListener,
        ListNodesFragment.NodesActionMenuListener,
        DeleteNodesDialogFragment.DeleteNodeListener,
        ListNodesFragment.OnScrollListener,
        SortDialogFragment.SortSelectionListener {

    // Views
    private var coordinatorLayout: CoordinatorLayout? = null
    private var toolbar: Toolbar? = null
    private var searchTitleView: View? = null
    private var toolbarAction: ToolbarAction? = null
    private var iconView: ImageView? = null
    private var numberChildrenView: TextView? = null
    private var modeTitleView: TextView? = null
    private var addNodeButtonView: AddNodeButtonView? = null
    private var groupNameView: TextView? = null

    private var mDatabase: Database? = null

    private var mListNodesFragment: ListNodesFragment? = null
    private var mCurrentGroupIsASearch: Boolean = false

    // Nodes
    private var mRootGroup: Group? = null
    private var mCurrentGroup: Group? = null
    private var mOldGroupToUpdate: Group? = null

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
        coordinatorLayout = findViewById(R.id.group_coordinator)
        iconView = findViewById(R.id.group_icon)
        numberChildrenView = findViewById(R.id.group_numbers)
        addNodeButtonView = findViewById(R.id.add_node_button)
        toolbar = findViewById(R.id.toolbar)
        searchTitleView = findViewById(R.id.search_title)
        groupNameView = findViewById(R.id.group_name)
        toolbarAction = findViewById(R.id.toolbar_action)
        modeTitleView = findViewById(R.id.mode_title_view)

        toolbar?.title = ""
        setSupportActionBar(toolbar)

        // Focus view to reinitialize timeout
        resetAppTimeoutWhenViewFocusedOrChanged(addNodeButtonView)

        // Retrieve elements after an orientation change
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(OLD_GROUP_TO_UPDATE_KEY))
                mOldGroupToUpdate = savedInstanceState.getParcelable(OLD_GROUP_TO_UPDATE_KEY)
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
            mListNodesFragment = ListNodesFragment.newInstance(mCurrentGroup, mReadOnly, mCurrentGroupIsASearch)

        // Attach fragment to content view
        supportFragmentManager.beginTransaction().replace(
                R.id.nodes_list_fragment_container,
                mListNodesFragment!!,
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

        mDatabase?.let { database ->
            // Search suggestion
            mSearchSuggestionAdapter = SearchEntryCursorAdapter(this, database)

            // Init dialog thread
            mProgressDialogThread?.onActionFinish = { actionTask, result ->

                var oldNodes: List<Node> = ArrayList()
                result.data?.getBundle(OLD_NODES_KEY)?.let { oldNodesBundle ->
                    oldNodes = getListNodesFromBundle(database, oldNodesBundle)
                }
                var newNodes: List<Node> = ArrayList()
                result.data?.getBundle(NEW_NODES_KEY)?.let { newNodesBundle ->
                    newNodes = getListNodesFromBundle(database, newNodesBundle)
                }

                when (actionTask) {
                    ACTION_DATABASE_UPDATE_GROUP_TASK -> {
                        if (result.isSuccess) {
                            mListNodesFragment?.updateNodes(oldNodes, newNodes)
                        }
                    }
                    ACTION_DATABASE_CREATE_GROUP_TASK,
                    ACTION_DATABASE_COPY_NODES_TASK,
                    ACTION_DATABASE_MOVE_NODES_TASK -> {
                        if (result.isSuccess) {
                            mListNodesFragment?.addNodes(newNodes)
                        }
                    }
                    ACTION_DATABASE_DELETE_NODES_TASK -> {
                        if (result.isSuccess) {

                            // Rebuild all the list to avoid bug when delete node from sort
                            mListNodesFragment?.rebuildList()

                            // Add trash in views list if it doesn't exists
                            if (database.isRecycleBinEnabled) {
                                val recycleBin = database.recycleBin
                                val currentGroup = mCurrentGroup
                                if (currentGroup != null && recycleBin != null
                                        && currentGroup != recycleBin) {
                                    // Recycle bin already here, simply update it
                                    if (mListNodesFragment?.contains(recycleBin) == true) {
                                        mListNodesFragment?.updateNode(recycleBin)
                                    }
                                    // Recycle bin not here, verify if parents are similar to add it
                                    else if (currentGroup == recycleBin.parent) {
                                        mListNodesFragment?.addNode(recycleBin)
                                    }
                                }
                            }
                        }
                    }
                }

                if (!result.isSuccess) {
                    coordinatorLayout?.let { coordinatorLayout ->
                        result.exception?.errorId?.let { errorId ->
                            Snackbar.make(coordinatorLayout, errorId, Snackbar.LENGTH_LONG).asError().show()
                        } ?: result.message?.let { message ->
                            Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG).asError().show()
                        }
                    }
                }

                finishNodeAction()

                refreshNumberOfChildren()
            }
        }

        Log.i(TAG, "Finished creating tree")
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        intent?.let { intentNotNull ->
            Log.d(TAG, "setNewIntent: $intentNotNull")
            setIntent(intentNotNull)
            mCurrentGroupIsASearch = if (Intent.ACTION_SEARCH == intentNotNull.action) {
                // only one instance of search in backstack
                openSearchGroup(retrieveCurrentGroup(intentNotNull, null))
                true
            } else {
                false
            }
        }
    }

    private fun openSearchGroup(group: Group?) {
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

    private fun openChildGroup(group: Group) {
        openGroup(group, false)
    }

    private fun openGroup(group: Group?, isASearch: Boolean) {
        // Check TimeoutHelper
        TimeoutHelper.checkTimeAndLockIfTimeoutOrResetTimeout(this) {
            // Open a group in a new fragment
            val newListNodeFragment = ListNodesFragment.newInstance(group, mReadOnly, isASearch)
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
        super.onSaveInstanceState(outState)
    }

    private fun retrieveCurrentGroup(intent: Intent, savedInstanceState: Bundle?): Group? {

        // Force read only if the database is like that
        mReadOnly = mDatabase?.isReadOnly == true || mReadOnly

        // If it's a search
        if (Intent.ACTION_SEARCH == intent.action) {
            return mDatabase?.search(intent.getStringExtra(SearchManager.QUERY).trim { it <= ' ' })
        }
        // else a real group
        else {
            var pwGroupId: NodeId<*>? = null
            if (savedInstanceState != null && savedInstanceState.containsKey(GROUP_ID_KEY)) {
                pwGroupId = savedInstanceState.getParcelable(GROUP_ID_KEY)
            } else {
                if (getIntent() != null)
                    pwGroupId = intent.getParcelableExtra(GROUP_ID_KEY)
            }

            Log.w(TAG, "Creating tree view")
            val currentGroup: Group?
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

        // Assign number of children
        refreshNumberOfChildren()

        // Show selection mode message if needed
        if (mSelectionMode) {
            modeTitleView?.visibility = View.VISIBLE
        } else {
            modeTitleView?.visibility = View.GONE
        }

        // Show button if allowed
        addNodeButtonView?.apply {

            // To enable add button
            val addGroupEnabled = !mReadOnly && !mCurrentGroupIsASearch
            var addEntryEnabled = !mReadOnly && !mCurrentGroupIsASearch
            mCurrentGroup?.let {
                if (!it.allowAddEntryIfIsRoot())
                    addEntryEnabled = it != mRootGroup && addEntryEnabled
            }
            enableAddGroup(addGroupEnabled)
            enableAddEntry(addEntryEnabled)

            if (isEnable)
                showButton()
        }
    }

    private fun refreshNumberOfChildren() {
        numberChildrenView?.apply {
            if (PreferencesUtil.showNumberEntries(context)) {
                text = mCurrentGroup?.getChildEntries(true)?.size?.toString() ?: ""
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }
    }

    override fun onScrolled(dy: Int) {
        addNodeButtonView?.hideButtonOnScrollListener(dy)
    }

    override fun onNodeClick(node: Node) {
        when (node.type) {
            Type.GROUP -> try {
                openChildGroup(node as Group)
            } catch (e: ClassCastException) {
                Log.e(TAG, "Node can't be cast in Group")
            }

            Type.ENTRY -> try {
                val entryVersioned = node as Entry
                EntrySelectionHelper.doEntrySelectionAction(intent,
                        {
                            EntryActivity.launch(this@GroupActivity, entryVersioned, mReadOnly)
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
                                mDatabase?.let { database ->
                                    AutofillHelper.buildResponseWhenEntrySelected(this@GroupActivity,
                                            entryVersioned.getEntryInfo(database))
                                }
                            }
                            finish()
                        })
            } catch (e: ClassCastException) {
                Log.e(TAG, "Node can't be cast in Entry")
            }
        }
    }

    private var actionNodeMode: ActionMode? = null

    private fun finishNodeAction() {
        actionNodeMode?.finish()
        actionNodeMode = null
    }

    override fun onNodeSelected(nodes: List<Node>): Boolean {
        if (nodes.isNotEmpty()) {
            if (actionNodeMode == null || toolbarAction?.getSupportActionModeCallback() == null) {
                mListNodesFragment?.actionNodesCallback(nodes, this)?.let {
                    actionNodeMode = toolbarAction?.startSupportActionMode(it)
                }
            } else {
                actionNodeMode?.invalidate()
            }
        } else {
            finishNodeAction()
        }
        return true
    }

    override fun onOpenMenuClick(node: Node): Boolean {
        finishNodeAction()
        onNodeClick(node)
        return true
    }

    override fun onEditMenuClick(node: Node): Boolean {
        finishNodeAction()
        when (node.type) {
            Type.GROUP -> {
                mOldGroupToUpdate = node as Group
                GroupEditDialogFragment.build(mOldGroupToUpdate!!)
                        .show(supportFragmentManager,
                                GroupEditDialogFragment.TAG_CREATE_GROUP)
            }
            Type.ENTRY -> EntryEditActivity.launch(this@GroupActivity, node as Entry)
        }
        return true
    }

    override fun onCopyMenuClick(nodes: List<Node>): Boolean {
        actionNodeMode?.invalidate()

        // Nothing here fragment calls onPasteMenuClick internally
        return true
    }

    override fun onMoveMenuClick(nodes: List<Node>): Boolean {
        actionNodeMode?.invalidate()

        // Nothing here fragment calls onPasteMenuClick internally
        return true
    }

    override fun onPasteMenuClick(pasteMode: ListNodesFragment.PasteMode?,
                                  nodes: List<Node>): Boolean {
        // Move or copy only if allowed (in root if allowed)
        if (mCurrentGroup != mDatabase?.rootGroup
                || mDatabase?.rootCanContainsEntry() == true) {

            when (pasteMode) {
                ListNodesFragment.PasteMode.PASTE_FROM_COPY -> {
                    // Copy
                    mCurrentGroup?.let { newParent ->
                        mProgressDialogThread?.startDatabaseCopyNodes(
                                nodes,
                                newParent,
                                !mReadOnly && mAutoSaveEnable
                        )
                    }
                }
                ListNodesFragment.PasteMode.PASTE_FROM_MOVE -> {
                    // Move
                    mCurrentGroup?.let { newParent ->
                        mProgressDialogThread?.startDatabaseMoveNodes(
                                nodes,
                                newParent,
                                !mReadOnly && mAutoSaveEnable
                        )
                    }
                }
                else -> {
                }
            }
        } else {
            coordinatorLayout?.let { coordinatorLayout ->
                Snackbar.make(coordinatorLayout,
                        R.string.error_copy_entry_here,
                        Snackbar.LENGTH_LONG).asError().show()
            }
        }
        finishNodeAction()
        return true
    }

    override fun onDeleteMenuClick(nodes: List<Node>): Boolean {
        val database = mDatabase

        // If recycle bin enabled, ensure it exists
        if (database != null && database.isRecycleBinEnabled) {
            database.ensureRecycleBinExists(resources)
        }

        // If recycle bin enabled and not in recycle bin, move in recycle bin
        if (database != null
                && database.isRecycleBinEnabled
                && database.recycleBin != mCurrentGroup) {
            mProgressDialogThread?.startDatabaseDeleteNodes(
                    nodes,
                    !mReadOnly && mAutoSaveEnable
            )
        }
        // else open the dialog to confirm deletion
        else {
            DeleteNodesDialogFragment.getInstance(nodes)
                    .show(supportFragmentManager, "deleteNodesDialogFragment")
        }
        finishNodeAction()
        return true
    }

    override fun permanentlyDeleteNodes(nodes: List<Node>) {
        mProgressDialogThread?.startDatabaseDeleteNodes(
                nodes,
                !mReadOnly && mAutoSaveEnable
        )
    }

    override fun onResume() {
        super.onResume()
        // Refresh the elements
        assignGroupViewElements()
        // Refresh suggestions to change preferences
        mSearchSuggestionAdapter?.reInit(this)
    }

    override fun onPause() {
        super.onPause()

        finishNodeAction()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        val inflater = menuInflater
        inflater.inflate(R.menu.search, menu)
        inflater.inflate(R.menu.database, menu)
        if (mReadOnly) {
            menu.findItem(R.id.menu_save_database)?.isVisible = false
        }
        if (!mSelectionMode) {
            inflater.inflate(R.menu.default_menu, menu)
            MenuUtil.contributionMenuInflater(inflater, menu)
        }

        // Menu for recycle bin
        if (!mReadOnly
                && mDatabase?.isRecycleBinEnabled == true
                && mDatabase?.recycleBin == mCurrentGroup) {
            inflater.inflate(R.menu.recycle_bin, menu)
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
        val addNodeButtonEducationPerformed = mListNodesFragment != null
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
                )
        if (!addNodeButtonEducationPerformed) {

            val searchMenuEducationPerformed = toolbar != null
                    && toolbar!!.findViewById<View>(R.id.menu_search) != null
                    && groupActivityEducation.checkAndPerformedSearchMenuEducation(
                    toolbar!!.findViewById(R.id.menu_search),
                    {
                        menu.findItem(R.id.menu_search).expandActionView()
                    },
                    {
                        performedNextEducation(groupActivityEducation, menu)
                    })

            if (!searchMenuEducationPerformed) {

                val sortMenuEducationPerformed = toolbar != null
                        && toolbar!!.findViewById<View>(R.id.menu_sort) != null
                        && groupActivityEducation.checkAndPerformedSortMenuEducation(
                        toolbar!!.findViewById(R.id.menu_sort),
                        {
                            onOptionsItemSelected(menu.findItem(R.id.menu_sort))
                        },
                        {
                            performedNextEducation(groupActivityEducation, menu)
                        })

                if (!sortMenuEducationPerformed) {
                    // lockMenuEducationPerformed
                    toolbar != null
                            && toolbar!!.findViewById<View>(R.id.menu_lock) != null
                            && groupActivityEducation.checkAndPerformedLockMenuEducation(
                            toolbar!!.findViewById(R.id.menu_lock),
                            {
                                onOptionsItemSelected(menu.findItem(R.id.menu_lock))
                            },
                            {
                                performedNextEducation(groupActivityEducation, menu)
                            })
                }
            }
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
            R.id.menu_save_database -> {
                mProgressDialogThread?.startDatabaseSave(!mReadOnly)
                return true
            }
            R.id.menu_empty_recycle_bin -> {
                mCurrentGroup?.getChildren()?.let { listChildren ->
                    // Automatically delete all elements
                    onDeleteMenuClick(listChildren)
                }
                return true
            }
            else -> {
                // Check the time lock before launching settings
                MenuUtil.onDefaultMenuOptionsItemSelected(this, item, mReadOnly, true)
                return super.onOptionsItemSelected(item)
            }
        }
    }

    override fun approveEditGroup(action: GroupEditDialogFragment.EditGroupDialogAction?,
                                  name: String?,
                                  icon: IconImage?) {

        if (name != null && name.isNotEmpty() && icon != null) {
            when (action) {
                GroupEditDialogFragment.EditGroupDialogAction.CREATION -> {
                    // If group creation
                    mCurrentGroup?.let { currentGroup ->
                        // Build the group
                        mDatabase?.createGroup()?.let { newGroup ->
                            newGroup.title = name
                            newGroup.icon = icon
                            // Not really needed here because added in runnable but safe
                            newGroup.parent = currentGroup

                            mProgressDialogThread?.startDatabaseCreateGroup(
                                    newGroup,
                                    currentGroup,
                                    !mReadOnly && mAutoSaveEnable
                            )
                        }
                    }
                }
                GroupEditDialogFragment.EditGroupDialogAction.UPDATE -> {
                    // If update add new elements
                    mOldGroupToUpdate?.let { oldGroupToUpdate ->
                        val updateGroup = Group(oldGroupToUpdate).let { updateGroup ->
                            updateGroup.apply {
                                // WARNING remove parent and children to keep memory
                                removeParent()
                                removeChildren()

                                title = name
                                this.icon = icon // TODO custom icon
                            }
                        }
                        // If group updated save it in the database
                        mProgressDialogThread?.startDatabaseUpdateGroup(
                                oldGroupToUpdate,
                                updateGroup,
                                !mReadOnly && mAutoSaveEnable
                        )
                    }
                }
                else -> {}
            }
        }
    }

    override fun cancelEditGroup(action: GroupEditDialogFragment.EditGroupDialogAction?,
                                 name: String?,
                                 icon: IconImage?) {
        // Do nothing here
    }

    override// For icon in create tree dialog
    fun iconPicked(bundle: Bundle) {
        (supportFragmentManager
                .findFragmentByTag(GroupEditDialogFragment.TAG_CREATE_GROUP) as GroupEditDialogFragment)
                .iconPicked(bundle)
    }

    override fun onSortSelected(sortNodeEnum: SortNodeEnum, ascending: Boolean, groupsBefore: Boolean, recycleBinBottom: Boolean) {
        mListNodesFragment?.onSortSelected(sortNodeEnum, ascending, groupsBefore, recycleBinBottom)
    }

    override fun startActivity(intent: Intent) {

        // Get the intent, verify the action and get the query
        if (Intent.ACTION_SEARCH == intent.action) {
            // manually launch the real search activity
            val searchIntent = Intent(applicationContext, GroupActivity::class.java).apply {
                // Add bundle of current intent
                putExtras(this@GroupActivity.intent)
                // add query to the Intent Extras
                action = Intent.ACTION_SEARCH
                putExtra(SearchManager.QUERY, intent.getStringExtra(SearchManager.QUERY))
            }

            super.startActivity(searchIntent)
        } else {
            super.startActivity(intent)
        }
    }

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

        super.startActivityForResult(intent, requestCode, options)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AutofillHelper.onActivityResultSetResultAndFinish(this, requestCode, resultCode, data)
        }

        // Not directly get the entry from intent data but from database
        mListNodesFragment?.rebuildList()
    }

    private fun removeSearchInIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            mCurrentGroupIsASearch = false
            intent.action = Intent.ACTION_DEFAULT
            intent.removeExtra(SearchManager.QUERY)
        }
    }

    override fun onBackPressed() {
        if (mListNodesFragment?.nodeActionSelectionMode == true) {
            finishNodeAction()
        } else {
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
    }

    companion object {

        private val TAG = GroupActivity::class.java.name

        private const val GROUP_ID_KEY = "GROUP_ID_KEY"
        private const val LIST_NODES_FRAGMENT_TAG = "LIST_NODES_FRAGMENT_TAG"
        private const val SEARCH_FRAGMENT_TAG = "SEARCH_FRAGMENT_TAG"
        private const val OLD_GROUP_TO_UPDATE_KEY = "OLD_GROUP_TO_UPDATE_KEY"

        private fun buildAndLaunchIntent(context: Context, group: Group?, readOnly: Boolean,
                                         intentBuildLauncher: (Intent) -> Unit) {
            val checkTime = if (context is Activity)
                TimeoutHelper.checkTimeAndLockIfTimeout(context)
            else
                TimeoutHelper.checkTime(context)
            if (checkTime) {
                val intent = Intent(context, GroupActivity::class.java)
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
        fun launch(context: Context, readOnly: Boolean = PreferencesUtil.enableReadOnlyDatabase(context)) {
            TimeoutHelper.recordTime(context)
            buildAndLaunchIntent(context, null, readOnly) { intent ->
                context.startActivity(intent)
            }
        }

        /*
         * -------------------------
         * 		Keyboard Launch
         * -------------------------
         */
        // TODO implement pre search to directly open the direct group

        fun launchForKeyboardSelection(context: Context, readOnly: Boolean) {
            TimeoutHelper.recordTime(context)
            buildAndLaunchIntent(context, null, readOnly) { intent ->
                EntrySelectionHelper.startActivityForEntrySelection(context, intent)
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
