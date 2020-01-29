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
import android.text.InputType
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
            for (i in 0 until structure.windowNodeCount) {
                val windowNode = structure.getWindowNodeAt(i)
                title.add(windowNode.title)
                windowNode.rootViewNode.webDomain?.let {
                    webDomain.add(it)
                }
                parseViewNode(windowNode.rootViewNode)
            }
            // If not explicit username field found, add the field just before password field.
            if (username.isEmpty() && email.isEmpty()
                    && password.isNotEmpty() && usernameCandidate != null)
                username.add(usernameCandidate!!)
        }

        return result
    }

    private fun parseViewNode(node: AssistStructure.ViewNode) {
        val hints = node.autofillHints
        val autofillId = node.autofillId
        if (autofillId != null) {
            if (hints != null && hints.isNotEmpty()) {
                when {
                    Arrays.stream(hints).anyMatch { View.AUTOFILL_HINT_USERNAME == it } -> result?.username?.add(autofillId)
                    Arrays.stream(hints).anyMatch { View.AUTOFILL_HINT_EMAIL_ADDRESS == it } -> result?.email?.add(autofillId)
                    Arrays.stream(hints).anyMatch { View.AUTOFILL_HINT_PASSWORD == it } -> result?.password?.add(autofillId)
                    else -> Log.d(TAG, "unsupported hints")
                }
            } else if (node.autofillType == View.AUTOFILL_TYPE_TEXT) {
                val inputType = node.inputType
                when {
                    inputType and InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS > 0 -> result?.email?.add(autofillId)
                    inputType and InputType.TYPE_TEXT_VARIATION_PASSWORD > 0 -> result?.password?.add(autofillId)
                    result?.password?.isEmpty() == true -> usernameCandidate = autofillId
                }
            }
        }

        for (i in 0 until node.childCount)
            parseViewNode(node.getChildAt(i))
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    internal class Result {
        val title: MutableList<CharSequence>
        val webDomain: MutableList<String>
        val username: MutableList<AutofillId>
        val email: MutableList<AutofillId>
        val password: MutableList<AutofillId>

        init {
            title = ArrayList()
            webDomain = ArrayList()
            username = ArrayList()
            email = ArrayList()
            password = ArrayList()
        }

        fun allAutofillIds(): Array<AutofillId> {
            val all = ArrayList<AutofillId>()
            all.addAll(username)
            all.addAll(email)
            all.addAll(password)
            return all.toTypedArray()
        }
    }

    companion object {
        private val TAG = StructureParser::class.java.name
    }
}
