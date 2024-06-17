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

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.graphics.BlendMode
import android.graphics.drawable.Icon
import android.os.Build
import android.service.autofill.Dataset
import android.service.autofill.Field
import android.service.autofill.FillResponse
import android.service.autofill.InlinePresentation
import android.service.autofill.Presentations
import android.util.Log
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import android.widget.Toast
import android.widget.inline.InlinePresentationSpec
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.autofill.inline.UiVersions
import androidx.autofill.inline.v1.InlineSuggestionUi
import androidx.core.content.ContextCompat
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.AutofillLauncherActivity
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.activities.helpers.SpecialMode
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.template.TemplateField
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.settings.AutofillSettingsActivity
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.LOCK_ACTION
import com.kunzisoft.keepass.utils.getParcelableExtraCompat
import kotlin.math.min


@RequiresApi(api = Build.VERSION_CODES.O)
object AutofillHelper {

    private const val EXTRA_ASSIST_STRUCTURE = AutofillManager.EXTRA_ASSIST_STRUCTURE
    private const val EXTRA_INLINE_SUGGESTIONS_REQUEST = "com.kunzisoft.keepass.autofill.INLINE_SUGGESTIONS_REQUEST"

    fun retrieveAutofillComponent(intent: Intent?): AutofillComponent? {
        intent?.getParcelableExtraCompat<AssistStructure>(EXTRA_ASSIST_STRUCTURE)?.let { assistStructure ->
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                AutofillComponent(assistStructure,
                        intent.getParcelableExtraCompat(EXTRA_INLINE_SUGGESTIONS_REQUEST))
            } else {
                AutofillComponent(assistStructure, null)
            }
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

    private fun newRemoteViews(context: Context,
                               database: ContextualDatabase,
                               remoteViewsText: String,
                               remoteViewsIcon: IconImage? = null): RemoteViews {
        val remoteViews = RemoteViews(context.packageName, R.layout.item_autofill_entry)
        remoteViews.setTextViewText(R.id.autofill_entry_text, remoteViewsText)
        if (remoteViewsIcon != null) {
            try {
                database.iconDrawableFactory.getBitmapFromIcon(context,
                        remoteViewsIcon, ContextCompat.getColor(context, R.color.green))?.let { bitmap ->
                    remoteViews.setImageViewBitmap(R.id.autofill_entry_icon, bitmap)
                }
            } catch (e: Exception) {
                Log.e(RemoteViews::class.java.name, "Unable to assign icon in remote view", e)
            }
        }
        return remoteViews
    }

    private fun Dataset.Builder.addValueToDatasetBuilder(
        id: AutofillId,
        autofillValue: AutofillValue?
    ): Dataset.Builder {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setField(
                id, autofillValue?.let {
                    Field.Builder()
                        .setValue(it)
                        .build()
                }
            )
        } else {
            @Suppress("DEPRECATION")
            setValue(id, autofillValue)
        }
        Log.d(TAG, "Set Autofill value $autofillValue for id $id")
        return this
    }

    private fun buildDatasetForEntry(context: Context,
                                     database: ContextualDatabase,
                                     entryInfo: EntryInfo,
                                     struct: StructureParser.Result,
                                     inlinePresentation: InlinePresentation?): Dataset {
        val remoteViews: RemoteViews = newRemoteViews(context, database, makeEntryTitle(entryInfo), entryInfo.icon)

        val datasetBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Dataset.Builder(Presentations.Builder()
                .apply {
                    inlinePresentation?.let {
                        setInlinePresentation(inlinePresentation)
                    }
                }
                .setDialogPresentation(remoteViews)
                .setMenuPresentation(remoteViews)
                .build())
        } else {
            @Suppress("DEPRECATION")
            Dataset.Builder(remoteViews).apply {
                inlinePresentation?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        setInlinePresentation(inlinePresentation)
                    }
                }
            }
        }

        datasetBuilder.setId(entryInfo.id.toString())

        struct.usernameId?.let { usernameId ->
            datasetBuilder.addValueToDatasetBuilder(
                usernameId,
                AutofillValue.forText(entryInfo.username)
            )
        }
        struct.passwordId?.let { passwordId ->
            datasetBuilder.addValueToDatasetBuilder(
                passwordId,
                AutofillValue.forText(entryInfo.password)
            )
        }

        if (entryInfo.expires) {
            val year = entryInfo.expiryTime.getYearInt()
            val month = entryInfo.expiryTime.getMonthInt()
            val monthString = month.toString().padStart(2, '0')
            val day = entryInfo.expiryTime.getDay()
            val dayString = day.toString().padStart(2, '0')

            struct.creditCardExpirationDateId?.let {
                if (struct.isWebView) {
                    // set date string as defined in https://html.spec.whatwg.org
                    datasetBuilder.addValueToDatasetBuilder(
                        it,
                        AutofillValue.forText("$year\u002D$monthString")
                    )
                } else {
                    datasetBuilder.addValueToDatasetBuilder(
                        it,
                        AutofillValue.forDate(entryInfo.expiryTime.date.time)
                    )
                }
            }
            struct.creditCardExpirationYearId?.let {
                var autofillValue: AutofillValue? = null

                struct.creditCardExpirationYearOptions?.let { options ->
                    var yearIndex = options.indexOf(year.toString().substring(0, 2))

                    if (yearIndex == -1) {
                        yearIndex = options.indexOf(year.toString())
                    }
                    if (yearIndex != -1) {
                        autofillValue = AutofillValue.forList(yearIndex)
                        datasetBuilder.addValueToDatasetBuilder(
                            it,
                            autofillValue
                        )
                    }
                }

                if (autofillValue == null) {
                    datasetBuilder.addValueToDatasetBuilder(
                        it,
                        AutofillValue.forText(year.toString())
                    )
                }
            }
            struct.creditCardExpirationMonthId?.let {
                if (struct.isWebView) {
                    datasetBuilder.addValueToDatasetBuilder(
                        it,
                        AutofillValue.forText(monthString)
                    )
                } else {
                    if (struct.creditCardExpirationMonthOptions != null) {
                        // index starts at 0
                        datasetBuilder.addValueToDatasetBuilder(
                            it,
                            AutofillValue.forList(month - 1)
                        )
                    } else {
                        datasetBuilder.addValueToDatasetBuilder(
                            it,
                            AutofillValue.forText(monthString)
                        )
                    }
                }
            }
            struct.creditCardExpirationDayId?.let {
                if (struct.isWebView) {
                    datasetBuilder.addValueToDatasetBuilder(
                        it,
                        AutofillValue.forText(dayString)
                    )
                } else {
                    if (struct.creditCardExpirationDayOptions != null) {
                        datasetBuilder.addValueToDatasetBuilder(
                            it,
                            AutofillValue.forList(day - 1)
                        )
                    } else {
                        datasetBuilder.addValueToDatasetBuilder(
                            it,
                            AutofillValue.forText(dayString)
                        )
                    }
                }
            }
        }
        for (field in entryInfo.customFields) {
            if (field.name == TemplateField.LABEL_HOLDER) {
                struct.creditCardHolderId?.let { ccNameId ->
                    datasetBuilder.addValueToDatasetBuilder(
                        ccNameId,
                        AutofillValue.forText(field.protectedValue.stringValue)
                    )
                }
            }
            if (field.name == TemplateField.LABEL_NUMBER) {
                struct.creditCardNumberId?.let { ccnId ->
                    datasetBuilder.addValueToDatasetBuilder(
                        ccnId,
                        AutofillValue.forText(field.protectedValue.stringValue)
                    )
                }
            }
            if (field.name == TemplateField.LABEL_CVV) {
                struct.cardVerificationValueId?.let { cvvId ->
                    datasetBuilder.addValueToDatasetBuilder(
                        cvvId,
                        AutofillValue.forText(field.protectedValue.stringValue)
                    )
                }
            }
        }
        val dataset = datasetBuilder.build()
        Log.d(TAG, "Autofill Dataset $dataset created")
        return dataset
    }

    /**
     * Method to assign a drawable to a new icon from a database icon
     */
    private fun buildIconFromEntry(context: Context,
                                   database: ContextualDatabase,
                                   entryInfo: EntryInfo): Icon? {
        try {
            database.iconDrawableFactory.getBitmapFromIcon(context,
                    entryInfo.icon, ContextCompat.getColor(context, R.color.green))?.let { bitmap ->
                return Icon.createWithBitmap(bitmap)
            }
        } catch (e: Exception) {
            Log.e(RemoteViews::class.java.name, "Unable to assign icon in remote view", e)
        }
        return null
    }

    @SuppressLint("RestrictedApi")
    @RequiresApi(Build.VERSION_CODES.R)
    private fun buildInlinePresentationForEntry(context: Context,
                                                database: ContextualDatabase,
                                                compatInlineSuggestionsRequest: CompatInlineSuggestionsRequest,
                                                positionItem: Int,
                                                entryInfo: EntryInfo): InlinePresentation? {
        compatInlineSuggestionsRequest.inlineSuggestionsRequest?.let { inlineSuggestionsRequest ->
            val inlinePresentationSpecs = inlineSuggestionsRequest.inlinePresentationSpecs
            val maxSuggestion = inlineSuggestionsRequest.maxSuggestionCount

            if (positionItem <= maxSuggestion - 1) {

                // If positionItem is larger than the number of specs in the list, then
                // the last spec is used for the remainder of the suggestions
                val inlinePresentationSpec = inlinePresentationSpecs[min(positionItem, inlinePresentationSpecs.size - 1)]

                // Make sure that the IME spec claims support for v1 UI template.
                val imeStyle = inlinePresentationSpec.style
                if (!UiVersions.getVersions(imeStyle).contains(UiVersions.INLINE_UI_VERSION_1))
                    return null

                // Build the content for IME UI
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, AutofillSettingsActivity::class.java),
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        PendingIntent.FLAG_IMMUTABLE
                    } else {
                        0
                    }
                )
                return InlinePresentation(
                    InlineSuggestionUi.newContentBuilder(pendingIntent).apply {
                        setContentDescription(context.getString(R.string.autofill_sign_in_prompt))
                        setTitle(entryInfo.title)
                        setSubtitle(entryInfo.username)
                        setStartIcon(
                            Icon.createWithResource(context, R.mipmap.ic_launcher_round).apply {
                                setTintBlendMode(BlendMode.DST)
                            })
                        buildIconFromEntry(context, database, entryInfo)?.let { icon ->
                            setEndIcon(icon.apply {
                                setTintBlendMode(BlendMode.DST)
                            })
                        }
                    }.build().slice, inlinePresentationSpec, false
                )
            }
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("RestrictedApi")
    private fun buildInlinePresentationForManualSelection(context: Context,
                                                          inlinePresentationSpec: InlinePresentationSpec,
                                                          pendingIntent: PendingIntent): InlinePresentation? {
        // Make sure that the IME spec claims support for v1 UI template.
        val imeStyle = inlinePresentationSpec.style
        if (!UiVersions.getVersions(imeStyle).contains(UiVersions.INLINE_UI_VERSION_1))
            return null

        // Build the content for IME UI
        return InlinePresentation(
                InlineSuggestionUi.newContentBuilder(pendingIntent).apply {
                    setContentDescription(context.getString(R.string.autofill_sign_in_prompt))
                    setTitle(context.getString(R.string.autofill_select_entry))
                    setStartIcon(Icon.createWithResource(context, R.drawable.ic_arrow_right_green_24dp).apply {
                        setTintBlendMode(BlendMode.DST)
                    })
                }.build().slice, inlinePresentationSpec, false)
    }

    fun buildResponse(context: Context,
                      database: ContextualDatabase,
                      entriesInfo: List<EntryInfo>,
                      parseResult: StructureParser.Result,
                      compatInlineSuggestionsRequest: CompatInlineSuggestionsRequest?): FillResponse? {
        val responseBuilder = FillResponse.Builder()
        // Add Header
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val packageName = context.packageName
            parseResult.webDomain?.let { webDomain ->
                responseBuilder.setHeader(RemoteViews(packageName, R.layout.item_autofill_web_domain).apply {
                    setTextViewText(R.id.autofill_web_domain_text, webDomain)
                })
            } ?: kotlin.run {
                parseResult.applicationId?.let { applicationId ->
                    responseBuilder.setHeader(RemoteViews(packageName, R.layout.item_autofill_app_id).apply {
                        setTextViewText(R.id.autofill_app_id_text, applicationId)
                    })
                }
            }
        }

        // Add inline suggestion for new IME and dataset
        var numberInlineSuggestions = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            compatInlineSuggestionsRequest?.inlineSuggestionsRequest?.let { inlineSuggestionsRequest ->
                numberInlineSuggestions = minOf(inlineSuggestionsRequest.maxSuggestionCount, entriesInfo.size)
                if (PreferencesUtil.isAutofillManualSelectionEnable(context)) {
                    if (entriesInfo.size >= inlineSuggestionsRequest.maxSuggestionCount) {
                        --numberInlineSuggestions
                    }
                }
            }
        }

        entriesInfo.forEachIndexed { _, entry ->
            try {
                // Build inline presentation for compatible keyboard
                var inlinePresentation: InlinePresentation? = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                    && numberInlineSuggestions > 0
                    && compatInlineSuggestionsRequest != null) {
                    inlinePresentation = buildInlinePresentationForEntry(
                        context,
                        database,
                        compatInlineSuggestionsRequest,
                        numberInlineSuggestions--,
                        entry
                    )
                }
                // Create dataset for each entry
                responseBuilder.addDataset(
                    buildDatasetForEntry(context, database, entry, parseResult, inlinePresentation)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unable to add dataset")
            }
        }

        // Add a new dataset for manual selection
        if (PreferencesUtil.isAutofillManualSelectionEnable(context)) {
            val searchInfo = SearchInfo().apply {
                applicationId = parseResult.applicationId
                webDomain = parseResult.webDomain
                webScheme = parseResult.webScheme
                manualSelection = true
            }
            val manualSelectionView = RemoteViews(context.packageName, R.layout.item_autofill_select_entry)
            AutofillLauncherActivity.getPendingIntentForSelection(context,
                    searchInfo, compatInlineSuggestionsRequest)?.let { pendingIntent ->

                var inlinePresentation: InlinePresentation? = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    compatInlineSuggestionsRequest?.inlineSuggestionsRequest?.let { inlineSuggestionsRequest ->
                        val inlinePresentationSpec =
                            inlineSuggestionsRequest.inlinePresentationSpecs[0]
                        inlinePresentation = buildInlinePresentationForManualSelection(
                            context,
                            inlinePresentationSpec,
                            pendingIntent
                        )
                    }
                }

                val datasetBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Dataset.Builder(Presentations.Builder()
                        .apply {
                            inlinePresentation?.let {
                                setInlinePresentation(it)
                            }
                        }
                        .setDialogPresentation(manualSelectionView)
                        .setMenuPresentation(manualSelectionView)
                        .build())
                } else {
                    @Suppress("DEPRECATION")
                    Dataset.Builder(manualSelectionView).apply {
                        inlinePresentation?.let {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                setInlinePresentation(it)
                            }
                        }
                    }
                }

                parseResult.allAutofillIds().let { autofillIds ->
                    autofillIds.forEach { id ->
                        datasetBuilder.addValueToDatasetBuilder(id, null)
                        datasetBuilder.setAuthentication(pendingIntent.intentSender)
                    }
                    val dataset = datasetBuilder.build()
                    Log.d(TAG, "Autofill Dataset for manual selection $dataset created")
                    responseBuilder.addDataset(dataset)
                }
            }
        }

        return try {
            responseBuilder.build()
        } catch (e: Exception) {
            Log.e(TAG, "Unable to create Autofill response", e)
            null
        }
    }

    /**
     * Build the Autofill response for one entry
     */
    fun buildResponseAndSetResult(activity: Activity,
                                  database: ContextualDatabase,
                                  entryInfo: EntryInfo) {
        buildResponseAndSetResult(activity, database, ArrayList<EntryInfo>().apply { add(entryInfo) })
    }

    /**
     * Build the Autofill response for many entry
     */
    fun buildResponseAndSetResult(activity: Activity,
                                  database: ContextualDatabase,
                                  entriesInfo: List<EntryInfo>) {
        if (entriesInfo.isEmpty()) {
            activity.setResult(Activity.RESULT_CANCELED)
        } else {
            var setResultOk = false
            activity.intent?.getParcelableExtraCompat<AssistStructure>(EXTRA_ASSIST_STRUCTURE)?.let { structure ->
                StructureParser(structure).parse()?.let { result ->
                    // New Response
                    val response = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val compatInlineSuggestionsRequest = activity.intent?.getParcelableExtraCompat<CompatInlineSuggestionsRequest>(EXTRA_INLINE_SUGGESTIONS_REQUEST)
                        if (compatInlineSuggestionsRequest != null) {
                            Toast.makeText(activity.applicationContext, R.string.autofill_inline_suggestions_keyboard, Toast.LENGTH_SHORT).show()
                        }
                        buildResponse(activity, database, entriesInfo, result, compatInlineSuggestionsRequest)
                    } else {
                        buildResponse(activity, database, entriesInfo, result, null)
                    }
                    val mReplyIntent = Intent()
                    Log.d(activity.javaClass.name, "Success Autofill auth.")
                    mReplyIntent.putExtra(
                            AutofillManager.EXTRA_AUTHENTICATION_RESULT,
                            response)
                    setResultOk = true
                    activity.setResult(Activity.RESULT_OK, mReplyIntent)
                }
            }
            if (!setResultOk) {
                Log.w(activity.javaClass.name, "Failed Autofill auth.")
                activity.setResult(Activity.RESULT_CANCELED)
            }
        }
    }

    fun buildActivityResultLauncher(activity: AppCompatActivity,
                                    lockDatabase: Boolean = false): ActivityResultLauncher<Intent> {
        return activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            // Utility method to loop and close each activity with return data
            if (it.resultCode == Activity.RESULT_OK) {
                activity.setResult(it.resultCode, it.data)
            }
            if (it.resultCode == Activity.RESULT_CANCELED) {
                activity.setResult(Activity.RESULT_CANCELED)
            }
            activity.finish()

            if (lockDatabase && PreferencesUtil.isAutofillCloseDatabaseEnable(activity)) {
                // Close the database
                activity.sendBroadcast(Intent(LOCK_ACTION))
            }
        }
    }

    /**
     * Utility method to start an activity with an Autofill for result
     */
    fun startActivityForAutofillResult(activity: AppCompatActivity,
                                       intent: Intent,
                                       activityResultLauncher: ActivityResultLauncher<Intent>?,
                                       autofillComponent: AutofillComponent,
                                       searchInfo: SearchInfo?) {
        EntrySelectionHelper.addSpecialModeInIntent(intent, SpecialMode.SELECTION)
        intent.putExtra(EXTRA_ASSIST_STRUCTURE, autofillComponent.assistStructure)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && PreferencesUtil.isAutofillInlineSuggestionsEnable(activity)) {
            autofillComponent.compatInlineSuggestionsRequest?.let {
                intent.putExtra(EXTRA_INLINE_SUGGESTIONS_REQUEST, it)
            }
        }
        EntrySelectionHelper.addSearchInfoInIntent(intent, searchInfo)
        activityResultLauncher?.launch(intent)
    }

    private val TAG = AutofillHelper::class.java.name
}
