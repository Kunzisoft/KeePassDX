/*
 * Copyright 2017 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass Libre.
 *
 *  KeePass Libre is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass Libre is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass Libre.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.autofill;

import android.app.PendingIntent;
import android.app.assist.AssistStructure;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.keepassdroid.fileselect.FileSelectActivity;
import tech.jgross.keepass.KeePass;

@RequiresApi(api = Build.VERSION_CODES.O)
public class AutoFillAuthActivity extends KeePass {

    private AutofillHelper autofillHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        autofillHelper = new AutofillHelper();
        super.onCreate(savedInstanceState);
    }

    public static IntentSender getAuthIntentSenderForResponse(Context context) {
        final Intent intent = new Intent(context, AutoFillAuthActivity.class);
        return PendingIntent.getActivity(context, 0,
                intent, PendingIntent.FLAG_CANCEL_CURRENT).getIntentSender();
    }

    @Override
    protected void startFileSelectActivity() {
        // Pass extra for Autofill (EXTRA_ASSIST_STRUCTURE)
        AssistStructure assistStructure = autofillHelper.retrieveAssistStructure(getIntent());
        if (assistStructure != null) {
            FileSelectActivity.launch(this, assistStructure);
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        AutofillHelper.onActivityResultSetResultAndFinish(this, requestCode, resultCode, data);
    }
}
