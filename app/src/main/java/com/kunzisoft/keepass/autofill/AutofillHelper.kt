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
import androidx.core.content.ContextCompat
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.activities.helpers.SpecialMode
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.icons.assignDatabaseIcon
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.SearchInfo


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

    internal fun addHeader(responseBuilder: FillResponse.Builder,
                           packageName: String,
                           webDomain: String?,
                           applicationId: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (webDomain != null) {
                responseBuilder.setHeader(RemoteViews(packageName, R.layout.item_autofill_web_domain).apply {
                    setTextViewText(R.id.autofill_web_domain_text, webDomain)
                })
            } else if (applicationId != null) {
                responseBuilder.setHeader(RemoteViews(packageName, R.layout.item_autofill_app_id).apply {
                    setTextViewText(R.id.autofill_app_id_text, applicationId)
                })
            }
        }
    }

    internal fun buildDataset(context: Context,
                              entryInfo: EntryInfo,
                              struct: StructureParser.Result): Dataset? {
        val title = makeEntryTitle(entryInfo)
        val views = newRemoteViews(context, title, entryInfo.icon)
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
     * Build the Autofill response for one entry
     */
    fun buildResponse(activity: Activity, entryInfo: EntryInfo) {
        buildResponse(activity, ArrayList<EntryInfo>().apply { add(entryInfo) })
    }

    /**
     * Build the Autofill response for many entry
     */
    fun buildResponse(activity: Activity, entriesInfo: List<EntryInfo>) {
        if (entriesInfo.isEmpty()) {
            activity.setResult(Activity.RESULT_CANCELED)
        } else {
            var setResultOk = false
            activity.intent?.extras?.let { extras ->
                if (extras.containsKey(ASSIST_STRUCTURE)) {
                    activity.intent?.getParcelableExtra<AssistStructure>(ASSIST_STRUCTURE)?.let { structure ->
                        StructureParser(structure).parse()?.let { result ->
                            // New Response
                            val responseBuilder = FillResponse.Builder()
                            entriesInfo.forEach {
                                responseBuilder.addDataset(buildDataset(activity, it, result))
                            }
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
    }

    /**
     * Utility method to start an activity with an Autofill for result
     */
    fun startActivityForAutofillResult(activity: Activity,
                                       intent: Intent,
                                       assistStructure: AssistStructure,
                                       searchInfo: SearchInfo?) {
        EntrySelectionHelper.addSpecialModeInIntent(intent, SpecialMode.SELECTION)
        intent.putExtra(ASSIST_STRUCTURE, assistStructure)
        EntrySelectionHelper.addSearchInfoInIntent(intent, searchInfo)
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

    private fun newRemoteViews(context: Context,
                               remoteViewsText: String,
                               remoteViewsIcon: IconImage? = null): RemoteViews {
        val presentation = RemoteViews(context.packageName, R.layout.item_autofill_entry)
        presentation.setTextViewText(R.id.autofill_entry_text, remoteViewsText)
        if (remoteViewsIcon != null) {
            presentation.assignDatabaseIcon(context,
                    R.id.autofill_entry_icon,
                    Database.getInstance().drawFactory,
                    remoteViewsIcon,
                    ContextCompat.getColor(context, R.color.green))
        }
        return presentation
    }
}
