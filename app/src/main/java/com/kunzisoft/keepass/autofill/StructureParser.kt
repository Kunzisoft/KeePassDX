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

    fun parse(): Result? {
        result = Result()
        result?.apply {
            usernameCandidate = null
            mainLoop@ for (i in 0 until structure.windowNodeCount) {
                val windowNode = structure.getWindowNodeAt(i)
                /*
                title.add(windowNode.title)
                windowNode.rootViewNode.webDomain?.let {
                    webDomain.add(it)
                }
                */
                if (parseViewNode(windowNode.rootViewNode))
                    break@mainLoop
            }
            // If not explicit username field found, add the field just before password field.
            if (usernameId == null && passwordId != null && usernameCandidate != null)
                usernameId = usernameCandidate
        }

        // Return the result only if password field is retrieved
        return if (result?.passwordId != null)
            result
        else
            null
    }

    private fun parseViewNode(node: AssistStructure.ViewNode): Boolean {
        if (node.autofillId != null) {
            val hints = node.autofillHints
            if (hints != null && hints.isNotEmpty()) {
                if (parseNodeByAutofillHint(node))
                    return true
            } else {
                if (parseNodeByHtmlAttributes(node))
                    return true
            }
        }
        // Recursive method to process each node
        for (i in 0 until node.childCount) {
            if (parseViewNode(node.getChildAt(i)))
                return true
        }
        return false
    }

    private fun parseNodeByAutofillHint(node: AssistStructure.ViewNode): Boolean {
        val autofillId = node.autofillId
        node.autofillHints?.forEach {
            when {
                it.toLowerCase(Locale.ENGLISH) == View.AUTOFILL_HINT_USERNAME
                        || it.toLowerCase(Locale.ENGLISH) == View.AUTOFILL_HINT_EMAIL_ADDRESS
                        || it.toLowerCase(Locale.ENGLISH) == View.AUTOFILL_HINT_PHONE -> {
                    result?.usernameId = autofillId
                    Log.d(TAG, "Autofill username hint")
                }
                it.toLowerCase(Locale.ENGLISH) == View.AUTOFILL_HINT_PASSWORD
                        || it.toLowerCase(Locale.ENGLISH).contains("password") -> {
                    result?.passwordId = autofillId
                    Log.d(TAG, "Autofill password hint")
                    return true
                }
                // Ignore autocomplete="off"
                // https://developer.mozilla.org/en-US/docs/Web/Security/Securing_your_site/Turning_off_form_autocompletion
                it.toLowerCase(Locale.ENGLISH) == "off" ||
                it.toLowerCase(Locale.ENGLISH) == "on" -> {
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
                                    Log.d(TAG, "Autofill username type: ${node.htmlInfo?.tag} ${node.htmlInfo?.attributes}")
                                }
                                "text" -> {
                                    usernameCandidate = autofillId
                                    Log.d(TAG, "Autofill type: ${node.htmlInfo?.tag} ${node.htmlInfo?.attributes}")
                                }
                                "password" -> {
                                    result?.passwordId = autofillId
                                    Log.d(TAG, "Autofill password type: ${node.htmlInfo?.tag} ${node.htmlInfo?.attributes}")
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    internal class Result {
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
