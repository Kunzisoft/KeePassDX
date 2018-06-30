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
package com.kunzisoft.keepass.autofill;

import android.app.PendingIntent;
import android.app.assist.AssistStructure;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;

import com.kunzisoft.keepass.fileselect.FileSelectActivity;

@RequiresApi(api = Build.VERSION_CODES.O)
public class AutoFillAuthActivity extends AppCompatActivity {

    private AutofillHelper autofillHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        autofillHelper = new AutofillHelper();
        startFileSelectActivity();
        super.onCreate(savedInstanceState);
    }

    public static IntentSender getAuthIntentSenderForResponse(Context context) {
        final Intent intent = new Intent(context, AutoFillAuthActivity.class);
        return PendingIntent.getActivity(context, 0,
                intent, PendingIntent.FLAG_CANCEL_CURRENT).getIntentSender();
    }
    
    protected void startFileSelectActivity() {
        // Pass extra for Autofill (EXTRA_ASSIST_STRUCTURE)
        AssistStructure assistStructure = autofillHelper.retrieveAssistStructure(getIntent());
        if (assistStructure != null) {
            FileSelectActivity.launchForAutofillResult(this, assistStructure);
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
