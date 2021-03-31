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
 */
package com.kunzisoft.keepass.autofill

import android.app.assist.AssistStructure
import android.os.Build
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import androidx.annotation.RequiresApi
import java.util.*
import kotlin.collections.ArrayList


/**
 * Parse AssistStructure and guess username and password fields.
 */
@RequiresApi(api = Build.VERSION_CODES.O)
class StructureParser(private val structure: AssistStructure) {
    private var result: Result? = null

    private var usernameIdCandidate: AutofillId? = null
    private var usernameValueCandidate: AutofillValue? = null

    fun parse(saveValue: Boolean = false): Result? {
        try {
            result = Result()
            result?.apply {
                allowSaveValues = saveValue
                usernameIdCandidate = null
                usernameValueCandidate = null
                mainLoop@ for (i in 0 until structure.windowNodeCount) {
                    val windowNode = structure.getWindowNodeAt(i)
                    applicationId = windowNode.title.toString().split("/")[0]
                    Log.d(TAG, "Autofill applicationId: $applicationId")

                    if (parseViewNode(windowNode.rootViewNode))
                        break@mainLoop
                }
                // If not explicit username field found, add the field just before password field.
                if (usernameId == null && passwordId != null && usernameIdCandidate != null) {
                    usernameId = usernameIdCandidate
                    if (allowSaveValues) {
                        usernameValue = usernameValueCandidate
                    }
                }
            }

            return result
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseViewNode(node: AssistStructure.ViewNode): Boolean {
        // remember this
        if (node.className == "android.webkit.WebView") {
            result?.isWebView = true
        }

        // Get the domain of a web app
        node.webDomain?.let { webDomain ->
            if (webDomain.isNotEmpty()) {
                result?.webDomain = webDomain
                Log.d(TAG, "Autofill domain: $webDomain")
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            node.webScheme?.let { webScheme ->
                if (webScheme.isNotEmpty()) {
                    result?.webScheme = webScheme
                    Log.d(TAG, "Autofill scheme: $webScheme")
                }
            }
        }
        val domainNotEmpty = result?.webDomain?.isNotEmpty() == true

        var returnValue = false
        // Only parse visible nodes
        if (node.visibility == View.VISIBLE) {
            if (node.autofillId != null) {
                // Parse methods
                val hints = node.autofillHints
                if (hints != null && hints.isNotEmpty()) {
                    if (parseNodeByAutofillHint(node))
                        returnValue = true
                } else if (parseNodeByHtmlAttributes(node))
                    returnValue = true
                else if (parseNodeByAndroidInput(node))
                    returnValue = true
            }
            // Optimized return but only if domain not empty
            if (domainNotEmpty && returnValue)
                return true
            // Recursive method to process each node
            for (i in 0 until node.childCount) {
                if (parseViewNode(node.getChildAt(i)))
                    returnValue = true
                if (domainNotEmpty && returnValue)
                    return true
            }
        }
        return returnValue
    }

    private fun parseNodeByAutofillHint(node: AssistStructure.ViewNode): Boolean {
        val autofillId = node.autofillId
        node.autofillHints?.forEach {
            when {
                it.contains(View.AUTOFILL_HINT_USERNAME, true)
                        || it.contains(View.AUTOFILL_HINT_EMAIL_ADDRESS, true)
                        || it.contains("email", true)
                        || it.contains(View.AUTOFILL_HINT_PHONE, true) -> {
                    result?.usernameId = autofillId
                    result?.usernameValue = node.autofillValue
                    Log.d(TAG, "Autofill username hint")
                }
                it.contains(View.AUTOFILL_HINT_PASSWORD, true) -> {
                    result?.passwordId = autofillId
                    result?.passwordValue = node.autofillValue
                    Log.d(TAG, "Autofill password hint")
                    return true
                }
                it == "cc-name" -> {
                    result?.ccNameId = autofillId
                    result?.ccNameValue = node.autofillValue
                    return true
                }
                it == View.AUTOFILL_HINT_CREDIT_CARD_NUMBER || it == "cc-number" -> {
                    result?.ccnId = autofillId
                    result?.ccnValue = node.autofillValue
                    Log.d(TAG, "AUTOFILL_HINT_CREDIT_CARD_NUMBER hint")
                    return true
                }
                it == View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE || it == "cc-exp" -> {
                    result?.ccExpDateId = autofillId
                    Log.d(TAG, "AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE hint")
                    return true
                }
                it == View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR || it == "cc-exp-year" -> {
                    Log.d(TAG, "AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR hint")
                    result?.ccExpDateYearId = autofillId
                    if (node.autofillValue != null) {
                        if (node.autofillValue?.isText == true) {
                            try {
                                result?.ccExpDateYearValue =
                                        node.autofillValue?.textValue.toString().toInt()
                            } catch (e: Exception) {
                                result?.ccExpDateYearValue = 0
                            }
                        }
                    }
                    if (node.autofillOptions != null) {
                        result?.ccExpYearOptions = node.autofillOptions
                    }
                    return true
                }
                it == View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH || it == "cc-exp-month" -> {
                    Log.d(TAG, "AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH hint")
                    result?.ccExpDateMonthId = autofillId
                    if (node.autofillValue != null) {
                        if (node.autofillValue?.isText == true) {
                            try {
                                result?.ccExpDateMonthValue =
                                        node.autofillValue?.textValue.toString().toInt()
                            } catch (e: Exception) {
                                result?.ccExpDateMonthValue = 0
                            }
                        }
                    }
                    if (node.autofillOptions != null) {
                        result?.ccExpMonthOptions = node.autofillOptions
                    }
                    return true
                }
                it == View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE || it == "cc-csc" -> {
                    result?.cvvId = autofillId
                    result?.cvvValue = node.autofillValue
                    Log.d(TAG, "AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE hint")
                    return true
                }
                // Ignore autocomplete="off"
                // https://developer.mozilla.org/en-US/docs/Web/Security/Securing_your_site/Turning_off_form_autocompletion
                it.equals("off", true) ||
                        it.equals("on", true) -> {
                    Log.d(TAG, "Autofill web hint")
                    return parseNodeByHtmlAttributes(node)
                }
                else -> Log.d(TAG, "Autofill unsupported hint $it")
            }
        }
        return false
    }

    private fun parseNodeByHtmlAttributes(node: AssistStructure.ViewNode): Boolean {
        val autofillId = node.autofillId
        val nodHtml = node.htmlInfo
        when (nodHtml?.tag?.toLowerCase(Locale.ENGLISH)) {
            "input" -> {
                nodHtml.attributes?.forEach { pairAttribute ->
                    when (pairAttribute.first.toLowerCase(Locale.ENGLISH)) {
                        "type" -> {
                            when (pairAttribute.second.toLowerCase(Locale.ENGLISH)) {
                                "tel", "email" -> {
                                    result?.usernameId = autofillId
                                    result?.usernameValue = node.autofillValue
                                    Log.d(TAG, "Autofill username web type: ${node.htmlInfo?.tag} ${node.htmlInfo?.attributes}")
                                }
                                "text" -> {
                                    usernameIdCandidate = autofillId
                                    usernameValueCandidate = node.autofillValue
                                    Log.d(TAG, "Autofill username candidate web type: ${node.htmlInfo?.tag} ${node.htmlInfo?.attributes}")
                                }
                                "password" -> {
                                    result?.passwordId = autofillId
                                    result?.passwordValue = node.autofillValue
                                    Log.d(TAG, "Autofill password web type: ${node.htmlInfo?.tag} ${node.htmlInfo?.attributes}")
                                    return true
                                }
                            }
                        }
                    }
                }
            }
        }
        return false
    }

    private fun inputIsVariationType(inputType: Int, vararg type: Int): Boolean {
        type.forEach {
            if (inputType and InputType.TYPE_MASK_VARIATION == it)
                return true
        }
        return false
    }

    private fun showHexInputType(inputType: Int): String {
        return "0x${"%08x".format(inputType)}"
    }

    private fun parseNodeByAndroidInput(node: AssistStructure.ViewNode): Boolean {
        val autofillId = node.autofillId
        val inputType = node.inputType
        when (inputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_TEXT -> {
                when {
                    inputIsVariationType(inputType,
                            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS) -> {
                        result?.usernameId = autofillId
                        result?.usernameValue = node.autofillValue
                        Log.d(TAG, "Autofill username android text type: ${showHexInputType(inputType)}")
                    }
                    inputIsVariationType(inputType,
                            InputType.TYPE_TEXT_VARIATION_NORMAL,
                            InputType.TYPE_TEXT_VARIATION_PERSON_NAME,
                            InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT) -> {
                        usernameIdCandidate = autofillId
                        usernameValueCandidate = node.autofillValue
                        Log.d(TAG, "Autofill username candidate android text type: ${showHexInputType(inputType)}")
                    }
                    inputIsVariationType(inputType,
                            InputType.TYPE_TEXT_VARIATION_PASSWORD,
                            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) -> {
                        result?.passwordId = autofillId
                        result?.passwordValue = node.autofillValue
                        Log.d(TAG, "Autofill password android text type: ${showHexInputType(inputType)}")
                        return true
                    }
                    inputIsVariationType(inputType,
                            InputType.TYPE_TEXT_VARIATION_EMAIL_SUBJECT,
                            InputType.TYPE_TEXT_VARIATION_FILTER,
                            InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE,
                            InputType.TYPE_TEXT_VARIATION_PHONETIC,
                            InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS,
                            InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE,
                            InputType.TYPE_TEXT_VARIATION_URI) -> {
                        // Type not used
                    }
                    else -> {
                        Log.d(TAG, "Autofill unknown android text type: ${showHexInputType(inputType)}")
                    }
                }
            }
            InputType.TYPE_CLASS_NUMBER -> {
                when {
                    inputIsVariationType(inputType,
                            InputType.TYPE_NUMBER_VARIATION_NORMAL) -> {
                        usernameIdCandidate = autofillId
                        usernameValueCandidate = node.autofillValue
                        Log.d(TAG, "Autofill username candidate android number type: ${showHexInputType(inputType)}")
                    }
                    inputIsVariationType(inputType,
                            InputType.TYPE_NUMBER_VARIATION_PASSWORD) -> {
                        result?.passwordId = autofillId
                        result?.passwordValue = node.autofillValue
                        Log.d(TAG, "Autofill password android number type: ${showHexInputType(inputType)}")
                        return true
                    }
                    else -> {
                        Log.d(TAG, "Autofill unknown android number type: ${showHexInputType(inputType)}")
                    }
                }
            }
        }
        return false
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    class Result {
        var isWebView: Boolean = false
        var applicationId: String? = null

        var webDomain: String? = null
            set(value) {
                if (field == null)
                    field = value
            }

        var webScheme: String? = null
            set(value) {
                if (field == null)
                    field = value
            }

        // if the user selects the credit card expiration date from a list of options
        // all options are stored here
        var ccExpMonthOptions: Array<CharSequence>? = null
        var ccExpYearOptions: Array<CharSequence>? = null

        var usernameId: AutofillId? = null
            set(value) {
                if (field == null)
                    field = value
            }

        var passwordId: AutofillId? = null
            set(value) {
                if (field == null)
                    field = value
            }

        var ccNameId: AutofillId? = null
            set(value) {
                if (field == null)
                    field = value
            }

        var ccnId: AutofillId? = null
            set(value) {
                if (field == null)
                    field = value
            }

        var ccExpDateId: AutofillId? = null
            set(value) {
                if (field == null)
                    field = value
            }

        var ccExpDateYearId: AutofillId? = null
            set(value) {
                if (field == null)
                    field = value
            }

        var ccExpDateMonthId: AutofillId? = null
            set(value) {
                if (field == null)
                    field = value
            }

        var cvvId: AutofillId? = null
            set(value) {
                if (field == null)
                    field = value
            }

        fun allAutofillIds(): Array<AutofillId> {
            val all = ArrayList<AutofillId>()
            usernameId?.let {
                all.add(it)
            }
            passwordId?.let {
                all.add(it)
            }
            ccNameId?.let {
                all.add(it)
            }
            ccnId?.let {
                all.add(it)
            }
            ccExpDateId?.let {
                all.add(it)
            }
            ccExpDateYearId?.let {
                all.add(it)
            }
            ccExpDateMonthId?.let {
                all.add(it)
            }
            cvvId?.let {
                all.add(it)
            }
            return all.toTypedArray()
        }

        // Only in registration mode
        var allowSaveValues = false

        var usernameValue: AutofillValue? = null
            set(value) {
                if (allowSaveValues && field == null)
                    field = value
            }

        var passwordValue: AutofillValue? = null
            set(value) {
                if (allowSaveValues && field == null)
                    field = value
            }

        // stores name of cardholder
        var ccNameValue: AutofillValue? = null
            set(value) {
                if (allowSaveValues && field == null)
                    field = value
            }

        // stores credit card number
        var ccnValue: AutofillValue? = null
            set(value) {
                if (allowSaveValues && field == null)
                    field = value
            }

        // for year of CC expiration date
        var ccExpDateYearValue = 0
            set(value) {
                if (allowSaveValues)
                    field = value
            }

        // for month of CC expiration date
        var ccExpDateMonthValue = 0
            set(value) {
                if (allowSaveValues)
                    field = value
            }

        // the security code for the credit card (also called CVV)
        var cvvValue: AutofillValue? = null
            set(value) {
                if (allowSaveValues && field == null)
                    field = value
            }
    }

    companion object {
        private val TAG = StructureParser::class.java.name
    }
}
