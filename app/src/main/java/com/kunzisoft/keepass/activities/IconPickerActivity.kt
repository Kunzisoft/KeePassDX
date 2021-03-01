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
package com.kunzisoft.keepass.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.commit
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.fragments.IconPickerFragment
import com.kunzisoft.keepass.activities.helpers.SelectFileHelper
import com.kunzisoft.keepass.activities.lock.LockingActivity
import com.kunzisoft.keepass.activities.lock.resetAppTimeoutWhenViewFocusedOrChanged
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.UriUtil
import com.kunzisoft.keepass.view.updateLockPaddingLeft
import com.kunzisoft.keepass.viewmodels.IconPickerViewModel


class IconPickerActivity : LockingActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var uploadButton: View
    private var lockView: View? = null

    private val iconPickerViewModel: IconPickerViewModel by viewModels()

    private var mDatabase: Database? = null

    private var mSelectFileHelper: SelectFileHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_icon_picker)

        toolbar = findViewById(R.id.toolbar)
        toolbar.title = getString(R.string.about)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        uploadButton = findViewById(R.id.icon_picker_upload)
        uploadButton.setOnClickListener {
            mSelectFileHelper?.selectFileOnClickViewListener?.onClick(it)
        }
        uploadButton.setOnLongClickListener {
            mSelectFileHelper?.selectFileOnClickViewListener?.onLongClick(it)
            true
        }

        mDatabase = Database.getInstance()

        lockView = findViewById(R.id.lock_button)
        lockView?.setOnClickListener {
            lockAndExit()
        }

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add(R.id.icon_picker_fragment, IconPickerFragment(), ICON_PICKER_FRAGMENT_TAG)
            }
        }

        // Focus view to reinitialize timeout
        findViewById<ViewGroup>(R.id.icon_picker_container)?.resetAppTimeoutWhenViewFocusedOrChanged(this)

        mSelectFileHelper = SelectFileHelper(this)

        // TODO  keep previous standard icon id
        iconPickerViewModel.iconStandardSelected.observe(this) { iconStandard ->
            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra(EXTRA_ICON, IconImage(iconStandard))
            })
            finish()
        }
        iconPickerViewModel.iconCustomSelected.observe(this) { iconCustom ->
            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra(EXTRA_ICON, IconImage(iconCustom))
            })
            finish()
        }
    }

    override fun onResume() {
        super.onResume()

        // Show the lock button
        lockView?.visibility = if (PreferencesUtil.showLockDatabaseButton(this)) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // Padding if lock button visible
        toolbar.updateLockPaddingLeft()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        mSelectFileHelper?.onActivityResultCallback(requestCode, resultCode, data) { uri ->
            uri?.let { iconToUploadUri ->
                UriUtil.getFileData(this, iconToUploadUri)?.also { documentFile ->
                    if (documentFile.length() <= MAX_ICON_SIZE) {
                        mDatabase?.buildNewCustomIcon(UriUtil.getBinaryDir(this))
                    } else {
                        // TODO Error Icon size too big
                    }
                }
            }
        }
    }

    companion object {

        private const val ICON_PICKER_FRAGMENT_TAG = "ICON_PICKER_FRAGMENT_TAG"

        private const val ICON_SELECTED_REQUEST = 15861
        private const val EXTRA_ICON = "EXTRA_ICON"

        private const val MAX_ICON_SIZE = 5242880

        fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?, listener: (icon: IconImage) -> Unit) {
            if (requestCode == ICON_SELECTED_REQUEST) {
                if (resultCode == Activity.RESULT_OK) {
                    listener.invoke(data?.getParcelableExtra(EXTRA_ICON) ?: IconImage())
                }
            }
        }

        fun launch(context: Activity) {
            // Create an instance to return the picker icon
            context.startActivityForResult(Intent(context, IconPickerActivity::class.java), ICON_SELECTED_REQUEST)
        }
    }
}
