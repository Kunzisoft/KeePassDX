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
package com.kunzisoft.keepass.activities.dialogs

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import com.kunzisoft.keepass.R

class UnavailableFeatureDialogFragment : DialogFragment() {
    private var minVersionRequired = Build.VERSION_CODES.M

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            arguments?.apply {
                if (containsKey(MIN_REQUIRED_VERSION_ARG))
                    minVersionRequired = getInt(MIN_REQUIRED_VERSION_ARG)
            }

            val rootView = activity.layoutInflater.inflate(R.layout.fragment_unavailable_feature, null)
            val messageView = rootView.findViewById<TextView>(R.id.unavailable_feature_message)

            val builder = AlertDialog.Builder(activity)

            val message = SpannableStringBuilder()
            message.append(getString(R.string.unavailable_feature_text))
                    .append("\n\n")
            if (Build.VERSION.SDK_INT < minVersionRequired) {
                message.append(getString(R.string.unavailable_feature_version,
                        androidNameFromApiNumber(Build.VERSION.SDK_INT, Build.VERSION.RELEASE),
                        androidNameFromApiNumber(minVersionRequired)))
                message.append("\n\n")
                        .append(HtmlCompat.fromHtml("<a href=\"https://source.android.com/setup/build-numbers\">CodeNames</a>", HtmlCompat.FROM_HTML_MODE_LEGACY))
            } else
                message.append(getString(R.string.unavailable_feature_hardware))

            messageView.text = message
            messageView.movementMethod = LinkMovementMethod.getInstance()

            builder.setView(rootView)
                    .setPositiveButton(android.R.string.ok) { _, _ -> }
            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }

    private fun androidNameFromApiNumber(apiNumber: Int, releaseVersion: String = ""): String {
        var version = releaseVersion
        val builder = StringBuilder()
        val fields = Build.VERSION_CODES::class.java.fields
        var apiName = ""
        for (field in fields) {
            val fieldName = field.name
            var fieldValue = -1
            try {
                fieldValue = field.getInt(Any())
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }

            if (fieldValue == apiNumber) {
                apiName = fieldName
            }
        }
        if (apiName.isEmpty()) {
            val mapper = arrayOf("ANDROID BASE", "ANDROID BASE 1.1", "CUPCAKE", "DONUT", "ECLAIR", "ECLAIR_0_1", "ECLAIR_MR1", "FROYO", "GINGERBREAD", "GINGERBREAD_MR1", "HONEYCOMB", "HONEYCOMB_MR1", "HONEYCOMB_MR2", "ICE_CREAM_SANDWICH", "ICE_CREAM_SANDWICH_MR1", "JELLY_BEAN", "JELLY_BEAN", "JELLY_BEAN", "KITKAT", "KITKAT", "LOLLIPOOP", "LOLLIPOOP_MR1", "MARSHMALLOW", "NOUGAT", "NOUGAT", "OREO", "OREO", "PIE", "", "")
            val index = apiNumber - 1
            apiName = if (index < mapper.size) mapper[index] else "UNKNOWN_VERSION"
        }
        if (version.isEmpty()) {
            val versions = arrayOf("1.0", "1.1", "1.5", "1.6", "2.0", "2.0.1", "2.1", "2.2.X", "2.3", "2.3.3", "3.0", "3.1", "3.2.0", "4.0.1", "4.0.3", "4.1.0", "4.2.0", "4.3.0", "4.4", "4.4", "5.0", "5.1", "6.0", "7.0", "7.1", "8.0.0", "8.1.0", "9", "10", "11")
            val index = apiNumber - 1
            version = if (index < versions.size) versions[index] else "UNKNOWN_VERSION"
        }

        builder.append("\n\t")
        if (apiName.isNotEmpty())
            builder.append(apiName).append(" ")
        if (version.isNotEmpty())
            builder.append(version).append(" ")
        builder.append("(API ").append(apiNumber).append(")")
        builder.append("\n")
        return builder.toString()
    }

    companion object {

        private const val MIN_REQUIRED_VERSION_ARG = "MIN_REQUIRED_VERSION_ARG"

        fun getInstance(minVersionRequired: Int): UnavailableFeatureDialogFragment {
            val fragment = UnavailableFeatureDialogFragment()
            val args = Bundle()
            args.putInt(MIN_REQUIRED_VERSION_ARG, minVersionRequired)
            fragment.arguments = args
            return fragment
        }
    }
}
