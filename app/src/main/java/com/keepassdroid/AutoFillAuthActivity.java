/*
 * Copyright 2017 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
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
package com.keepassdroid;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.keepassdroid.fileselect.FileSelectActivity;
import com.kunzisoft.keepass.KeePass;

import static com.keepassdroid.PasswordActivity.KEY_AUTOFILL_RESPONSE;

@RequiresApi(api = Build.VERSION_CODES.O)
public class AutoFillAuthActivity extends KeePass {

    public static IntentSender getAuthIntentSenderForResponse(Context context) {
        final Intent intent = new Intent(context, AutoFillAuthActivity.class);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
                .getIntentSender();
    }

    protected void startFileSelectActivity() {
        Intent intent = new Intent(this, FileSelectActivity.class);
        intent.putExtra(KEY_AUTOFILL_RESPONSE, true);
        startActivityForResult(intent, 0);
    }
}
