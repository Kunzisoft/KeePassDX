/*
 * Copyright 2020 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.settings

import android.app.Activity
import android.app.backup.BackupManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.AssignMasterKeyDialogFragment
import com.kunzisoft.keepass.activities.helpers.ExternalFileHelper
import com.kunzisoft.keepass.activities.legacy.DatabaseLockActivity
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.model.MainCredential
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.view.showActionErrorIfNeeded
import org.joda.time.DateTime
import java.util.*

open class SettingsActivity
    : DatabaseLockActivity(),
        MainPreferenceFragment.Callback,
        AssignMasterKeyDialogFragment.AssignPasswordDialogListener {

    private var backupManager: BackupManager? = null
    private var mExternalFileHelper: ExternalFileHelper? = null
    private var appPropertiesFileCreationRequestCode: Int? = null

    private var coordinatorLayout: CoordinatorLayout? = null
    private var toolbar: Toolbar? = null
    private var lockView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_toolbar)

        coordinatorLayout = findViewById(R.id.toolbar_coordinator)
        toolbar = findViewById(R.id.toolbar)

        mExternalFileHelper = ExternalFileHelper(this)

        if (savedInstanceState?.getString(TITLE_KEY).isNullOrEmpty())
            toolbar?.setTitle(R.string.settings)
        else
            toolbar?.title = savedInstanceState?.getString(TITLE_KEY)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        lockView = findViewById(R.id.lock_button)
        lockView?.setOnClickListener {
            lockAndExit()
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .add(R.id.fragment_container, retrieveMainFragment())
                    .commit()
        } else {
            lockView?.visibility = if (savedInstanceState.getBoolean(SHOW_LOCK)) View.VISIBLE else View.GONE
        }

        backupManager = BackupManager(this)

        // To reload the current screen
        if (intent.extras?.containsKey(FRAGMENT_ARG) == true) {
            intent.extras?.getString(FRAGMENT_ARG)?.let { fragmentScreenName ->
                onNestedPreferenceSelected(NestedSettingsFragment.Screen.valueOf(fragmentScreenName), true)
            }
            // Eat state
            intent.removeExtra(FRAGMENT_ARG)
        }
    }

    /**
     * Retrieve the main fragment to show in first
     * @return The main fragment
     */
    protected open fun retrieveMainFragment(): Fragment {
        return MainPreferenceFragment()
    }

    override fun viewToInvalidateTimeout(): View? {
        return coordinatorLayout
    }

    override fun finishActivityIfDatabaseNotLoaded(): Boolean {
        return false
    }

    override fun onDatabaseActionFinished(
        database: Database,
        actionTask: String,
        result: ActionRunnable.Result
    ) {
        super.onDatabaseActionFinished(database, actionTask, result)

        coordinatorLayout?.showActionErrorIfNeeded(result)
    }

    override fun reloadActivity() {
        keepCurrentScreen()
        super.reloadActivity()
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

    override fun onAssignKeyDialogPositiveClick(mainCredential: MainCredential) {
        assignPassword(mainCredential)
    }

    override fun onAssignKeyDialogNegativeClick(mainCredential: MainCredential) {}

    private fun hideOrShowLockButton(key: NestedSettingsFragment.Screen) {
        if (PreferencesUtil.showLockDatabaseButton(this)) {
            when (key) {
                NestedSettingsFragment.Screen.DATABASE,
                NestedSettingsFragment.Screen.DATABASE_MASTER_KEY,
                NestedSettingsFragment.Screen.DATABASE_SECURITY -> {
                    lockView?.visibility = View.VISIBLE
                }
                else -> {
                    lockView?.visibility = View.GONE
                }
            }
        } else {
            lockView?.visibility = View.GONE
        }
    }

    override fun onBackPressed() {
        // this if statement is necessary to navigate through nested and main fragments
        if (supportFragmentManager.backStackEntryCount == 0) {
            super.onBackPressed()
        } else {
            supportFragmentManager.popBackStack()
        }
        toolbar?.setTitle(R.string.settings)
        hideOrShowLockButton(NestedSettingsFragment.Screen.APPLICATION)
    }

    private fun replaceFragment(key: NestedSettingsFragment.Screen, reload: Boolean) {
        supportFragmentManager.beginTransaction().apply {
            if (reload) {
                setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                        R.anim.slide_in_left, R.anim.slide_out_right)
            } else {
                setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left, R.anim.slide_out_right)
            }
            replace(R.id.fragment_container, NestedSettingsFragment.newInstance(key), TAG_NESTED)
            addToBackStack(TAG_NESTED)
            commit()
        }

        toolbar?.title = NestedSettingsFragment.retrieveTitle(resources, key)
        hideOrShowLockButton(key)
    }

    /**
     * To keep the current screen when activity is reloaded
      */
    private fun keepCurrentScreen() {
        (supportFragmentManager.findFragmentByTag(TAG_NESTED) as? NestedSettingsFragment?)
                ?.getScreen()?.let { fragmentKey ->
            intent.putExtra(FRAGMENT_ARG, fragmentKey.name)
        }
    }

    override fun onNestedPreferenceSelected(key: NestedSettingsFragment.Screen, reload: Boolean) {
        if (mTimeoutEnable)
            checkTimeAndLockIfTimeoutOrResetTimeout {
                replaceFragment(key, reload)
            }
        else
            replaceFragment(key, reload)
    }

    fun importAppProperties() {
        mExternalFileHelper?.openDocument()
    }

    fun exportAppProperties() {
        appPropertiesFileCreationRequestCode = mExternalFileHelper?.createDocument(getString(R.string.app_properties_file_name,
            DateTime.now().toLocalDateTime().toString("yyyy-MM-dd'_'HH-mm")))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Import app properties result
        try {
            mExternalFileHelper?.onOpenDocumentResult(requestCode, resultCode, data) { selectedFileUri ->
                selectedFileUri?.let { uri ->
                    val appProperties = Properties()
                    contentResolver?.openInputStream(uri)?.use { inputStream ->
                        appProperties.load(inputStream)
                    }
                    PreferencesUtil.setAppProperties(this, appProperties)

                    // Restart the current activity
                    reloadActivity()
                    Toast.makeText(this, R.string.success_import_app_properties, Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, R.string.error_import_app_properties, Toast.LENGTH_LONG).show()
            Log.e(TAG, "Unable to import app properties", e)
        }

        // Export app properties result
        try {
            if (requestCode == appPropertiesFileCreationRequestCode) {
                mExternalFileHelper?.onCreateDocumentResult(requestCode, resultCode, data) { createdFileUri ->
                    createdFileUri?.let { uri ->
                        contentResolver?.openOutputStream(uri)?.use { outputStream ->
                            PreferencesUtil
                                    .getAppProperties(this)
                                    .store(outputStream, getString(R.string.description_app_properties))
                        }
                        Toast.makeText(this, R.string.success_export_app_properties, Toast.LENGTH_LONG).show()
                    }
                }
                appPropertiesFileCreationRequestCode = null
            }
        } catch (e: Exception) {
            Toast.makeText(this, R.string.error_export_app_properties, Toast.LENGTH_LONG).show()
            Log.e(DatabaseLockActivity.TAG, "Unable to export app properties", e)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(SHOW_LOCK, lockView?.visibility == View.VISIBLE)
        outState.putString(TITLE_KEY, toolbar?.title?.toString())
    }

    companion object {

        private val TAG = SettingsActivity::class.java.name

        private const val SHOW_LOCK = "SHOW_LOCK"
        private const val TITLE_KEY = "TITLE_KEY"
        private const val TAG_NESTED = "TAG_NESTED"
        private const val FRAGMENT_ARG = "FRAGMENT_ARG"

        fun launch(activity: Activity, timeoutEnable: Boolean) {
            val intent = Intent(activity, SettingsActivity::class.java)
            intent.putExtra(TIMEOUT_ENABLE_KEY, timeoutEnable)
            if (!timeoutEnable) {
                activity.startActivity(intent)
            } else if (TimeoutHelper.checkTimeAndLockIfTimeout(activity)) {
                activity.startActivity(intent)
            }
        }
    }
}
