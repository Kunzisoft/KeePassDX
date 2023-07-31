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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.legacy.DatabaseModeActivity
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.helper.SearchHelper
import com.kunzisoft.keepass.magikeyboard.MagikeyboardService
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.otp.OtpEntryFields
import com.kunzisoft.keepass.utils.KeyboardUtil.isKeyboardActivatedInSettings
import com.kunzisoft.keepass.utils.getParcelableCompat
import com.kunzisoft.keepass.utils.WebDomain

/**
 * Activity to search or select entry in database,
 * Commonly used with Magikeyboard
 */
class EntrySelectionLauncherActivity : DatabaseModeActivity() {

    override fun applyCustomStyle(): Boolean {
        return false
    }

    override fun finishActivityIfReloadRequested(): Boolean {
        return false
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)

        val keySelectionBundle = intent.getBundleExtra(KEY_SELECTION_BUNDLE)
        if (keySelectionBundle != null) {
            // To manage package name
            var searchInfo = SearchInfo()
            keySelectionBundle.getParcelableCompat<SearchInfo>(KEY_SEARCH_INFO)?.let { mSearchInfo ->
                searchInfo = mSearchInfo
            }
            launch(database, searchInfo)
        } else {
            // To manage share
            var sharedWebDomain: String? = null
            var otpString: String? = null

            when (intent?.action) {
                Intent.ACTION_SEND -> {
                    if ("text/plain" == intent.type) {
                        // Retrieve web domain or OTP
                        intent.getStringExtra(Intent.EXTRA_TEXT)?.let { extra ->
                            if (OtpEntryFields.isOTPUri(extra))
                                otpString = extra
                            else
                                sharedWebDomain = Uri.parse(extra).host
                        }
                    }
                    launchSelection(database, sharedWebDomain, otpString)
                }
                Intent.ACTION_VIEW -> {
                    // Retrieve OTP
                    intent.dataString?.let { extra ->
                        if (OtpEntryFields.isOTPUri(extra))
                            otpString = extra
                    }
                    launchSelection(database, sharedWebDomain, otpString)
                }
                else -> {
                    if (database != null) {
                        GroupActivity.launch(this, database)
                    } else {
                        FileDatabaseSelectActivity.launch(this)
                    }
                }
            }
        }
        finish()
    }

    private fun launchSelection(database: ContextualDatabase?,
                                sharedWebDomain: String?,
                                otpString: String?) {
        // Build domain search param
        val searchInfo = SearchInfo().apply {
            this.webDomain = sharedWebDomain
            this.otpString = otpString
        }

        WebDomain.getConcreteWebDomain(this, searchInfo.webDomain) { concreteWebDomain ->
            searchInfo.webDomain = concreteWebDomain
            launch(database, searchInfo)
        }
    }

    private fun launch(database: ContextualDatabase?,
                       searchInfo: SearchInfo) {

        // Setting to integrate Magikeyboard
        val searchShareForMagikeyboard = isKeyboardActivatedInSettings()

        // If database is open
        val readOnly = database?.isReadOnly != false
        SearchHelper.checkAutoSearchInfo(this,
                database,
                searchInfo,
                { openedDatabase, items ->
                    // Items found
                    if (searchInfo.otpString != null) {
                        if (!readOnly) {
                            GroupActivity.launchForSaveResult(
                                this,
                                openedDatabase,
                                searchInfo,
                                false)
                        } else {
                            Toast.makeText(applicationContext,
                                    R.string.autofill_read_only_save,
                                    Toast.LENGTH_LONG)
                                    .show()
                        }
                    } else if (searchShareForMagikeyboard) {
                        MagikeyboardService.performSelection(
                            items,
                            { entryInfo ->
                                // Automatically populate keyboard
                                MagikeyboardService.populateKeyboardAndMoveAppToBackground(
                                    this,
                                    entryInfo
                                )
                            },
                            { autoSearch ->
                                GroupActivity.launchForKeyboardSelectionResult(this,
                                    openedDatabase,
                                    searchInfo,
                                    autoSearch)
                            }
                        )
                    } else {
                        GroupActivity.launchForSearchResult(this,
                            openedDatabase,
                            searchInfo,
                            true)
                    }
                },
                { openedDatabase ->
                    // Show the database UI to select the entry
                    if (searchInfo.otpString != null) {
                        if (!readOnly) {
                            GroupActivity.launchForSaveResult(this,
                                openedDatabase,
                                searchInfo,
                                false)
                        } else {
                            Toast.makeText(applicationContext,
                                    R.string.autofill_read_only_save,
                                    Toast.LENGTH_LONG)
                                    .show()
                        }
                    } else if (searchShareForMagikeyboard) {
                        GroupActivity.launchForKeyboardSelectionResult(this,
                            openedDatabase,
                            searchInfo,
                            false)
                    } else {
                        GroupActivity.launchForSearchResult(this,
                            openedDatabase,
                            searchInfo,
                            false)
                    }
                },
                {
                    // If database not open
                    if (searchInfo.otpString != null) {
                        FileDatabaseSelectActivity.launchForSaveResult(this,
                                searchInfo)
                    } else if (searchShareForMagikeyboard) {
                        FileDatabaseSelectActivity.launchForKeyboardSelectionResult(this,
                                searchInfo)
                    } else {
                        FileDatabaseSelectActivity.launchForSearchResult(this,
                                searchInfo)
                    }
                }
        )
    }

    companion object {

        private const val KEY_SELECTION_BUNDLE = "KEY_SELECTION_BUNDLE"
        private const val KEY_SEARCH_INFO = "KEY_SEARCH_INFO"

        fun launch(context: Context,
                   searchInfo: SearchInfo? = null) {
            val intent = Intent(context, EntrySelectionLauncherActivity::class.java).apply {
                putExtra(KEY_SELECTION_BUNDLE, Bundle().apply {
                    putParcelable(KEY_SEARCH_INFO, searchInfo)
                })
            }
            // New task needed because don't launch from an Activity context
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }
    }
}
