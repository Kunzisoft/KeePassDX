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
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.commit
import com.google.android.material.snackbar.Snackbar
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.IconEditDialogFragment
import com.kunzisoft.keepass.activities.fragments.IconPickerFragment
import com.kunzisoft.keepass.activities.helpers.ExternalFileHelper
import com.kunzisoft.keepass.activities.helpers.setOpenDocumentClickListener
import com.kunzisoft.keepass.activities.legacy.DatabaseLockActivity
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.icon.IconImageCustom
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.BinaryDatabaseManager
import com.kunzisoft.keepass.utils.UriUtil
import com.kunzisoft.keepass.view.asError
import com.kunzisoft.keepass.view.updateLockPaddingLeft
import com.kunzisoft.keepass.viewmodels.IconPickerViewModel
import kotlinx.coroutines.*


class IconPickerActivity : DatabaseLockActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var coordinatorLayout: CoordinatorLayout
    private lateinit var uploadButton: View
    private var lockView: View? = null

    private var mIconImage: IconImage = IconImage()

    private val mainScope = CoroutineScope(Dispatchers.Main)

    private val iconPickerViewModel: IconPickerViewModel by viewModels()
    private var mCustomIconsSelectionMode = false
    private var mIconsSelected: List<IconImageCustom> = ArrayList()

    private var mExternalFileHelper: ExternalFileHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_icon_picker)

        toolbar = findViewById(R.id.toolbar)
        toolbar.title = " "
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        updateIconsSelectedViews()

        coordinatorLayout = findViewById(R.id.icon_picker_coordinator)

        mExternalFileHelper = ExternalFileHelper(this)

        uploadButton = findViewById(R.id.icon_picker_upload)

        lockView = findViewById(R.id.lock_button)
        lockView?.setOnClickListener {
            lockAndExit()
        }

        intent?.getParcelableExtra<IconImage>(EXTRA_ICON)?.let {
            mIconImage = it
        }

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add(R.id.icon_picker_fragment, IconPickerFragment.getInstance(
                        // Default selection tab
                        if (mIconImage.custom.isUnknown)
                            IconPickerFragment.IconTab.STANDARD
                        else
                            IconPickerFragment.IconTab.CUSTOM
                ), ICON_PICKER_FRAGMENT_TAG)
            }
        } else {
            mIconImage = savedInstanceState.getParcelable(EXTRA_ICON) ?: mIconImage
        }

        iconPickerViewModel.standardIconPicked.observe(this) { iconStandard ->
            mIconImage.standard = iconStandard
            // Remove the custom icon if a standard one is selected
            mIconImage.custom = IconImageCustom()
            setResult()
            finish()
        }
        iconPickerViewModel.customIconPicked.observe(this) { iconCustom ->
            // Keep the standard icon if a custom one is selected
            mIconImage.custom = iconCustom
            setResult()
            finish()
        }
        iconPickerViewModel.customIconsSelected.observe(this) { iconsSelected ->
            mIconsSelected = iconsSelected
            updateIconsSelectedViews()
        }
        iconPickerViewModel.customIconAdded.observe(this) { iconCustomAdded ->
            if (iconCustomAdded.error && !iconCustomAdded.errorConsumed) {
                Snackbar.make(coordinatorLayout, iconCustomAdded.errorStringId, Snackbar.LENGTH_LONG).asError().show()
                iconCustomAdded.errorConsumed = true
            }
            uploadButton.isEnabled = true
        }
        iconPickerViewModel.customIconRemoved.observe(this) { iconCustomRemoved ->
            if (iconCustomRemoved.error && !iconCustomRemoved.errorConsumed) {
                Snackbar.make(coordinatorLayout, iconCustomRemoved.errorStringId, Snackbar.LENGTH_LONG).asError().show()
                iconCustomRemoved.errorConsumed = true
            }
            uploadButton.isEnabled = true
        }
        iconPickerViewModel.customIconUpdated.observe(this) { iconCustomUpdated ->
            if (iconCustomUpdated.error && !iconCustomUpdated.errorConsumed) {
                Snackbar.make(coordinatorLayout, iconCustomUpdated.errorStringId, Snackbar.LENGTH_LONG).asError().show()
                iconCustomUpdated.errorConsumed = true
            }
            iconCustomUpdated.iconCustom?.let {
                mDatabase?.updateCustomIcon(it)
            }
            iconPickerViewModel.deselectAllCustomIcons()
        }
    }

    override fun viewToInvalidateTimeout(): View? {
        return findViewById<ViewGroup>(R.id.icon_picker_container)
    }

    override fun finishActivityIfReloadRequested(): Boolean {
        return true
    }

    override fun onDatabaseRetrieved(database: Database?) {
        super.onDatabaseRetrieved(database)

        if (database?.allowCustomIcons == true) {
            uploadButton.setOpenDocumentClickListener(mExternalFileHelper)
        } else {
            uploadButton.visibility = View.GONE
        }
    }

    private fun updateIconsSelectedViews() {
        if (mIconsSelected.isEmpty()) {
            mCustomIconsSelectionMode = false
            toolbar.title = " "
        } else {
            mCustomIconsSelectionMode = true
            toolbar.title = mIconsSelected.size.toString()
        }
        invalidateOptionsMenu()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(EXTRA_ICON, mIconImage)
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.icon, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.menu_edit)?.apply {
            isEnabled = mIconsSelected.size == 1
            isVisible = isEnabled
        }
        menu?.findItem(R.id.menu_delete)?.apply {
            isEnabled = mCustomIconsSelectionMode
            isVisible = isEnabled
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (mCustomIconsSelectionMode) {
                    iconPickerViewModel.deselectAllCustomIcons()
                } else {
                    onBackPressed()
                }
            }
            R.id.menu_edit -> {
                updateCustomIcon(mIconsSelected[0])
            }
            R.id.menu_delete -> {
                mIconsSelected.forEach { iconToRemove ->
                    removeCustomIcon(iconToRemove)
                }
            }
            R.id.menu_external_icon -> {
                UriUtil.gotoUrl(this, R.string.external_icon_url)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun addCustomIcon(iconToUploadUri: Uri?) {
        uploadButton.isEnabled = false
        mainScope.launch {
            withContext(Dispatchers.IO) {
                // on Progress with thread
                val asyncResult: Deferred<IconPickerViewModel.IconCustomState?> = async {
                    val iconCustomState = IconPickerViewModel.IconCustomState(null, true, R.string.error_upload_file)
                    UriUtil.getFileData(this@IconPickerActivity, iconToUploadUri)?.also { documentFile ->
                        if (documentFile.length() > MAX_ICON_SIZE) {
                            iconCustomState.errorStringId = R.string.error_file_to_big
                        } else {
                            mDatabase?.buildNewCustomIcon { customIcon, binary ->
                                if (customIcon != null) {
                                    iconCustomState.iconCustom = customIcon
                                    mDatabase?.let { database ->
                                        BinaryDatabaseManager.resizeBitmapAndStoreDataInBinaryFile(
                                                contentResolver,
                                                database,
                                                iconToUploadUri,
                                                binary)
                                        when {
                                            binary == null -> {
                                            }
                                            binary.getSize() <= 0 -> {
                                            }
                                            database.isCustomIconBinaryDuplicate(binary) -> {
                                                iconCustomState.errorStringId = R.string.error_duplicate_file
                                            }
                                            else -> {
                                                iconCustomState.error = false
                                            }
                                        }
                                    }
                                    if (iconCustomState.error) {
                                        mDatabase?.removeCustomIcon(customIcon)
                                    }
                                }
                            }
                        }
                    }
                    iconCustomState
                }
                withContext(Dispatchers.Main) {
                    asyncResult.await()?.let { customIcon ->
                        iconPickerViewModel.addCustomIcon(customIcon)
                    }
                }
            }
        }
    }

    private fun updateCustomIcon(iconImageCustom: IconImageCustom) {
        IconEditDialogFragment.update(iconImageCustom)
            .show(supportFragmentManager, IconEditDialogFragment.TAG_UPDATE_ICON)
    }

    private fun removeCustomIcon(iconImageCustom: IconImageCustom) {
        uploadButton.isEnabled = false
        iconPickerViewModel.deselectAllCustomIcons()
        mDatabase?.removeCustomIcon(iconImageCustom)
        iconPickerViewModel.removeCustomIcon(
                IconPickerViewModel.IconCustomState(iconImageCustom, false, R.string.error_remove_file)
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        mExternalFileHelper?.onOpenDocumentResult(requestCode, resultCode, data) { uri ->
            addCustomIcon(uri)
        }
    }

    private fun setResult() {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(EXTRA_ICON, mIconImage)
        })
    }

    override fun onBackPressed() {
        setResult()
        super.onBackPressed()
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

        fun launch(context: Activity,
                   previousIcon: IconImage?) {
            // Create an instance to return the picker icon
            context.startActivityForResult(
                    Intent(context,
                    IconPickerActivity::class.java).apply {
                        if (previousIcon != null)
                            putExtra(EXTRA_ICON, previousIcon)
                    },
                    ICON_SELECTED_REQUEST)
        }
    }
}
