/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.autofill

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import com.kunzisoft.keepass.activities.GroupActivity
import com.kunzisoft.keepass.app.App
import com.kunzisoft.keepass.fileselect.FileDatabaseSelectActivity
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.TimeoutHelper

@RequiresApi(api = Build.VERSION_CODES.O)
class AutoFillLauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Pass extra for Autofill (EXTRA_ASSIST_STRUCTURE)
        val assistStructure = AutofillHelper.retrieveAssistStructure(intent)
        if (assistStructure != null) {
            if (App.currentDatabase.loaded && TimeoutHelper.checkTime(this))
                GroupActivity.launchForAutofillResult(this, assistStructure, PreferencesUtil.enableReadOnlyDatabase(this))
            else {
                FileDatabaseSelectActivity.launchForAutofillResult(this, assistStructure)
            }
        } else {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        super.onCreate(savedInstanceState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        AutofillHelper.onActivityResultSetResultAndFinish(this, requestCode, resultCode, data)
    }

    companion object {

        fun getAuthIntentSenderForResponse(context: Context): IntentSender {
            val intent = Intent(context, AutoFillLauncherActivity::class.java)
            return PendingIntent.getActivity(context, 0,
                    intent, PendingIntent.FLAG_CANCEL_CURRENT).intentSender
        }
    }
}
