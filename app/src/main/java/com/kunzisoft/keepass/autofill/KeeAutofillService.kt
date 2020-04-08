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
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.model.SearchInfo

@RequiresApi(api = Build.VERSION_CODES.O)
class KeeAutofillService : AutofillService() {

    override fun onFillRequest(request: FillRequest,
                               cancellationSignal: CancellationSignal,
                               callback: FillCallback) {
        val fillContexts = request.fillContexts
        val latestStructure = fillContexts[fillContexts.size - 1].structure

        cancellationSignal.setOnCancelListener { Log.w(TAG, "Cancel autofill.") }

        // Check user's settings for authenticating Responses and Datasets.
        StructureParser(latestStructure).parse()?.let { parseResult ->

            val searchInfo = SearchInfo().apply {
                applicationId = parseResult.applicationId
                webDomain = parseResult.domain
            }

            AutofillHelper.checkAutoSearchInfo(this,
                    Database.getInstance(),
                    searchInfo,
                    { items ->
                        items.forEach {
                            val responseBuilder = FillResponse.Builder()
                            responseBuilder.addDataset(AutofillHelper.buildDataset(this, it, parseResult))
                            callback.onSuccess(responseBuilder.build())
                        }
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

    private fun showUIForEntrySelection(parseResult: StructureParser.Result,
                                        searchInfo: SearchInfo,
                                        callback: FillCallback) {
        parseResult.allAutofillIds().let { autofillIds ->
            if (autofillIds.isNotEmpty()) {
                // If the entire Autofill Response is authenticated, AuthActivity is used
                // to generate Response.
                val sender = AutofillLauncherActivity.getAuthIntentSenderForResponse(this,
                        searchInfo)
                val presentation = RemoteViews(packageName, R.layout.item_autofill_service_unlock)

                val responseBuilder = FillResponse.Builder()
                responseBuilder.setAuthentication(autofillIds, sender, presentation)
                callback.onSuccess(responseBuilder.build())
            }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        // TODO Save autofill
        //callback.onFailure(getString(R.string.autofill_not_support_save));
    }

    override fun onConnected() {
        Log.d(TAG, "onConnected")
    }

    override fun onDisconnected() {
        Log.d(TAG, "onDisconnected")
    }

    companion object {
        private val TAG = KeeAutofillService::class.java.name
    }
}
