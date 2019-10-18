/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
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
 *
 */
package com.kunzisoft.keepass.settings

import android.app.Activity
import android.app.backup.BackupManager
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.AssignMasterKeyDialogFragment
import com.kunzisoft.keepass.activities.dialogs.PasswordEncodingDialogFragment
import com.kunzisoft.keepass.activities.helpers.ReadOnlyHelper
import com.kunzisoft.keepass.activities.lock.LockingActivity
import com.kunzisoft.keepass.database.action.ProgressDialogThread
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.timeout.TimeoutHelper

open class SettingsActivity
    : LockingActivity(),
        MainPreferenceFragment.Callback,
        AssignMasterKeyDialogFragment.AssignPasswordDialogListener {

    private var backupManager: BackupManager? = null

    private var toolbar: Toolbar? = null

    var progressDialogThread: ProgressDialogThread? = null

    companion object {

        private const val TAG_NESTED = "TAG_NESTED"

        fun launch(activity: Activity, readOnly: Boolean, timeoutEnable: Boolean) {
            val intent = Intent(activity, SettingsActivity::class.java)
            ReadOnlyHelper.putReadOnlyInIntent(intent, readOnly)
            intent.putExtra(TIMEOUT_ENABLE_KEY, timeoutEnable)
            if (!timeoutEnable) {
                activity.startActivity(intent)
            } else if (TimeoutHelper.checkTimeAndLockIfTimeout(activity)) {
                activity.startActivity(intent)
            }
        }
    }

    /**
     * Retrieve the main fragment to show in first
     * @return The main fragment
     */
    protected open fun retrieveMainFragment(): Fragment {
        return MainPreferenceFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_toolbar)
        toolbar = findViewById(R.id.toolbar)
        toolbar?.setTitle(R.string.settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .add(R.id.fragment_container, retrieveMainFragment())
                    .commit()
        }

        backupManager = BackupManager(this)

        progressDialogThread = ProgressDialogThread(this) { actionTask, result ->
            // Call result in fragment
            (supportFragmentManager
                    .findFragmentByTag(TAG_NESTED) as NestedSettingsFragment?)
                    ?.onProgressDialogThreadResult(actionTask, result)
        }
    }

    override fun onResume() {
        super.onResume()

        progressDialogThread?.registerProgressTask()
    }

    override fun onPause() {

        progressDialogThread?.unregisterProgressTask()

        super.onPause()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onStop() {
        backupManager?.dataChanged()
        super.onStop()
    }

    override fun onAssignKeyDialogPositiveClick(masterPasswordChecked: Boolean,
                                                masterPassword: String?,
                                                keyFileChecked: Boolean,
                                                keyFile: Uri?) {
        Database.getInstance().let { database ->
            // Show the progress dialog now or after dialog confirmation
            if (database.validatePasswordEncoding(masterPassword, keyFileChecked)) {
                progressDialogThread?.startDatabaseAssignPassword(
                        masterPasswordChecked,
                        masterPassword,
                        keyFileChecked,
                        keyFile
                )
            } else {
                PasswordEncodingDialogFragment().apply {
                    positiveButtonClickListener = DialogInterface.OnClickListener { _, _ ->
                        progressDialogThread?.startDatabaseAssignPassword(
                                masterPasswordChecked,
                                masterPassword,
                                keyFileChecked,
                                keyFile
                        )
                    }
                    show(supportFragmentManager, "passwordEncodingTag")
                }
            }
        }
    }

    override fun onAssignKeyDialogNegativeClick(masterPasswordChecked: Boolean,
                                                masterPassword: String?,
                                                keyFileChecked: Boolean,
                                                keyFile: Uri?) {

    }

    override fun onBackPressed() {
        // this if statement is necessary to navigate through nested and main fragments
        if (supportFragmentManager.backStackEntryCount == 0) {
            super.onBackPressed()
        } else {
            supportFragmentManager.popBackStack()
        }
        toolbar?.setTitle(R.string.settings)
    }

    private fun replaceFragment(key: NestedSettingsFragment.Screen) {
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.fragment_container, NestedSettingsFragment.newInstance(key, mReadOnly), TAG_NESTED)
                .addToBackStack(TAG_NESTED)
                .commit()

        toolbar?.title = NestedSettingsFragment.retrieveTitle(resources, key)
    }

    override fun onNestedPreferenceSelected(key: NestedSettingsFragment.Screen) {
        if (mTimeoutEnable)
            TimeoutHelper.checkTimeAndLockIfTimeoutOrResetTimeout(this) {
                replaceFragment(key)
            }
        else
            replaceFragment(key)
    }
}
