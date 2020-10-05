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

import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.*
import android.util.Log
import android.view.autofill.AutofillId
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.AutofillLauncherActivity
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.search.SearchHelper
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.settings.PreferencesUtil

@RequiresApi(api = Build.VERSION_CODES.O)
class KeeAutofillService : AutofillService() {

    var applicationIdBlocklist: Set<String>? = null
    var webDomainBlocklist: Set<String>? = null

    override fun onCreate() {
        super.onCreate()

        applicationIdBlocklist = PreferencesUtil.applicationIdBlocklist(this)
        webDomainBlocklist = PreferencesUtil.webDomainBlocklist(this)
    }

    override fun onFillRequest(request: FillRequest,
                               cancellationSignal: CancellationSignal,
                               callback: FillCallback) {
        val fillContexts = request.fillContexts
        val latestStructure = fillContexts[fillContexts.size - 1].structure

        cancellationSignal.setOnCancelListener { Log.w(TAG, "Cancel autofill.") }

        // Check user's settings for authenticating Responses and Datasets.
        StructureParser(latestStructure).parse()?.let { parseResult ->

            // Build search info only if applicationId or webDomain are not blocked
            if (searchAllowedFor(parseResult.applicationId, applicationIdBlocklist)
                    && searchAllowedFor(parseResult.domain, webDomainBlocklist)) {
                val searchInfo = SearchInfo().apply {
                    applicationId = parseResult.applicationId
                    webDomain = parseResult.domain
                }

                SearchHelper.checkAutoSearchInfo(this,
                        Database.getInstance(),
                        searchInfo,
                        { items ->
                            val responseBuilder = FillResponse.Builder()
                            AutofillHelper.addHeader(responseBuilder, packageName,
                                    parseResult.domain, parseResult.applicationId)
                            items.forEach {
                                responseBuilder.addDataset(AutofillHelper.buildDataset(this, it, parseResult))
                            }
                            callback.onSuccess(responseBuilder.build())
                        },
                        {
                            // Show UI if no search result
                            showUIForEntrySelection(parseResult, searchInfo, callback)
                        },
                        {
                            // Show UI if database not open
                            showUIForEntrySelection(parseResult, searchInfo, callback)
                        }
                )
            }
        }
    }

    private fun showUIForEntrySelection(parseResult: StructureParser.Result,
                                        searchInfo: SearchInfo,
                                        callback: FillCallback) {
        parseResult.allAutofillIds().let { autofillIds ->
            if (autofillIds.isNotEmpty()) {
                // If the entire Autofill Response is authenticated, AuthActivity is used
                // to generate Response.
                val sender = AutofillLauncherActivity.getAuthIntentSenderForResponse(this,
                        searchInfo)
                val responseBuilder = FillResponse.Builder()
                val remoteViewsUnlock: RemoteViews = if (!parseResult.domain.isNullOrEmpty()) {
                    RemoteViews(packageName, R.layout.item_autofill_unlock_web_domain).apply {
                        setTextViewText(R.id.autofill_web_domain_text, parseResult.domain)
                    }
                } else if (!parseResult.applicationId.isNullOrEmpty()) {
                    RemoteViews(packageName, R.layout.item_autofill_unlock_app_id).apply {
                        setTextViewText(R.id.autofill_app_id_text, parseResult.applicationId)
                    }
                } else {
                    RemoteViews(packageName, R.layout.item_autofill_unlock)
                }
                // Tell to service the interest to save credentials
                var types: Int = SaveInfo.SAVE_DATA_TYPE_GENERIC
                val info = ArrayList<AutofillId>()
                // Only if at least a password
                parseResult.passwordId?.let { passwordInfo ->
                    parseResult.usernameId?.let { usernameInfo ->
                        types = types or SaveInfo.SAVE_DATA_TYPE_USERNAME
                        info.add(usernameInfo)
                    }
                    types = types or SaveInfo.SAVE_DATA_TYPE_PASSWORD
                    info.add(passwordInfo)
                }
                if (info.isNotEmpty()) {
                    responseBuilder.setSaveInfo(
                            SaveInfo.Builder(types, info.toTypedArray()).build()
                    )
                }
                // Build response
                responseBuilder.setAuthentication(autofillIds, sender, remoteViewsUnlock)
                callback.onSuccess(responseBuilder.build())
            }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val fillContexts = request.fillContexts
        val latestStructure = fillContexts[fillContexts.size - 1].structure

        StructureParser(latestStructure, true).parse()?.let { parseResult ->
            parseResult.passwordValue?.let { autofillPasswordValue ->
                Log.d(TAG, "autofill onSaveRequest password ${autofillPasswordValue.textValue}")
                callback.onSuccess()
                return
            }
        }
        callback.onFailure("Unable to save values from form")
    }

    override fun onConnected() {
        Log.d(TAG, "onConnected")
    }

    override fun onDisconnected() {
        Log.d(TAG, "onDisconnected")
    }

    companion object {
        private val TAG = KeeAutofillService::class.java.name

        fun searchAllowedFor(element: String?, blockList: Set<String>?): Boolean {
            element?.let { elementNotNull ->
                if (blockList?.any { appIdBlocked ->
                            elementNotNull.contains(appIdBlocked)
                        } == true
                ) {
                    Log.d(TAG, "Autofill not allowed for $elementNotNull")
                    return false
                }
            }
            return true
        }
    }
}
