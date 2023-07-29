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
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.activities.helpers.SpecialMode
import com.kunzisoft.keepass.activities.legacy.DatabaseModeActivity
import com.kunzisoft.keepass.autofill.AutofillComponent
import com.kunzisoft.keepass.autofill.AutofillHelper
import com.kunzisoft.keepass.autofill.CompatInlineSuggestionsRequest
import com.kunzisoft.keepass.autofill.KeeAutofillService
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.helper.SearchHelper
import com.kunzisoft.keepass.model.RegisterInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.getParcelableCompat
import com.kunzisoft.keepass.utils.getParcelableExtraCompat
import com.kunzisoft.keepass.utils.WebDomain

@RequiresApi(api = Build.VERSION_CODES.O)
class AutofillLauncherActivity : DatabaseModeActivity() {

    private var mAutofillActivityResultLauncher: ActivityResultLauncher<Intent>? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            AutofillHelper.buildActivityResultLauncher(this, true)
        else null

    override fun applyCustomStyle(): Boolean {
        return false
    }

    override fun finishActivityIfReloadRequested(): Boolean {
        return true
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)

        // Retrieve selection mode
        EntrySelectionHelper.retrieveSpecialModeFromIntent(intent).let { specialMode ->
            when (specialMode) {
                SpecialMode.SELECTION -> {
                    intent.getBundleExtra(KEY_SELECTION_BUNDLE)?.let { bundle ->
                        // To pass extra inline request
                        var compatInlineSuggestionsRequest: CompatInlineSuggestionsRequest? = null
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            compatInlineSuggestionsRequest = bundle.getParcelableCompat(KEY_INLINE_SUGGESTION)
                        }
                        // Build search param
                        bundle.getParcelableCompat<SearchInfo>(KEY_SEARCH_INFO)?.let { searchInfo ->
                            WebDomain.getConcreteWebDomain(
                                this,
                                searchInfo.webDomain
                            ) { concreteWebDomain ->
                                // Pass extra for Autofill (EXTRA_ASSIST_STRUCTURE)
                                val assistStructure = AutofillHelper
                                    .retrieveAutofillComponent(intent)
                                    ?.assistStructure
                                val newAutofillComponent = if (assistStructure != null) {
                                    AutofillComponent(
                                        assistStructure,
                                        compatInlineSuggestionsRequest
                                    )
                                } else {
                                    null
                                }
                                searchInfo.webDomain = concreteWebDomain
                                launchSelection(database, newAutofillComponent, searchInfo)
                            }
                        }
                    }
                    // Remove bundle
                    intent.removeExtra(KEY_SELECTION_BUNDLE)
                }
                SpecialMode.REGISTRATION -> {
                    // To register info
                    val registerInfo = intent.getParcelableExtraCompat<RegisterInfo>(KEY_REGISTER_INFO)
                    val searchInfo = SearchInfo(registerInfo?.searchInfo)
                    WebDomain.getConcreteWebDomain(this, searchInfo.webDomain) { concreteWebDomain ->
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

    private fun launchSelection(database: ContextualDatabase?,
                                autofillComponent: AutofillComponent?,
                                searchInfo: SearchInfo) {
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
                            mAutofillActivityResultLauncher,
                            autofillComponent,
                            searchInfo,
                            false)
                    },
                    {
                        // If database not open
                        FileDatabaseSelectActivity.launchForAutofillResult(this,
                                mAutofillActivityResultLauncher,
                                autofillComponent,
                                searchInfo)
                    }
            )
        }
    }

    private fun launchRegistration(database: ContextualDatabase?,
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

    companion object {

        private const val KEY_SELECTION_BUNDLE = "KEY_SELECTION_BUNDLE"
        private const val KEY_SEARCH_INFO = "KEY_SEARCH_INFO"
        private const val KEY_INLINE_SUGGESTION = "KEY_INLINE_SUGGESTION"

        private const val KEY_REGISTER_INFO = "KEY_REGISTER_INFO"

        fun getPendingIntentForSelection(context: Context,
                                         searchInfo: SearchInfo? = null,
                                         compatInlineSuggestionsRequest: CompatInlineSuggestionsRequest? = null): PendingIntent {
            return PendingIntent.getActivity(context, 0,
                // Doesn't work with direct extra Parcelable (don't know why?)
                // Wrap into a bundle to bypass the problem
                Intent(context, AutofillLauncherActivity::class.java).apply {
                    putExtra(KEY_SELECTION_BUNDLE, Bundle().apply {
                        putParcelable(KEY_SEARCH_INFO, searchInfo)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            putParcelable(KEY_INLINE_SUGGESTION, compatInlineSuggestionsRequest)
                        }
                    })
                },
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
                } else {
                    PendingIntent.FLAG_CANCEL_CURRENT
                })
        }

        fun getPendingIntentForRegistration(context: Context,
                                            registerInfo: RegisterInfo): PendingIntent {
            return PendingIntent.getActivity(context, 0,
                Intent(context, AutofillLauncherActivity::class.java).apply {
                    EntrySelectionHelper.addSpecialModeInIntent(this, SpecialMode.REGISTRATION)
                    putExtra(KEY_REGISTER_INFO, registerInfo)
                },
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
                } else {
                    PendingIntent.FLAG_CANCEL_CURRENT
                })
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
