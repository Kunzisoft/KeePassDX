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
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BlendMode
import android.graphics.drawable.Icon
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.*
import android.util.Log
import android.view.autofill.AutofillId
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.autofill.inline.UiVersions
import androidx.autofill.inline.v1.InlineSuggestionUi
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.AutofillLauncherActivity
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.DatabaseTaskProvider
import com.kunzisoft.keepass.database.helper.SearchHelper
import com.kunzisoft.keepass.model.CreditCard
import com.kunzisoft.keepass.model.RegisterInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.settings.AutofillSettingsActivity
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.WebDomain
import org.joda.time.DateTime


@RequiresApi(api = Build.VERSION_CODES.O)
class KeeAutofillService : AutofillService() {

    private var mDatabaseTaskProvider: DatabaseTaskProvider? = null
    private var mDatabase: ContextualDatabase? = null
    private var applicationIdBlocklist: Set<String>? = null
    private var webDomainBlocklist: Set<String>? = null
    private var askToSaveData: Boolean = false
    private var autofillInlineSuggestionsEnabled: Boolean = false

    override fun onCreate() {
        super.onCreate()

        mDatabaseTaskProvider = DatabaseTaskProvider(this)
        mDatabaseTaskProvider?.registerProgressTask()
        mDatabaseTaskProvider?.onDatabaseRetrieved = { database ->
            this.mDatabase = database
        }

        getPreferences()
    }

    override fun onDestroy() {
        mDatabaseTaskProvider?.unregisterProgressTask()

        super.onDestroy()
    }

    private fun getPreferences() {
        applicationIdBlocklist = PreferencesUtil.applicationIdBlocklist(this)
        webDomainBlocklist = PreferencesUtil.webDomainBlocklist(this)
        askToSaveData = PreferencesUtil.askToSaveAutofillData(this)
        autofillInlineSuggestionsEnabled = PreferencesUtil.isAutofillInlineSuggestionsEnable(this)
    }

    override fun onFillRequest(request: FillRequest,
                               cancellationSignal: CancellationSignal,
                               callback: FillCallback) {

        cancellationSignal.setOnCancelListener { Log.w(TAG, "Cancel autofill.") }

        if (request.flags and FillRequest.FLAG_COMPATIBILITY_MODE_REQUEST != 0) {
            Log.d(TAG, "Autofill requested in compatibility mode")
        } else {
            Log.d(TAG, "Autofill requested in native mode")
        }

        // Check user's settings for authenticating Responses and Datasets.
        val latestStructure = request.fillContexts.last().structure
        StructureParser(latestStructure).parse()?.let { parseResult ->

            // Build search info only if applicationId or webDomain are not blocked
            if (autofillAllowedFor(parseResult.applicationId, applicationIdBlocklist)
                    && autofillAllowedFor(parseResult.webDomain, webDomainBlocklist)) {
                val searchInfo = SearchInfo().apply {
                    applicationId = parseResult.applicationId
                    webDomain = parseResult.webDomain
                    webScheme = parseResult.webScheme
                }
                WebDomain.getConcreteWebDomain(this, searchInfo.webDomain) { webDomainWithoutSubDomain ->
                    searchInfo.webDomain = webDomainWithoutSubDomain
                    val inlineSuggestionsRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                            && autofillInlineSuggestionsEnabled) {
                        CompatInlineSuggestionsRequest(request)
                    } else {
                        null
                    }
                    launchSelection(mDatabase,
                            searchInfo,
                            parseResult,
                            inlineSuggestionsRequest,
                            callback)
                }
            }
        }
    }

    private fun launchSelection(database: ContextualDatabase?,
                                searchInfo: SearchInfo,
                                parseResult: StructureParser.Result,
                                inlineSuggestionsRequest: CompatInlineSuggestionsRequest?,
                                callback: FillCallback) {
        SearchHelper.checkAutoSearchInfo(this,
                database,
                searchInfo,
                { openedDatabase, items ->
                    callback.onSuccess(
                            AutofillHelper.buildResponse(this, openedDatabase,
                                    items, parseResult, inlineSuggestionsRequest)
                    )
                },
                { openedDatabase ->
                    // Show UI if no search result
                    showUIForEntrySelection(parseResult, openedDatabase,
                            searchInfo, inlineSuggestionsRequest, callback)
                },
                {
                    // Show UI if database not open
                    showUIForEntrySelection(parseResult, null,
                            searchInfo, inlineSuggestionsRequest, callback)
                }
        )
    }

    @SuppressLint("RestrictedApi")
    private fun showUIForEntrySelection(parseResult: StructureParser.Result,
                                        database: ContextualDatabase?,
                                        searchInfo: SearchInfo,
                                        inlineSuggestionsRequest: CompatInlineSuggestionsRequest?,
                                        callback: FillCallback) {
        var success = false
        parseResult.allAutofillIds().let { autofillIds ->
            if (autofillIds.isNotEmpty()) {
                // If the entire Autofill Response is authenticated, AuthActivity is used
                // to generate Response.
                val intentSender = AutofillLauncherActivity.getPendingIntentForSelection(this,
                        searchInfo, inlineSuggestionsRequest).intentSender
                val responseBuilder = FillResponse.Builder()
                val remoteViewsUnlock: RemoteViews = if (database == null) {
                    if (!parseResult.webDomain.isNullOrEmpty()) {
                        RemoteViews(
                            packageName,
                            R.layout.item_autofill_unlock_web_domain
                        ).apply {
                            setTextViewText(
                                R.id.autofill_web_domain_text,
                                parseResult.webDomain
                            )
                        }
                    } else if (!parseResult.applicationId.isNullOrEmpty()) {
                        RemoteViews(packageName, R.layout.item_autofill_unlock_app_id).apply {
                            setTextViewText(
                                R.id.autofill_app_id_text,
                                parseResult.applicationId
                            )
                        }
                    } else {
                        RemoteViews(packageName, R.layout.item_autofill_unlock)
                    }
                } else {
                    if (!parseResult.webDomain.isNullOrEmpty()) {
                        RemoteViews(
                            packageName,
                            R.layout.item_autofill_select_entry_web_domain
                        ).apply {
                            setTextViewText(
                                R.id.autofill_web_domain_text,
                                parseResult.webDomain
                            )
                        }
                    } else if (!parseResult.applicationId.isNullOrEmpty()) {
                        RemoteViews(packageName, R.layout.item_autofill_select_entry_app_id).apply {
                            setTextViewText(
                                R.id.autofill_app_id_text,
                                parseResult.applicationId
                            )
                        }
                    } else {
                        RemoteViews(packageName, R.layout.item_autofill_select_entry)
                    }
                }

                // Tell the autofill framework the interest to save credentials
                if (askToSaveData) {
                    var types: Int = SaveInfo.SAVE_DATA_TYPE_GENERIC
                    val requiredIds = ArrayList<AutofillId>()
                    val optionalIds = ArrayList<AutofillId>()

                    // Only if at least a password
                    parseResult.passwordId?.let { passwordInfo ->
                        parseResult.usernameId?.let { usernameInfo ->
                            types = types or SaveInfo.SAVE_DATA_TYPE_USERNAME
                            requiredIds.add(usernameInfo)
                        }
                        types = types or SaveInfo.SAVE_DATA_TYPE_PASSWORD
                        requiredIds.add(passwordInfo)
                    }
                    // or a credit card form
                    if (requiredIds.isEmpty()) {
                        parseResult.creditCardNumberId?.let { numberId ->
                            types = types or SaveInfo.SAVE_DATA_TYPE_CREDIT_CARD
                            requiredIds.add(numberId)
                            Log.d(TAG, "Asking to save credit card number")
                        }
                        parseResult.creditCardExpirationDateId?.let { id -> optionalIds.add(id) }
                        parseResult.creditCardExpirationYearId?.let { id -> optionalIds.add(id) }
                        parseResult.creditCardExpirationMonthId?.let { id -> optionalIds.add(id) }
                        parseResult.creditCardHolderId?.let { id -> optionalIds.add(id) }
                        parseResult.cardVerificationValueId?.let { id -> optionalIds.add(id) }
                    }
                    if (requiredIds.isNotEmpty()) {
                        val builder = SaveInfo.Builder(types, requiredIds.toTypedArray())
                        if (optionalIds.isNotEmpty()) {
                            builder.setOptionalIds(optionalIds.toTypedArray())
                        }
                        responseBuilder.setSaveInfo(builder.build())
                    }
                }

                // Build inline presentation
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                        && autofillInlineSuggestionsEnabled) {
                    var inlinePresentation: InlinePresentation? = null
                    inlineSuggestionsRequest?.inlineSuggestionsRequest?.let { inlineSuggestionsRequest ->
                        val inlinePresentationSpecs = inlineSuggestionsRequest.inlinePresentationSpecs
                        if (inlineSuggestionsRequest.maxSuggestionCount > 0
                                && inlinePresentationSpecs.size > 0) {
                            val inlinePresentationSpec = inlinePresentationSpecs[0]

                            // Make sure that the IME spec claims support for v1 UI template.
                            val imeStyle = inlinePresentationSpec.style
                            if (UiVersions.getVersions(imeStyle).contains(UiVersions.INLINE_UI_VERSION_1)) {
                                // Build the content for IME UI
                                inlinePresentation = InlinePresentation(
                                        InlineSuggestionUi.newContentBuilder(
                                                PendingIntent.getActivity(this,
                                                    0,
                                                    Intent(this, AutofillSettingsActivity::class.java),
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                        PendingIntent.FLAG_IMMUTABLE
                                                    } else {
                                                        0
                                                    })
                                        ).apply {
                                            setContentDescription(getString(R.string.autofill_sign_in_prompt))
                                            setTitle(getString(R.string.autofill_sign_in_prompt))
                                            setStartIcon(Icon.createWithResource(this@KeeAutofillService, R.mipmap.ic_launcher_round).apply {
                                                setTintBlendMode(BlendMode.DST)
                                            })
                                        }.build().slice, inlinePresentationSpec, false)
                            }
                        }
                    }

                    // Build response
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        try {
                            // Buggy method on some API 33 devices
                            responseBuilder.setAuthentication(
                                autofillIds,
                                intentSender,
                                Presentations.Builder().apply {
                                    inlinePresentation?.let {
                                        setInlinePresentation(it)
                                    }
                                    setDialogPresentation(remoteViewsUnlock)
                                }.build()
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Unable to use the new setAuthentication method.", e)
                            @Suppress("DEPRECATION")
                            responseBuilder.setAuthentication(autofillIds, intentSender, remoteViewsUnlock, inlinePresentation)
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        responseBuilder.setAuthentication(autofillIds, intentSender, remoteViewsUnlock, inlinePresentation)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    responseBuilder.setAuthentication(autofillIds, intentSender, remoteViewsUnlock)
                }
                success = true
                callback.onSuccess(responseBuilder.build())
            }
        }
        if (!success)
            callback.onFailure("Unable to get Autofill ids for UI selection")
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        var success = false
        if (askToSaveData) {
            val latestStructure = request.fillContexts.last().structure
            StructureParser(latestStructure).parse(true)?.let { parseResult ->

                if (autofillAllowedFor(parseResult.applicationId, applicationIdBlocklist)
                        && autofillAllowedFor(parseResult.webDomain, webDomainBlocklist)) {
                    Log.d(TAG, "autofill onSaveRequest password")

                    // Build expiration from date or from year and month
                    var expiration: DateTime? = parseResult.creditCardExpirationValue
                    if (parseResult.creditCardExpirationValue == null
                        && parseResult.creditCardExpirationYearValue != 0
                        && parseResult.creditCardExpirationMonthValue != 0) {
                        expiration = DateTime()
                            .withYear(parseResult.creditCardExpirationYearValue)
                            .withMonthOfYear(parseResult.creditCardExpirationMonthValue)
                        if (parseResult.creditCardExpirationDayValue != 0) {
                            expiration = expiration.withDayOfMonth(parseResult.creditCardExpirationDayValue)
                        }
                    }

                    // Show UI to save data
                    val registerInfo = RegisterInfo(
                            SearchInfo().apply {
                                applicationId = parseResult.applicationId
                                webDomain = parseResult.webDomain
                                webScheme = parseResult.webScheme
                            },
                            parseResult.usernameValue?.textValue?.toString(),
                            parseResult.passwordValue?.textValue?.toString(),
                            CreditCard(
                                parseResult.creditCardHolder,
                                parseResult.creditCardNumber,
                                expiration,
                                parseResult.cardVerificationValue
                            ))

                    // TODO Callback in each activity #765
                    //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    //    callback.onSuccess(AutofillLauncherActivity.getAuthIntentSenderForRegistration(this,
                    //            registerInfo))
                    //} else {
                    AutofillLauncherActivity.launchForRegistration(this, registerInfo)
                    success = true
                    callback.onSuccess()
                    //}
                }
            }
        }
        if (!success) {
            callback.onFailure("Saving form values is not allowed")
        }
    }

    override fun onConnected() {
        Log.d(TAG, "onConnected")
        getPreferences()
    }

    override fun onDisconnected() {
        Log.d(TAG, "onDisconnected")
    }

    companion object {
        private val TAG = KeeAutofillService::class.java.name

        fun autofillAllowedFor(element: String?, blockList: Set<String>?): Boolean {
            element?.let { elementNotNull ->
                if (blockList?.any { appIdBlocked ->
                            elementNotNull.contains(appIdBlocked)
                        } == true
                ) {
                    Log.d(TAG, "Autofill not allowed for $elementNotNull")
                    return false
                }
            }
            return true
        }
    }
}
