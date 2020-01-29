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
import androidx.annotation.RequiresApi
import android.util.Log
import android.widget.RemoteViews
import com.kunzisoft.keepass.R

@RequiresApi(api = Build.VERSION_CODES.O)
class KeeAutofillService : AutofillService() {

    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal,
                               callback: FillCallback) {
        val fillContexts = request.fillContexts
        val latestStructure = fillContexts[fillContexts.size - 1].structure

        cancellationSignal.setOnCancelListener { Log.e(TAG, "Cancel autofill not implemented in this sample.") }

        val responseBuilder = FillResponse.Builder()
        // Check user's settings for authenticating Responses and Datasets.
        val parseResult = StructureParser(latestStructure).parse()
        parseResult?.allAutofillIds()?.let { autofillIds ->
            if (listOf(*autofillIds).isNotEmpty()) {
                // If the entire Autofill Response is authenticated, AuthActivity is used
                // to generate Response.
                val sender = AutofillLauncherActivity.getAuthIntentSenderForResponse(this)
                val presentation = RemoteViews(packageName, R.layout.item_autofill_service_unlock)
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
