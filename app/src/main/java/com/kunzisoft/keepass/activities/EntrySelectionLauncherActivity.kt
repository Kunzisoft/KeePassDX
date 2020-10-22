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
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.search.SearchHelper
import com.kunzisoft.keepass.magikeyboard.MagikIME
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.UriUtil

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

        // Build search param
        val searchInfo = SearchInfo().apply {
            webDomain = sharedWebDomain
        }
        if (!PreferencesUtil.searchSubdomains(this)) {
            UriUtil.getWebDomainWithoutSubDomain(this, sharedWebDomain) { webDomainWithoutSubDomain ->
                searchInfo.webDomain = webDomainWithoutSubDomain
                launch(searchInfo)
            }
        } else {
            launch(searchInfo)
        }

        super.onCreate(savedInstanceState)
    }

    private fun launch(searchInfo: SearchInfo) {
        // Setting to integrate Magikeyboard
        val searchShareForMagikeyboard = PreferencesUtil.isKeyboardSearchShareEnable(this)

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
                            GroupActivity.launchForKeyboardSelectionResult(this,
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
                        GroupActivity.launchForKeyboardSelectionResult(this,
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
                        FileDatabaseSelectActivity.launchForKeyboardSelectionResult(this,
                                searchInfo)
                    } else {
                        FileDatabaseSelectActivity.launch(this,
                                searchInfo)
                    }
                }
        )

        finish()
    }
}

fun populateKeyboardAndMoveAppToBackground(activity: Activity,
                                           entry: EntryInfo,
                                           intent: Intent,
                                           toast: Boolean = true) {
    // Populate Magikeyboard with entry
    MagikIME.addEntryAndLaunchNotificationIfAllowed(activity, entry, toast)
    // Consume the selection mode
    EntrySelectionHelper.removeModesFromIntent(intent)
    activity.moveTaskToBack(true)
}
