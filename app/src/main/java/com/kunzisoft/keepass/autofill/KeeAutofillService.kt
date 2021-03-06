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
import android.view.inputmethod.InlineSuggestionsRequest
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.autofill.inline.UiVersions
import androidx.autofill.inline.v1.InlineSuggestionUi
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.AutofillLauncherActivity
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.search.SearchHelper
import com.kunzisoft.keepass.model.RegisterInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.settings.AutofillSettingsActivity
import com.kunzisoft.keepass.settings.PreferencesUtil
import java.util.concurrent.atomic.AtomicBoolean


@RequiresApi(api = Build.VERSION_CODES.O)
class KeeAutofillService : AutofillService() {

    var applicationIdBlocklist: Set<String>? = null
    var webDomainBlocklist: Set<String>? = null
    var askToSaveData: Boolean = false
    var autofillInlineSuggestionsEnabled: Boolean = false
    private var mLock = AtomicBoolean()

    override fun onCreate() {
        super.onCreate()
        getPreferences()
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

        // Lock
        if (!mLock.get()) {
            mLock.set(true)
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
                    SearchInfo.getConcreteWebDomain(this, searchInfo.webDomain) { webDomainWithoutSubDomain ->
                        searchInfo.webDomain = webDomainWithoutSubDomain
                        val inlineSuggestionsRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                                && autofillInlineSuggestionsEnabled) {
                            request.inlineSuggestionsRequest
                        } else {
                            null
                        }
                        launchSelection(searchInfo,
                                parseResult,
                                inlineSuggestionsRequest,
                                callback)
                    }
                }
            }
        }
    }

    private fun launchSelection(searchInfo: SearchInfo,
                                parseResult: StructureParser.Result,
                                inlineSuggestionsRequest: InlineSuggestionsRequest?,
                                callback: FillCallback) {
        SearchHelper.checkAutoSearchInfo(this,
                Database.getInstance(),
                searchInfo,
                { items ->
                    callback.onSuccess(
                            AutofillHelper.buildResponse(this,
                                    items, parseResult, inlineSuggestionsRequest)
                    )
                },
                {
                    // Show UI if no search result
                    showUIForEntrySelection(parseResult,
                            searchInfo, inlineSuggestionsRequest, callback)
                },
                {
                    // Show UI if database not open
                    showUIForEntrySelection(parseResult,
                            searchInfo, inlineSuggestionsRequest, callback)
                }
        )
    }

    @SuppressLint("RestrictedApi")
    private fun showUIForEntrySelection(parseResult: StructureParser.Result,
                                        searchInfo: SearchInfo,
                                        inlineSuggestionsRequest: InlineSuggestionsRequest?,
                                        callback: FillCallback) {
        parseResult.allAutofillIds().let { autofillIds ->
            if (autofillIds.isNotEmpty()) {
                // If the entire Autofill Response is authenticated, AuthActivity is used
                // to generate Response.
                val intentSender = AutofillLauncherActivity.getAuthIntentSenderForSelection(this,
                        searchInfo, inlineSuggestionsRequest)
                val responseBuilder = FillResponse.Builder()
                val remoteViewsUnlock: RemoteViews = if (!parseResult.webDomain.isNullOrEmpty()) {
                    RemoteViews(packageName, R.layout.item_autofill_unlock_web_domain).apply {
                        setTextViewText(R.id.autofill_web_domain_text, parseResult.webDomain)
                    }
                } else if (!parseResult.applicationId.isNullOrEmpty()) {
                    RemoteViews(packageName, R.layout.item_autofill_unlock_app_id).apply {
                        setTextViewText(R.id.autofill_app_id_text, parseResult.applicationId)
                    }
                } else {
                    RemoteViews(packageName, R.layout.item_autofill_unlock)
                }

                // Tell to service the interest to save credentials
                if (askToSaveData) {
                    var types: Int = SaveInfo.SAVE_DATA_TYPE_GENERIC
                    val info = ArrayList<AutofillId>()
                    // Only if at least a password
                    parseResult.passwordId?.let { passwordInfo ->
                        parseResult.usernameId?.let { usernameInfo ->
                            types = types or SaveInfo.SAVE_DATA_TYPE_USERNAME
                            info.add(usernameInfo)
                        }
                        types = types or SaveInfo.SAVE_DATA_TYPE_PASSWORD
                        info.add(passwordInfo)
                    }
                    if (info.isNotEmpty()) {
                        responseBuilder.setSaveInfo(
                                SaveInfo.Builder(types, info.toTypedArray()).build()
                        )
                    }
                }

                // Build inline presentation
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                        && autofillInlineSuggestionsEnabled) {
                    var inlinePresentation: InlinePresentation? = null
                    inlineSuggestionsRequest?.let {
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
                                                        0)
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
                    responseBuilder.setAuthentication(autofillIds, intentSender, remoteViewsUnlock, inlinePresentation)
                }
                responseBuilder.setAuthentication(autofillIds, intentSender, remoteViewsUnlock)
                callback.onSuccess(responseBuilder.build())
            }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        if (askToSaveData) {
            val latestStructure = request.fillContexts.last().structure
            StructureParser(latestStructure).parse(true)?.let { parseResult ->

                if (autofillAllowedFor(parseResult.applicationId, applicationIdBlocklist)
                        && autofillAllowedFor(parseResult.webDomain, webDomainBlocklist)) {
                    Log.d(TAG, "autofill onSaveRequest password")

                    // Show UI to save data
                    val registerInfo = RegisterInfo(SearchInfo().apply {
                        applicationId = parseResult.applicationId
                        webDomain = parseResult.webDomain
                        webScheme = parseResult.webScheme
                    },
                            parseResult.usernameValue?.textValue?.toString(),
                            parseResult.passwordValue?.textValue?.toString())
                    // TODO Callback in each activity #765
                    //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    //    callback.onSuccess(AutofillLauncherActivity.getAuthIntentSenderForRegistration(this,
                    //            registerInfo))
                    //} else {
                        AutofillLauncherActivity.launchForRegistration(this, registerInfo)
                        callback.onSuccess()
                    //}
                    return
                }
            }
        }
        callback.onFailure("Saving form values is not allowed")
    }

    override fun onConnected() {
        Log.d(TAG, "onConnected")
        getPreferences()
    }

    override fun onDisconnected() {
        mLock.set(false)
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
