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
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.CollapsingToolbarLayout
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.education.EntryActivityEducation
import com.kunzisoft.keepass.activities.lock.LockingHideActivity
import com.kunzisoft.keepass.activities.helpers.ReadOnlyHelper
import com.kunzisoft.keepass.view.EntryContentsView
import com.kunzisoft.keepass.app.App
import com.kunzisoft.keepass.database.element.EntryVersioned
import com.kunzisoft.keepass.database.element.PwNodeId
import com.kunzisoft.keepass.icons.assignDatabaseIcon
import com.kunzisoft.keepass.notifications.NotificationEntryCopyManager
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.settings.PreferencesUtil.isFirstTimeAskAllowCopyPasswordAndProtectedFields
import com.kunzisoft.keepass.settings.SettingsAutofillActivity
import com.kunzisoft.keepass.timeout.ClipboardHelper
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.MenuUtil
import com.kunzisoft.keepass.utils.Util

class EntryActivity : LockingHideActivity() {

    private var collapsingToolbarLayout: CollapsingToolbarLayout? = null
    private var titleIconView: ImageView? = null
    private var entryContentsView: EntryContentsView? = null
    private var toolbar: Toolbar? = null

    private var mEntry: EntryVersioned? = null
    private var mShowPassword: Boolean = false

    private var clipboardHelper: ClipboardHelper? = null
    private var firstLaunchOfActivity: Boolean = false

    private var iconColor: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_entry)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val currentDatabase = App.currentDatabase
        readOnly = currentDatabase.isReadOnly || readOnly

        mShowPassword = !PreferencesUtil.isPasswordMask(this)

        // Get Entry from UUID
        try {
            val keyEntry: PwNodeId<*> = intent.getParcelableExtra(KEY_ENTRY)
            mEntry = currentDatabase.getEntryById(keyEntry)
        } catch (e: ClassCastException) {
            Log.e(TAG, "Unable to retrieve the entry key")
        }

        if (mEntry == null) {
            Toast.makeText(this, R.string.entry_not_found, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Update last access time.
        mEntry?.touch(modified = false, touchParents = false)

        // Retrieve the textColor to tint the icon
        val taIconColor = theme.obtainStyledAttributes(intArrayOf(R.attr.textColorInverse))
        iconColor = taIconColor.getColor(0, Color.WHITE)
        taIconColor.recycle()

        // Refresh Menu contents in case onCreateMenuOptions was called before mEntry was set
        invalidateOptionsMenu()

        // Get views
        collapsingToolbarLayout = findViewById(R.id.toolbar_layout)
        titleIconView = findViewById(R.id.entry_icon)
        entryContentsView = findViewById(R.id.entry_contents)
        entryContentsView?.applyFontVisibilityToFields(PreferencesUtil.fieldFontIsInVisibility(this))

        // Init the clipboard helper
        clipboardHelper = ClipboardHelper(this)
        firstLaunchOfActivity = true
    }

    override fun onResume() {
        super.onResume()

        mEntry?.let { entry ->
            // Fill data in resume to update from EntryEditActivity
            fillEntryDataInContentsView(entry)
            // Refresh Menu
            invalidateOptionsMenu()
            // Manage entry copy to start notification if allowed
            NotificationEntryCopyManager.launchNotificationIfAllowed(this,
                    firstLaunchOfActivity,
                    entry)
        }

        firstLaunchOfActivity = false
    }

    private fun fillEntryDataInContentsView(entry: EntryVersioned) {

        val database = App.currentDatabase
        database.startManageEntry(entry)
        // Assign title icon
        titleIconView?.assignDatabaseIcon(database.drawFactory, entry.icon, iconColor)

        // Assign title text
        val entryTitle = entry.getVisualTitle()
        collapsingToolbarLayout?.title = entryTitle
        toolbar?.title = entryTitle

        // Assign basic fields
        entryContentsView?.assignUserName(entry.username)
        entryContentsView?.assignUserNameCopyListener(View.OnClickListener {
            clipboardHelper?.timeoutCopyToClipboard(entry.username,
                            getString(R.string.copy_field,
                            getString(R.string.entry_user_name)))
        })

        val allowCopyPassword = PreferencesUtil.allowCopyPasswordAndProtectedFields(this)
        entryContentsView?.assignPassword(entry.password, allowCopyPassword)
        if (allowCopyPassword) {
            entryContentsView?.assignPasswordCopyListener(View.OnClickListener {
                clipboardHelper?.timeoutCopyToClipboard(entry.password,
                                getString(R.string.copy_field,
                                getString(R.string.entry_password)))
            })
        } else {
            // If dialog not already shown
            if (isFirstTimeAskAllowCopyPasswordAndProtectedFields(this)) {
                entryContentsView?.assignPasswordCopyListener(View.OnClickListener {
                    val message = getString(R.string.allow_copy_password_warning) +
                            "\n\n" +
                            getString(R.string.clipboard_warning)
                    val warningDialog = AlertDialog.Builder(this@EntryActivity)
                            .setMessage(message).create()
                    warningDialog.setButton(AlertDialog.BUTTON_POSITIVE, getText(android.R.string.ok)
                    ) { dialog, _ ->
                        PreferencesUtil.setAllowCopyPasswordAndProtectedFields(this@EntryActivity, true)
                        dialog.dismiss()
                        fillEntryDataInContentsView(entry)
                    }
                    warningDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getText(android.R.string.cancel)
                    ) { dialog, _ ->
                        PreferencesUtil.setAllowCopyPasswordAndProtectedFields(this@EntryActivity, false)
                        dialog.dismiss()
                        fillEntryDataInContentsView(entry)
                    }
                    warningDialog.show()
                })
            } else {
                entryContentsView?.assignPasswordCopyListener(null)
            }
        }

        entryContentsView?.assignURL(entry.url)
        entryContentsView?.setHiddenPasswordStyle(!mShowPassword)
        entryContentsView?.assignComment(entry.notes)

        // Assign custom fields
        if (entry.allowExtraFields()) {
            entryContentsView?.clearExtraFields()

            entry.fields.doActionToAllCustomProtectedField { label, value ->
                val showAction = !value.isProtected || PreferencesUtil.allowCopyPasswordAndProtectedFields(this@EntryActivity)
                entryContentsView?.addExtraField(label, value, showAction, View.OnClickListener {
                    clipboardHelper?.timeoutCopyToClipboard(
                            value.toString(),
                            getString(R.string.copy_field, label)
                    )
                })
            }
        }

        // Assign dates
        entry.creationTime.date?.let {
            entryContentsView?.assignCreationDate(it)
        }
        entry.lastModificationTime.date?.let {
            entryContentsView?.assignModificationDate(it)
        }
        entry.lastAccessTime.date?.let {
            entryContentsView?.assignLastAccessDate(it)
        }
        val expires = entry.expiryTime.date
        if (entry.isExpires && expires != null) {
            entryContentsView?.assignExpiresDate(expires)
        } else {
            entryContentsView?.assignExpiresDate(getString(R.string.never))
        }

        database.stopManageEntry(entry)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            EntryEditActivity.ADD_OR_UPDATE_ENTRY_REQUEST_CODE ->
                // Not directly get the entry from intent data but from database
                mEntry?.let {
                    fillEntryDataInContentsView(it)
                }
        }
    }

    private fun changeShowPasswordIcon(togglePassword: MenuItem?) {
        if (mShowPassword) {
            togglePassword?.setTitle(R.string.menu_hide_password)
            togglePassword?.setIcon(R.drawable.ic_visibility_off_white_24dp)
        } else {
            togglePassword?.setTitle(R.string.menu_showpass)
            togglePassword?.setIcon(R.drawable.ic_visibility_white_24dp)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        val inflater = menuInflater
        MenuUtil.contributionMenuInflater(inflater, menu)
        inflater.inflate(R.menu.entry, menu)
        inflater.inflate(R.menu.database_lock, menu)

        if (readOnly) {
            menu.findItem(R.id.menu_edit)?.isVisible = false
        }

        val togglePassword = menu.findItem(R.id.menu_toggle_pass)
        entryContentsView?.let {
            if (it.isPasswordPresent || it.atLeastOneFieldProtectedPresent()) {
                changeShowPasswordIcon(togglePassword)
            } else {
                togglePassword?.isVisible = false
            }
        }

        val gotoUrl = menu.findItem(R.id.menu_goto_url)
        gotoUrl?.apply {
            // In API >= 11 onCreateOptionsMenu may be called before onCreate completes
            // so mEntry may not be set
            if (mEntry == null) {
                isVisible = false
            } else {
                if (mEntry?.url?.isEmpty() != false) {
                    // disable button if url is not available
                    isVisible = false
                }
            }
        }

        // Show education views
        Handler().post { performedNextEducation(EntryActivityEducation(this), menu) }

        return true
    }

    private fun performedNextEducation(entryActivityEducation: EntryActivityEducation,
                                       menu: Menu) {
        if (entryContentsView?.isUserNamePresent == true
                && entryActivityEducation.checkAndPerformedEntryCopyEducation(
                        findViewById(R.id.entry_user_name_action_image),
                        {
                            clipboardHelper?.timeoutCopyToClipboard(mEntry!!.username,
                                    getString(R.string.copy_field,
                                            getString(R.string.entry_user_name)))
                        },
                        {
                            // Launch autofill settings
                            startActivity(Intent(this@EntryActivity, SettingsAutofillActivity::class.java))
                        }))
        else if (toolbar?.findViewById<View>(R.id.menu_edit) != null && entryActivityEducation.checkAndPerformedEntryEditEducation(
                        toolbar!!.findViewById(R.id.menu_edit),
                        {
                            onOptionsItemSelected(menu.findItem(R.id.menu_edit))
                        },
                        {
                            // Open Keepass doc to create field references
                            startActivity(Intent(Intent.ACTION_VIEW,
                                    Uri.parse(getString(R.string.field_references_url))))
                        }))
        ;
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_contribute -> return MenuUtil.onContributionItemSelected(this)

            R.id.menu_toggle_pass -> {
                mShowPassword = !mShowPassword
                changeShowPasswordIcon(item)
                entryContentsView?.setHiddenPasswordStyle(!mShowPassword)
                return true
            }

            R.id.menu_edit -> {
                mEntry?.let {
                    EntryEditActivity.launch(this@EntryActivity, it)
                }
                return true
            }

            R.id.menu_goto_url -> {
                var url: String = mEntry?.url ?: ""

                // Default http:// if no protocol specified
                if (!url.contains("://")) {
                    url = "http://$url"
                }

                try {
                    Util.gotoUrl(this, url)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this, R.string.no_url_handler, Toast.LENGTH_LONG).show()
                }

                return true
            }

            R.id.menu_lock -> {
                lockAndExit()
                return true
            }

            android.R.id.home -> finish() // close this activity and return to preview activity (if there is any)
        }

        return super.onOptionsItemSelected(item)
    }


    override fun finish() {
        // Transit data in previous Activity after an update
        /*
		TODO Slowdown when add entry as result
        Intent intent = new Intent();
        intent.putExtra(EntryEditActivity.ADD_OR_UPDATE_ENTRY_KEY, mEntry);
        setResult(EntryEditActivity.UPDATE_ENTRY_RESULT_CODE, intent);
        */
        super.finish()
    }

    companion object {
        private val TAG = EntryActivity::class.java.name

        const val KEY_ENTRY = "entry"

        fun launch(activity: Activity, pw: EntryVersioned, readOnly: Boolean) {
            if (TimeoutHelper.checkTimeAndLockIfTimeout(activity)) {
                val intent = Intent(activity, EntryActivity::class.java)
                intent.putExtra(KEY_ENTRY, pw.nodeId)
                ReadOnlyHelper.putReadOnlyInIntent(intent, readOnly)
                activity.startActivityForResult(intent, EntryEditActivity.ADD_OR_UPDATE_ENTRY_REQUEST_CODE)
            }
        }
    }
}
