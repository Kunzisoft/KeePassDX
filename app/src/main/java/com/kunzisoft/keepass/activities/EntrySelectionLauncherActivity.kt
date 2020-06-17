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
import androidx.appcompat.app.AppCompatActivity
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.search.SearchHelper
import com.kunzisoft.keepass.magikeyboard.MagikIME
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.settings.PreferencesUtil

/**
 * Activity to search or select entry in database,
 * Commonly used with Magikeyboard
 */
class EntrySelectionLauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        var sharedWebDomain: String? = null

        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if ("text/plain" == intent.type) {
                    // Retrieve web domain
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                        sharedWebDomain = Uri.parse(it).host
                    }
                }
            }
            else -> {}
        }

        // Setting to integrate Magikeyboard
        val searchShareForMagikeyboard = PreferencesUtil.isKeyboardSearchShareEnable(this)

        // Build search param
        val searchInfo = SearchInfo().apply {
            type = if (searchShareForMagikeyboard)
                getString(R.string.keyboard_name)
            else
                getString(R.string.search)
            webDomain = sharedWebDomain
        }

        // If database is open
        SearchHelper.checkAutoSearchInfo(this,
                Database.getInstance(),
                searchInfo,
                { items ->
                    // Items found
                    if (searchShareForMagikeyboard) {
                        if (items.size == 1) {
                            // Automatically populate keyboard
                            val entryPopulate = items[0]
                            populateKeyboardAndMoveAppToBackground(this,
                                    entryPopulate,
                                    intent)
                        } else {
                            // Select the one we want
                            GroupActivity.launchForEntrySelectionResult(this,
                                    true,
                                    searchInfo)
                        }
                    } else {
                        GroupActivity.launch(this,
                                true,
                                searchInfo)
                    }
                },
                {
                    // Show the database UI to select the entry
                    if (searchShareForMagikeyboard) {
                        GroupActivity.launchForEntrySelectionResult(this,
                                false,
                                searchInfo)
                    } else {
                        GroupActivity.launch(this,
                                false,
                                searchInfo)
                    }
                },
                {
                    // If database not open
                    if (searchShareForMagikeyboard) {
                        FileDatabaseSelectActivity.launchForEntrySelectionResult(this,
                                searchInfo)
                    } else {
                        FileDatabaseSelectActivity.launch(this,
                                searchInfo)
                    }
                }
        )

        finish()

        super.onCreate(savedInstanceState)
    }
}

fun populateKeyboardAndMoveAppToBackground(activity: Activity,
                                           entry: EntryInfo,
                                           intent: Intent,
                                           toast: Boolean = true) {
    // Populate Magikeyboard with entry
    MagikIME.addEntryAndLaunchNotificationIfAllowed(activity, entry, toast)
    // Consume the selection mode
    EntrySelectionHelper.removeEntrySelectionModeFromIntent(intent)
    activity.moveTaskToBack(true)
}
