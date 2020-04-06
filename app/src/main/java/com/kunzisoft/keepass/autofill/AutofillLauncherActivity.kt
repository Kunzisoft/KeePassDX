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
package com.kunzisoft.keepass.autofill

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.kunzisoft.keepass.activities.FileDatabaseSelectActivity
import com.kunzisoft.keepass.activities.GroupActivity
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.search.SearchHelper.Companion.MAX_SEARCH_ENTRY
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.timeout.TimeoutHelper

@RequiresApi(api = Build.VERSION_CODES.O)
class AutofillLauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Pass extra for Autofill (EXTRA_ASSIST_STRUCTURE)
        val assistStructure = AutofillHelper.retrieveAssistStructure(intent)
        if (assistStructure != null) {
            val database = Database.getInstance()
            // Build search param
            val searchInfo = SearchInfo().apply {
                applicationId = intent.getStringExtra(KEY_SEARCH_APPLICATION_ID)
                webDomain = intent.getStringExtra(KEY_SEARCH_DOMAIN)
            }
            // If database is open
            if (database.loaded && TimeoutHelper.checkTime(this)) {
                var searchWithoutUI = false
                // If search provide results
                database.createVirtualGroupFromSearch(searchInfo, MAX_SEARCH_ENTRY)?.let { searchGroup ->
                    if (searchGroup.getNumberOfChildEntries() > 0) {
                        // Build response with the entry selected
                        AutofillHelper.buildResponse(this@AutofillLauncherActivity,
                                searchGroup.getChildEntriesInfo(database))
                        searchWithoutUI = true
                        finish()
                    }
                }

                // Show the database UI to select the entry
                if (!searchWithoutUI) {
                    GroupActivity.launchForAutofillResult(this,
                            assistStructure, searchInfo)
                }
            } else {
                FileDatabaseSelectActivity.launchForAutofillResult(this,
                        assistStructure, searchInfo)
            }
        } else {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        super.onCreate(savedInstanceState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        AutofillHelper.onActivityResultSetResultAndFinish(this, requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {

        private const val KEY_SEARCH_APPLICATION_ID = "KEY_SEARCH_APPLICATION_ID"
        private const val KEY_SEARCH_DOMAIN = "KEY_SEARCH_DOMAIN"

        fun getAuthIntentSenderForResponse(context: Context,
                                           searchInfo: SearchInfo? = null): IntentSender {
            return PendingIntent.getActivity(context, 0,
                    // Doesn't work with Parcelable (don't know why?)
                    Intent(context, AutofillLauncherActivity::class.java).apply {
                        searchInfo?.let {
                            putExtra(KEY_SEARCH_APPLICATION_ID, it.applicationId)
                            putExtra(KEY_SEARCH_DOMAIN, it.webDomain)
                        }
                    },
                    PendingIntent.FLAG_CANCEL_CURRENT).intentSender
        }
    }
}
