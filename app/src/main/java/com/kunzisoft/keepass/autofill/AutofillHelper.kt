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
import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.support.annotation.RequiresApi
import android.util.Log
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.EntrySelectionHelper
import com.kunzisoft.keepass.database.element.EntryVersioned
import java.util.*


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

    private fun makeEntryTitle(entry: EntryVersioned): String {
        if (!entry.title.isEmpty() && !entry.username.isEmpty())
            return String.format("%s (%s)", entry.title, entry.username)
        if (!entry.title.isEmpty())
            return entry.title
        if (!entry.username.isEmpty())
            return entry.username
        return if (!entry.notes.isEmpty()) entry.notes.trim { it <= ' ' } else ""
        // TODO No title
    }

    private fun buildDataset(context: Context,
                             entry: EntryVersioned,
                             struct: StructureParser.Result): Dataset? {
        val title = makeEntryTitle(entry)
        val views = newRemoteViews(context.packageName, title)
        val builder = Dataset.Builder(views)
        builder.setId(entry.nodeId.toString())

        struct.password.forEach { id -> builder.setValue(id, AutofillValue.forText(entry.password)) }

        val ids = ArrayList(struct.username)
        if (entry.username.contains("@") || struct.username.isEmpty())
            ids.addAll(struct.email)
        ids.forEach { id -> builder.setValue(id, AutofillValue.forText(entry.username)) }

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
    fun buildResponseWhenEntrySelected(activity: Activity, entry: EntryVersioned) {
        val mReplyIntent: Intent
        activity.intent?.let { intent ->
            if (intent.extras.containsKey(ASSIST_STRUCTURE)) {
                val structure = intent.getParcelableExtra<AssistStructure>(ASSIST_STRUCTURE)
                val result = StructureParser(structure).parse()

                // New Response
                val responseBuilder = FillResponse.Builder()
                val dataset = buildDataset(activity, entry, result)
                responseBuilder.addDataset(dataset)
                mReplyIntent = Intent()
                Log.d(activity.javaClass.name, "Successed Autofill auth.")
                mReplyIntent.putExtra(
                        AutofillManager.EXTRA_AUTHENTICATION_RESULT,
                        responseBuilder.build())
                activity.setResult(Activity.RESULT_OK, mReplyIntent)
            } else {
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
        activity.startActivityForResult(intent, AutofillHelper.AUTOFILL_RESPONSE_REQUEST_CODE)
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
        val presentation = RemoteViews(packageName, R.layout.autofill_service_list_item)
        presentation.setTextViewText(R.id.text, remoteViewsText)
        return presentation
    }
}
