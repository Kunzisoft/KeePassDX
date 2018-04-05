/*
 * Copyright 2017 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.autofill;

import android.app.assist.AssistStructure;
import android.content.IntentSender;
import android.os.Build;
import android.os.CancellationSignal;
import android.service.autofill.AutofillService;
import android.service.autofill.FillCallback;
import android.service.autofill.FillContext;
import android.service.autofill.FillRequest;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveCallback;
import android.service.autofill.SaveRequest;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.autofill.AutofillId;
import android.widget.RemoteViews;

import com.kunzisoft.keepass.R;

import java.util.Arrays;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.O)
public class KeeAutofillService extends AutofillService {
    private static final String TAG = "KeeAutofillService";

    @Override
    public void onFillRequest(@NonNull FillRequest request, @NonNull CancellationSignal cancellationSignal,
                              @NonNull FillCallback callback) {
        List<FillContext> fillContexts = request.getFillContexts();
        AssistStructure latestStructure = fillContexts.get(fillContexts.size() - 1).getStructure();

        cancellationSignal.setOnCancelListener(() ->
                Log.e(TAG, "Cancel autofill not implemented in this sample.")
        );

        FillResponse.Builder responseBuilder = new FillResponse.Builder();
        // Check user's settings for authenticating Responses and Datasets.
        StructureParser.Result parseResult = new StructureParser(latestStructure).parse();
        AutofillId[] autofillIds = parseResult.allAutofillIds();
        if (!Arrays.asList(autofillIds).isEmpty()) {
            // If the entire Autofill Response is authenticated, AuthActivity is used
            // to generate Response.
            IntentSender sender = AutoFillAuthActivity.getAuthIntentSenderForResponse(this);
            RemoteViews presentation = new RemoteViews(getPackageName(), R.layout.autofill_service_unlock);
            responseBuilder.setAuthentication(autofillIds, sender, presentation);
            callback.onSuccess(responseBuilder.build());
        }
    }

    @Override
    public void onSaveRequest(@NonNull SaveRequest request, @NonNull SaveCallback callback) {
        // TODO Save autofill
        //callback.onFailure(getString(R.string.autofill_not_support_save));
    }

    @Override
    public void onConnected() {
        Log.d(TAG, "onConnected");
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "onDisconnected");
    }
}
