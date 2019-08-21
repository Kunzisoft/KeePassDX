/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.activities.dialogs

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.text.Html
import android.text.SpannableStringBuilder
import com.kunzisoft.keepass.BuildConfig
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.utils.UriUtil

/**
 * Custom Dialog that asks the user to download the pro version or make a donation.
 */
class ProFeatureDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(activity)

            val stringBuilder = SpannableStringBuilder()
            if (BuildConfig.CLOSED_STORE) {
                // TODO HtmlCompat with androidX
                stringBuilder.append(Html.fromHtml(getString(R.string.html_text_ad_free))).append("\n\n")
                stringBuilder.append(Html.fromHtml(getString(R.string.html_text_buy_pro)))
                builder.setPositiveButton(R.string.download) { _, _ ->
                    UriUtil.gotoUrl(context!!, R.string.app_pro_url)
                }
            } else {
                stringBuilder.append(Html.fromHtml(getString(R.string.html_text_feature_generosity))).append("\n\n")
                stringBuilder.append(Html.fromHtml(getString(R.string.html_text_donation)))
                builder.setPositiveButton(R.string.contribute) { _, _ ->
                    UriUtil.gotoUrl(context!!, R.string.contribution_url)
                }
            }
            builder.setMessage(stringBuilder)
            builder.setNegativeButton(android.R.string.cancel) { _, _ -> dismiss() }
            // Create the AlertDialog object and return it
            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }
}
