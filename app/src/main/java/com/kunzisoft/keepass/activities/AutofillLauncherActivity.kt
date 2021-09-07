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
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.view.inputmethod.InlineSuggestionsRequest
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.activities.helpers.SpecialMode
import com.kunzisoft.keepass.activities.legacy.DatabaseModeActivity
import com.kunzisoft.keepass.autofill.AutofillHelper
import com.kunzisoft.keepass.autofill.AutofillHelper.EXTRA_INLINE_SUGGESTIONS_REQUEST
import com.kunzisoft.keepass.autofill.KeeAutofillService
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.search.SearchHelper
import com.kunzisoft.keepass.model.RegisterInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.LOCK_ACTION

@RequiresApi(api = Build.VERSION_CODES.O)
class AutofillLauncherActivity : DatabaseModeActivity() {

    override fun applyCustomStyle(): Boolean {
        return false
    }

    override fun finishActivityIfReloadRequested(): Boolean {
        return true
    }

    override fun onDatabaseRetrieved(database: Database?) {
        super.onDatabaseRetrieved(database)

        // Retrieve selection mode
        EntrySelectionHelper.retrieveSpecialModeFromIntent(intent).let { specialMode ->
            when (specialMode) {
                SpecialMode.SELECTION -> {
                    // Build search param
                    val searchInfo = SearchInfo().apply {
                        applicationId = intent.getStringExtra(KEY_SEARCH_APPLICATION_ID)
                        webDomain = intent.getStringExtra(KEY_SEARCH_DOMAIN)
                        webScheme = intent.getStringExtra(KEY_SEARCH_SCHEME)
                        manualSelection = intent.getBooleanExtra(KEY_MANUAL_SELECTION, false)
                    }
                    SearchInfo.getConcreteWebDomain(this, searchInfo.webDomain) { concreteWebDomain ->
                        searchInfo.webDomain = concreteWebDomain
                        launchSelection(database, searchInfo)
                    }
                }
                SpecialMode.REGISTRATION -> {
                    // To register info
                    val registerInfo = intent.getParcelableExtra<RegisterInfo>(KEY_REGISTER_INFO)
                    val searchInfo = SearchInfo(registerInfo?.searchInfo)
                    SearchInfo.getConcreteWebDomain(this, searchInfo.webDomain) { concreteWebDomain ->
                        searchInfo.webDomain = concreteWebDomain
                        launchRegistration(database, searchInfo, registerInfo)
                    }
                }
                else -> {
                    // Not an autofill call
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
            }
        }
    }

    private fun launchSelection(database: Database?,
                                searchInfo: SearchInfo) {
        // Pass extra for Autofill (EXTRA_ASSIST_STRUCTURE)
        val autofillComponent = AutofillHelper.retrieveAutofillComponent(intent)

        if (autofillComponent == null) {
            setResult(Activity.RESULT_CANCELED)
            finish()
        } else if (!KeeAutofillService.autofillAllowedFor(searchInfo.applicationId,
                        PreferencesUtil.applicationIdBlocklist(this))
                || !KeeAutofillService.autofillAllowedFor(searchInfo.webDomain,
                        PreferencesUtil.webDomainBlocklist(this))) {
            showBlockRestartMessage()
            setResult(Activity.RESULT_CANCELED)
            finish()
        } else {
            // If database is open
            SearchHelper.checkAutoSearchInfo(this,
                    database,
                    searchInfo,
                    { openedDatabase, items ->
                        // Items found
                        AutofillHelper.buildResponseAndSetResult(this, openedDatabase, items)
                        finish()
                    },
                    { openedDatabase ->
                        // Show the database UI to select the entry
                        GroupActivity.launchForAutofillResult(this,
                            openedDatabase,
                            autofillComponent,
                            searchInfo,
                            false)
                    },
                    {
                        // If database not open
                        FileDatabaseSelectActivity.launchForAutofillResult(this,
                                autofillComponent,
                                searchInfo)
                    }
            )
        }
    }

    private fun launchRegistration(database: Database?,
                                   searchInfo: SearchInfo,
                                   registerInfo: RegisterInfo?) {
        if (!KeeAutofillService.autofillAllowedFor(searchInfo.applicationId,
                        PreferencesUtil.applicationIdBlocklist(this))
                || !KeeAutofillService.autofillAllowedFor(searchInfo.webDomain,
                        PreferencesUtil.webDomainBlocklist(this))) {
            showBlockRestartMessage()
            setResult(Activity.RESULT_CANCELED)
        } else {
            val readOnly = database?.isReadOnly != false
            SearchHelper.checkAutoSearchInfo(this,
                    database,
                    searchInfo,
                    { openedDatabase, _ ->
                        if (!readOnly) {
                            // Show the database UI to select the entry
                            GroupActivity.launchForRegistration(this,
                                openedDatabase,
                                registerInfo)
                        } else {
                            showReadOnlySaveMessage()
                        }
                    },
                    { openedDatabase ->
                        if (!readOnly) {
                            // Show the database UI to select the entry
                            GroupActivity.launchForRegistration(this,
                                openedDatabase,
                                registerInfo)
                        } else {
                            showReadOnlySaveMessage()
                        }
                    },
                    {
                        // If database not open
                        FileDatabaseSelectActivity.launchForRegistration(this,
                                registerInfo)
                    }
            )
        }
        finish()
    }

    private fun showBlockRestartMessage() {
        // If item not allowed, show a toast
        Toast.makeText(this.applicationContext, R.string.autofill_block_restart, Toast.LENGTH_LONG).show()
    }

    private fun showReadOnlySaveMessage() {
        Toast.makeText(this.applicationContext, R.string.autofill_read_only_save, Toast.LENGTH_LONG).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        AutofillHelper.onActivityResultSetResultAndFinish(this, requestCode, resultCode, data)

        if (PreferencesUtil.isAutofillCloseDatabaseEnable(this)) {
            // Close the database
            sendBroadcast(Intent(LOCK_ACTION))
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {

        private const val KEY_MANUAL_SELECTION = "KEY_MANUAL_SELECTION"
        private const val KEY_SEARCH_APPLICATION_ID = "KEY_SEARCH_APPLICATION_ID"
        private const val KEY_SEARCH_DOMAIN = "KEY_SEARCH_DOMAIN"
        private const val KEY_SEARCH_SCHEME = "KEY_SEARCH_SCHEME"

        private const val KEY_REGISTER_INFO = "KEY_REGISTER_INFO"

        fun getPendingIntentForSelection(context: Context,
                                         searchInfo: SearchInfo? = null,
                                         inlineSuggestionsRequest: InlineSuggestionsRequest? = null): PendingIntent {
            return PendingIntent.getActivity(context, 0,
                    // Doesn't work with Parcelable (don't know why?)
                    Intent(context, AutofillLauncherActivity::class.java).apply {
                        searchInfo?.let {
                            putExtra(KEY_SEARCH_APPLICATION_ID, it.applicationId)
                            putExtra(KEY_SEARCH_DOMAIN, it.webDomain)
                            putExtra(KEY_SEARCH_SCHEME, it.webScheme)
                            putExtra(KEY_MANUAL_SELECTION, it.manualSelection)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            inlineSuggestionsRequest?.let {
                                putExtra(EXTRA_INLINE_SUGGESTIONS_REQUEST, it)
                            }
                        }
                    },
                    PendingIntent.FLAG_CANCEL_CURRENT)
        }

        fun getPendingIntentForRegistration(context: Context,
                                            registerInfo: RegisterInfo): PendingIntent {
            return PendingIntent.getActivity(context, 0,
                    Intent(context, AutofillLauncherActivity::class.java).apply {
                        EntrySelectionHelper.addSpecialModeInIntent(this, SpecialMode.REGISTRATION)
                        putExtra(KEY_REGISTER_INFO, registerInfo)
                    },
                    PendingIntent.FLAG_CANCEL_CURRENT)
        }

        fun launchForRegistration(context: Context,
                                  registerInfo: RegisterInfo) {
            val intent = Intent(context, AutofillLauncherActivity::class.java)
            EntrySelectionHelper.addSpecialModeInIntent(intent, SpecialMode.REGISTRATION)
            intent.putExtra(KEY_REGISTER_INFO, registerInfo)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
