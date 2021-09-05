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
import android.service.autofill.FillResponse
import android.service.autofill.InlinePresentation
import android.util.Log
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.view.inputmethod.InlineSuggestionsRequest
import android.widget.RemoteViews
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.autofill.inline.UiVersions
import androidx.autofill.inline.v1.InlineSuggestionUi
import androidx.core.content.ContextCompat
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.AutofillLauncherActivity
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.activities.helpers.SpecialMode
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.OtpModel
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.database.element.template.TemplateField
import com.kunzisoft.keepass.otp.OtpTokenType
import com.kunzisoft.keepass.otp.OtpType
import com.kunzisoft.keepass.otp.TokenCalculator
import com.kunzisoft.keepass.settings.AutofillSettingsActivity
import com.kunzisoft.keepass.settings.PreferencesUtil
import kotlin.collections.ArrayList


@RequiresApi(api = Build.VERSION_CODES.O)
object AutofillHelper {

    private const val AUTOFILL_RESPONSE_REQUEST_CODE = 8165

    private const val EXTRA_ASSIST_STRUCTURE = AutofillManager.EXTRA_ASSIST_STRUCTURE
    const val EXTRA_INLINE_SUGGESTIONS_REQUEST = "com.kunzisoft.keepass.autofill.INLINE_SUGGESTIONS_REQUEST"

    fun retrieveAutofillComponent(intent: Intent?): AutofillComponent? {
        intent?.getParcelableExtra<AssistStructure?>(EXTRA_ASSIST_STRUCTURE)?.let { assistStructure ->
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                AutofillComponent(assistStructure,
                        intent.getParcelableExtra(EXTRA_INLINE_SUGGESTIONS_REQUEST))
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

    private fun makeOTPEntryTitle(entryInfo: EntryInfo): String {
        if (entryInfo.username.isNotEmpty())
            return String.format("OTP (%s)", entryInfo.username)
        if (entryInfo.title.isNotEmpty())
            return String.format("OTP (%s)", entryInfo.title)
        return "OTP"
    }

    private fun newRemoteViews(context: Context,
                               database: Database,
                               remoteViewsText: String,
                               remoteViewsIcon: IconImage? = null): RemoteViews {
        val presentation = RemoteViews(context.packageName, R.layout.item_autofill_entry)
        presentation.setTextViewText(R.id.autofill_entry_text, remoteViewsText)
        if (remoteViewsIcon != null) {
            try {
                database.iconDrawableFactory.getBitmapFromIcon(context,
                        remoteViewsIcon, ContextCompat.getColor(context, R.color.green))?.let { bitmap ->
                    presentation.setImageViewBitmap(R.id.autofill_entry_icon, bitmap)
                }
            } catch (e: Exception) {
                Log.e(RemoteViews::class.java.name, "Unable to assign icon in remote view", e)
            }
        }
        return presentation
    }

    private fun buildDataset(context: Context,
                             database: Database,
                             entryInfo: EntryInfo,
                             struct: StructureParser.Result,
                             inlinePresentation: InlinePresentation?): Dataset? {
        val title = makeEntryTitle(entryInfo)
        val views = newRemoteViews(context, database, title, entryInfo.icon)
        val builder = Dataset.Builder(views)
        builder.setId(entryInfo.id.toString())

        struct.usernameId?.let { usernameId ->
            builder.setValue(usernameId, AutofillValue.forText(entryInfo.username))
        }
        struct.passwordId?.let { passwordId ->
            builder.setValue(passwordId, AutofillValue.forText(entryInfo.password))
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
                    builder.setValue(it, AutofillValue.forText("$year\u002D$monthString"))
                } else {
                    builder.setValue(it, AutofillValue.forDate(entryInfo.expiryTime.date.time))
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
                        builder.setValue(it, autofillValue)
                    }
                }

                if (autofillValue == null) {
                    builder.setValue(it, AutofillValue.forText(year.toString()))
                }
            }
            struct.creditCardExpirationMonthId?.let {
                if (struct.isWebView) {
                    builder.setValue(it, AutofillValue.forText(monthString))
                } else {
                    if (struct.creditCardExpirationMonthOptions != null) {
                        // index starts at 0
                        builder.setValue(it, AutofillValue.forList(month - 1))
                    } else {
                        builder.setValue(it, AutofillValue.forText(monthString))
                    }
                }
            }
            struct.creditCardExpirationDayId?.let {
                if (struct.isWebView) {
                    builder.setValue(it, AutofillValue.forText(dayString))
                } else {
                    if (struct.creditCardExpirationDayOptions != null) {
                        builder.setValue(it, AutofillValue.forList(day - 1))
                    } else {
                        builder.setValue(it, AutofillValue.forText(dayString))
                    }
                }
            }
        }
        for (field in entryInfo.customFields) {
            if (field.name == TemplateField.LABEL_HOLDER) {
                struct.creditCardHolderId?.let { ccNameId ->
                    builder.setValue(ccNameId, AutofillValue.forText(field.protectedValue.stringValue))
                }
            }
            if (field.name == TemplateField.LABEL_NUMBER) {
                struct.creditCardNumberId?.let { ccnId ->
                    builder.setValue(ccnId, AutofillValue.forText(field.protectedValue.stringValue))
                }
            }
            if (field.name == TemplateField.LABEL_CVV) {
                struct.cardVerificationValueId?.let { cvvId ->
                    builder.setValue(cvvId, AutofillValue.forText(field.protectedValue.stringValue))
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            inlinePresentation?.let {
                builder.setInlinePresentation(it)
            }
        }

        return try {
            builder.build()
        } catch (e: Exception) {
            // at least one value must be set
            null
        }
    }

    private fun buildOTPDataset(context: Context,
                                database: Database,
                                entryInfo: EntryInfo,
                                struct: StructureParser.Result,
                                inlinePresentation: InlinePresentation?): Dataset? {
        val otpModel: OtpModel? = entryInfo.otpModel
        val title = makeOTPEntryTitle(entryInfo)
        val views = newRemoteViews(context, database, title, entryInfo.icon)
        val builder = Dataset.Builder(views)

        if (otpModel?.secret == null)
            return null

        val autoFillId: AutofillId? = when {
            struct.OTPId != null -> {
                struct.OTPId
            }
            else -> {
                struct.focusedId
            }
        }

        if (autoFillId != null) {
            otpModel.run {
                // calculate OTP value
                val token: String =
                        when (type) {
                            OtpType.HOTP -> TokenCalculator.HOTP(secret, counter, digits, algorithm)
                            OtpType.TOTP -> when (tokenType) {
                                OtpTokenType.STEAM -> TokenCalculator.TOTP_Steam(secret, period, digits, algorithm)
                                else -> TokenCalculator.TOTP_RFC6238(secret, period, digits, algorithm)
                            }
                        }
                builder.setValue(autoFillId, AutofillValue.forText(token))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    inlinePresentation?.let {
                        builder.setInlinePresentation(it)
                    }
                }
                return try {
                    builder.build()
                } catch (e: Exception) {
                    null
                }
            }
        }
        return null
    }

    /**
     * Method to assign a drawable to a new icon from a database icon
     */
    private fun buildIconFromEntry(context: Context,
                                   database: Database,
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

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("RestrictedApi")
    private fun buildInlinePresentationForEntry(context: Context,
                                                database: Database,
                                                inlineSuggestionsRequest: InlineSuggestionsRequest,
                                                positionItem: Int,
                                                entryInfo: EntryInfo): InlinePresentation? {
        val inlinePresentationSpecs = inlineSuggestionsRequest.inlinePresentationSpecs
        val maxSuggestion = inlineSuggestionsRequest.maxSuggestionCount

        if (positionItem <= maxSuggestion - 1
                && inlinePresentationSpecs.size > positionItem) {
            val inlinePresentationSpec = inlinePresentationSpecs[positionItem]

            // Make sure that the IME spec claims support for v1 UI template.
            val imeStyle = inlinePresentationSpec.style
            if (!UiVersions.getVersions(imeStyle).contains(UiVersions.INLINE_UI_VERSION_1))
                return null

            // Build the content for IME UI
            val pendingIntent = PendingIntent.getActivity(context,
                    0,
                    Intent(context, AutofillSettingsActivity::class.java),
                    0)
            return InlinePresentation(
                    InlineSuggestionUi.newContentBuilder(pendingIntent).apply {
                        setContentDescription(context.getString(R.string.autofill_sign_in_prompt))
                        setTitle(entryInfo.title)
                        setSubtitle(entryInfo.username)
                        setStartIcon(Icon.createWithResource(context, R.mipmap.ic_launcher_round).apply {
                            setTintBlendMode(BlendMode.DST)
                        })
                        buildIconFromEntry(context, database, entryInfo)?.let { icon ->
                            setEndIcon(icon.apply {
                                setTintBlendMode(BlendMode.DST)
                            })
                        }
                    }.build().slice, inlinePresentationSpec, false)
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("RestrictedApi")
    private fun buildInlinePresentationForOTPEntry(context: Context,
                                                   inlineSuggestionsRequest: InlineSuggestionsRequest,
                                                   positionItem: Int,
                                                   entryInfo: EntryInfo): InlinePresentation? {
        val inlinePresentationSpecs = inlineSuggestionsRequest.inlinePresentationSpecs
        val maxSuggestion = inlineSuggestionsRequest.maxSuggestionCount

        if (positionItem <= maxSuggestion - 1
                && inlinePresentationSpecs.size > positionItem) {
            val inlinePresentationSpec = inlinePresentationSpecs[positionItem]

            // Make sure that the IME spec claims support for v1 UI template.
            val imeStyle = inlinePresentationSpec.style
            if (!UiVersions.getVersions(imeStyle).contains(UiVersions.INLINE_UI_VERSION_1))
                return null

            // Build the content for IME UI
            val pendingIntent = PendingIntent.getActivity(context,
                    0,
                    Intent(context, AutofillSettingsActivity::class.java),
                    0)
            return InlinePresentation(
                    InlineSuggestionUi.newContentBuilder(pendingIntent).apply {
                        setContentDescription(context.getString(R.string.autofill_sign_in_prompt))
                        setTitle(makeOTPEntryTitle(entryInfo))
                        setStartIcon(Icon.createWithResource(context, R.mipmap.ic_launcher_round).apply {
                            setTintBlendMode(BlendMode.DST)
                        })
                        setEndIcon(Icon.createWithResource(context, R.drawable.ic_otp_green_24dp).apply {
                            setTintBlendMode(BlendMode.DST)
                        })
                    }.build().slice, inlinePresentationSpec, false)
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("RestrictedApi")
    private fun buildInlinePresentationForManualSelection(context: Context,
                                                          inlineSuggestionsRequest: InlineSuggestionsRequest,
                                                          pendingIntent: PendingIntent,
                                                          positionItem: Int): InlinePresentation? {
        val inlinePresentationSpecs = inlineSuggestionsRequest.inlinePresentationSpecs
        val maxSuggestion = inlineSuggestionsRequest.maxSuggestionCount

        if (positionItem <= maxSuggestion - 1
                && inlinePresentationSpecs.size > positionItem) {
            val inlinePresentationSpec = inlinePresentationSpecs[positionItem]
            // Make sure that the IME spec claims support for v1 UI template.
            val imeStyle = inlinePresentationSpec.style
            if (!UiVersions.getVersions(imeStyle).contains(UiVersions.INLINE_UI_VERSION_1))
                return null

            // Build the content for IME UI
            return InlinePresentation(
                    InlineSuggestionUi.newContentBuilder(pendingIntent).apply {
                        setContentDescription(context.getString(R.string.autofill_manual_selection_summary))
                        setTitle(context.getString(R.string.autofill_manual_selection_prompt))
                        setStartIcon(Icon.createWithResource(context, R.drawable.ic_arrow_right_green_24dp).apply {
                            setTintBlendMode(BlendMode.DST)
                        })
                    }.build().slice, inlinePresentationSpec, false)
        }
        return null
    }

    fun buildResponse(context: Context,
                      database: Database,
                      entriesInfo: List<EntryInfo>,
                      parseResult: StructureParser.Result,
                      inlineSuggestionsRequest: InlineSuggestionsRequest?): FillResponse? {
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

        var maxInlineItems = entriesInfo.size
        var inlinePosition = 0

        // if inline suggestions are supported
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            inlineSuggestionsRequest?.let {
                maxInlineItems = it.maxSuggestionCount
            }
            if (PreferencesUtil.isAutofillManualSelectionEnable(context)) {
                --maxInlineItems
            }
            if (PreferencesUtil.isAutofillOTPSelectionEnable(context)) {
                --maxInlineItems
            }
        }

        val entriesToDisplay = minOf(maxInlineItems, entriesInfo.size)

        entriesInfo.forEachIndexed { _, entry ->
            val inlinePresentation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && inlinePosition < entriesToDisplay) {
                    inlineSuggestionsRequest?.let {
                        buildInlinePresentationForEntry(context, database, inlineSuggestionsRequest, inlinePosition++, entry)
                    }
                } else {
                null
            }

            responseBuilder.addDataset(buildDataset(context, database, entry, parseResult, inlinePresentation))
        }

        if (PreferencesUtil.isAutofillOTPSelectionEnable(context)) {
            for (entry in entriesInfo) {
                entry.otpModel?.let {
                    val inlinePresentation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        inlineSuggestionsRequest?.let {
                            buildInlinePresentationForOTPEntry(context, inlineSuggestionsRequest, inlinePosition++, entry)
                        }
                    } else {
                        null
                    }
                    responseBuilder.addDataset(buildOTPDataset(context, database, entry, parseResult, inlinePresentation))
                }

                // show only the first OTP value, if there is more than one
                if (entry.otpModel != null) {
                    break
                }
            }
        }

        if (PreferencesUtil.isAutofillManualSelectionEnable(context)) {
            val searchInfo = SearchInfo().apply {
                applicationId = parseResult.applicationId
                webDomain = parseResult.webDomain
                webScheme = parseResult.webScheme
                manualSelection = true
            }
            val manualSelectionView = RemoteViews(context.packageName, R.layout.item_autofill_entry)
            manualSelectionView.setTextViewText(R.id.autofill_entry_text, context.getString(R.string.autofill_manual_selection_prompt))
            val pendingIntent = AutofillLauncherActivity.getPendingIntentForSelection(context,
                    searchInfo, inlineSuggestionsRequest)

            parseResult.allAutofillIds().let { autofillIds ->
                autofillIds.forEach { id ->
                    val builder = Dataset.Builder(manualSelectionView)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        inlineSuggestionsRequest?.let {
                            val inlinePresentation = buildInlinePresentationForManualSelection(context, inlineSuggestionsRequest, pendingIntent, inlinePosition)
                            inlinePresentation?.let {
                                builder.setInlinePresentation(it)
                            }
                        }
                    }
                    builder.setValue(id, AutofillValue.forText("dummy"))
                    builder.setAuthentication(pendingIntent.intentSender)
                    responseBuilder.addDataset(builder.build())
                }
            }
        }

        return try {
            responseBuilder.build()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Build the Autofill response for one entry
     */
    fun buildResponseAndSetResult(activity: Activity,
                                  database: Database,
                                  entryInfo: EntryInfo) {
        buildResponseAndSetResult(activity, database, ArrayList<EntryInfo>().apply { add(entryInfo) })
    }

    /**
     * Build the Autofill response for many entry
     */
    fun buildResponseAndSetResult(activity: Activity,
                                  database: Database,
                                  entriesInfo: List<EntryInfo>) {
        if (entriesInfo.isEmpty()) {
            activity.setResult(Activity.RESULT_CANCELED)
        } else {
            var setResultOk = false
            activity.intent?.getParcelableExtra<AssistStructure>(EXTRA_ASSIST_STRUCTURE)?.let { structure ->
                StructureParser(structure).parse()?.let { result ->
                    // New Response
                    val response = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val inlineSuggestionsRequest = activity.intent?.getParcelableExtra<InlineSuggestionsRequest?>(EXTRA_INLINE_SUGGESTIONS_REQUEST)
                        if (inlineSuggestionsRequest != null) {
                            Toast.makeText(activity.applicationContext, R.string.autofill_inline_suggestions_keyboard, Toast.LENGTH_SHORT).show()
                        }
                        buildResponse(activity, database, entriesInfo, result, inlineSuggestionsRequest)
                    } else {
                        buildResponse(activity, database, entriesInfo, result, null)
                    }
                    val mReplyIntent = Intent()
                    Log.d(activity.javaClass.name, "Successed Autofill auth.")
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

    /**
     * Utility method to start an activity with an Autofill for result
     */
    fun startActivityForAutofillResult(activity: Activity,
                                       intent: Intent,
                                       autofillComponent: AutofillComponent,
                                       searchInfo: SearchInfo?) {
        EntrySelectionHelper.addSpecialModeInIntent(intent, SpecialMode.SELECTION)
        intent.putExtra(EXTRA_ASSIST_STRUCTURE, autofillComponent.assistStructure)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && PreferencesUtil.isAutofillInlineSuggestionsEnable(activity)) {
            autofillComponent.inlineSuggestionsRequest?.let {
                intent.putExtra(EXTRA_INLINE_SUGGESTIONS_REQUEST, it)
            }
        }
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
}
