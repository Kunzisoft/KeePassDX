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

import android.app.Activity
import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.util.Log
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.model.EntryInfo


@RequiresApi(api = Build.VERSION_CODES.O)
object AutofillHelper {

    private const val AUTOFILL_RESPONSE_REQUEST_CODE = 8165

    private const val ASSIST_STRUCTURE = AutofillManager.EXTRA_ASSIST_STRUCTURE

    fun retrieveAssistStructure(intent: Intent?): AssistStructure? {
        intent?.let {
            return it.getParcelableExtra(ASSIST_STRUCTURE)
        }
        return null
    }

    private fun makeEntryTitle(entryInfo: EntryInfo): String {
        if (entryInfo.title.isNotEmpty() && entryInfo.username.isNotEmpty())
            return String.format("%s (%s)", entryInfo.title, entryInfo.username)
        if (entryInfo.title.isNotEmpty())
            return entryInfo.title
        if (entryInfo.url.isNotEmpty())
            return entryInfo.url
        if (entryInfo.username.isNotEmpty())
            return entryInfo.username
        return ""
    }

    private fun buildDataset(context: Context,
                             entryInfo: EntryInfo,
                             struct: StructureParser.Result): Dataset? {
        val title = makeEntryTitle(entryInfo)
        val views = newRemoteViews(context.packageName, title)
        val builder = Dataset.Builder(views)
        builder.setId(entryInfo.id)

        struct.usernameId?.let { usernameId ->
            builder.setValue(usernameId, AutofillValue.forText(entryInfo.username))
        }
        struct.passwordId?.let { password ->
            builder.setValue(password, AutofillValue.forText(entryInfo.password))
        }

        return try {
            builder.build()
        } catch (e: IllegalArgumentException) {
            // if not value be set
            null
        }
    }

    /**
     * Method to hit when right key is selected
     */
    fun buildResponseWhenEntrySelected(activity: Activity, entryInfo: EntryInfo) {
        var setResultOk = false
        activity.intent?.extras?.let { extras ->
            if (extras.containsKey(ASSIST_STRUCTURE)) {
                activity.intent?.getParcelableExtra<AssistStructure>(ASSIST_STRUCTURE)?.let { structure ->
                    StructureParser(structure).parse()?.let { result ->
                        // New Response
                        val responseBuilder = FillResponse.Builder()
                        val dataset = buildDataset(activity, entryInfo, result)
                        responseBuilder.addDataset(dataset)
                        val mReplyIntent = Intent()
                        Log.d(activity.javaClass.name, "Successed Autofill auth.")
                        mReplyIntent.putExtra(
                                AutofillManager.EXTRA_AUTHENTICATION_RESULT,
                                responseBuilder.build())
                        setResultOk = true
                        activity.setResult(Activity.RESULT_OK, mReplyIntent)
                    }
                }
            }
            if (!setResultOk) {
                Log.w(activity.javaClass.name, "Failed Autofill auth.")
                activity.setResult(Activity.RESULT_CANCELED)
            }
        }
    }

    /**
     * Utility method to start an activity with an Autofill for result
     */
    fun startActivityForAutofillResult(activity: Activity, intent: Intent, assistStructure: AssistStructure) {
        EntrySelectionHelper.addEntrySelectionModeExtraInIntent(intent)
        intent.putExtra(ASSIST_STRUCTURE, assistStructure)
        activity.startActivityForResult(intent, AUTOFILL_RESPONSE_REQUEST_CODE)
    }

    /**
     * Utility method to loop and close each activity with return data
     */
    fun onActivityResultSetResultAndFinish(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == AUTOFILL_RESPONSE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                activity.setResult(resultCode, data)
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                activity.setResult(Activity.RESULT_CANCELED)
            }
            activity.finish()
        }
    }

    private fun newRemoteViews(packageName: String, remoteViewsText: String): RemoteViews {
        val presentation = RemoteViews(packageName, R.layout.item_autofill_service)
        presentation.setTextViewText(R.id.text, remoteViewsText)
        return presentation
    }
}
