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
import androidx.annotation.RequiresApi
import android.util.Log
import android.view.View
import android.view.autofill.AutofillId
import java.util.*


/**
 * Parse AssistStructure and guess username and password fields.
 */
@RequiresApi(api = Build.VERSION_CODES.O)
internal class StructureParser(private val structure: AssistStructure) {
    private var result: Result? = null
    private var usernameCandidate: AutofillId? = null
    private var usernameNeeded = true

    fun parse(): Result? {
        try {
            result = Result()
            result?.apply {
                usernameCandidate = null
                mainLoop@ for (i in 0 until structure.windowNodeCount) {
                    val windowNode = structure.getWindowNodeAt(i)
                    applicationId = windowNode.title.toString().split("/")[0]
                    Log.d(TAG, "Autofill applicationId: $applicationId")

                    if (parseViewNode(windowNode.rootViewNode))
                        break@mainLoop
                }
                // If not explicit username field found, add the field just before password field.
                if (usernameId == null && passwordId != null && usernameCandidate != null)
                    usernameId = usernameCandidate
            }

            // Return the result only if password field is retrieved
            return if ((!usernameNeeded || result?.usernameId != null)
                    && result?.passwordId != null)
                result
            else
                null
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseViewNode(node: AssistStructure.ViewNode): Boolean {
        // Get the domain of a web app
        node.webDomain?.let {
            result?.domain = it
            Log.d(TAG, "Autofill domain: $it")
        }

        // Only parse visible nodes
        if (node.visibility == View.VISIBLE) {
            if (node.autofillId != null
                    && node.autofillType == View.AUTOFILL_TYPE_TEXT) {
                // Parse methods
                val hints = node.autofillHints
                if (hints != null && hints.isNotEmpty()) {
                    if (parseNodeByAutofillHint(node))
                        return true
                } else if (parseNodeByHtmlAttributes(node))
                    return true
                else if (parseNodeByAndroidInput(node))
                    return true
            }
            // Recursive method to process each node
            for (i in 0 until node.childCount) {
                if (parseViewNode(node.getChildAt(i)))
                    return true
            }
        }
        return false
    }

    private fun parseNodeByAutofillHint(node: AssistStructure.ViewNode): Boolean {
        val autofillId = node.autofillId
        node.autofillHints?.forEach {
            when {
                it.equals(View.AUTOFILL_HINT_USERNAME, true)
                        || it.equals(View.AUTOFILL_HINT_EMAIL_ADDRESS, true)
                        || it.equals(View.AUTOFILL_HINT_PHONE, true)
                        || it.equals("email", true)
                        || it.equals("usernameOrEmail", true)-> {
                    result?.usernameId = autofillId
                    Log.d(TAG, "Autofill username hint")
                }
                it.equals(View.AUTOFILL_HINT_PASSWORD, true)
                        || it.contains("password", true) -> {
                    result?.passwordId = autofillId
                    Log.d(TAG, "Autofill password hint")
                    // Username not needed in this specific case
                    usernameNeeded = false
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
                                    Log.d(TAG, "Autofill username web type: ${node.htmlInfo?.tag} ${node.htmlInfo?.attributes}")
                                }
                                "text" -> {
                                    usernameCandidate = autofillId
                                    Log.d(TAG, "Autofill username candidate web type: ${node.htmlInfo?.tag} ${node.htmlInfo?.attributes}")
                                }
                                "password" -> {
                                    result?.passwordId = autofillId
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

    private fun parseNodeByAndroidInput(node: AssistStructure.ViewNode): Boolean {
        val autofillId = node.autofillId
        val inputType = node.inputType
        if (inputType and InputType.TYPE_CLASS_TEXT != 0) {
            when {
                inputType and InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS != 0 -> {
                    result?.usernameId = autofillId
                    Log.d(TAG, "Autofill username android type: $inputType")
                }
                inputType and InputType.TYPE_TEXT_VARIATION_NORMAL != 0 ||
                        inputType and InputType.TYPE_NUMBER_VARIATION_NORMAL != 0 ||
                        inputType and InputType.TYPE_TEXT_VARIATION_PERSON_NAME != 0 -> {
                    usernameCandidate = autofillId
                    Log.d(TAG, "Autofill username candidate android type: $inputType")
                }
                inputType and InputType.TYPE_TEXT_VARIATION_PASSWORD != 0 ||
                        inputType and InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD != 0 ||
                        inputType and InputType.TYPE_NUMBER_VARIATION_PASSWORD != 0 -> {
                    result?.passwordId = autofillId
                    Log.d(TAG, "Autofill password android type: $inputType")
                    return true
                }
                inputType and InputType.TYPE_TEXT_VARIATION_EMAIL_SUBJECT != 0 ||
                inputType and InputType.TYPE_TEXT_VARIATION_FILTER != 0 ||
                inputType and InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE != 0 ||
                inputType and InputType.TYPE_TEXT_VARIATION_PHONETIC != 0 ||
                inputType and InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS != 0 ||
                inputType and InputType.TYPE_TEXT_VARIATION_URI != 0 ||
                inputType and InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT != 0 ||
                inputType and InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS != 0 ||
                inputType and InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD != 0 -> {
                    // Type not used
                }
                else -> {
                    Log.d(TAG, "Autofill unknown android type: $inputType")
                    usernameCandidate = autofillId
                }
            }
        }
        return false
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    internal class Result {
        var applicationId: String? = null
        var domain: String? = null
            set(value) {
                if (field == null)
                    field = value
            }

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

        fun allAutofillIds(): Array<AutofillId> {
            val all = ArrayList<AutofillId>()
            usernameId?.let {
                all.add(it)
            }
            passwordId?.let {
                all.add(it)
            }
            return all.toTypedArray()
        }
    }

    companion object {
        private val TAG = StructureParser::class.java.name
    }
}
