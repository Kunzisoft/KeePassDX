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
import android.os.Bundle
import android.text.SpannableStringBuilder
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.utils.UriUtil.openUrl

/**
 * Custom Dialog that asks the user to download the pro version or make a donation.
 */
class UnderDevelopmentFeatureDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(activity)

            val stringBuilder = SpannableStringBuilder()
            /*
            if (activity.isContributingUser()) {
                stringBuilder.append(HtmlCompat.fromHtml(getString(R.string.html_text_dev_feature_thanks), HtmlCompat.FROM_HTML_MODE_LEGACY)).append("\n\n")
                        .append(HtmlCompat.fromHtml(getString(R.string.html_rose), HtmlCompat.FROM_HTML_MODE_LEGACY)).append("\n\n")
                        .append(HtmlCompat.fromHtml(getString(R.string.html_text_dev_feature_work_hard), HtmlCompat.FROM_HTML_MODE_LEGACY)).append("\n")
                        .append(HtmlCompat.fromHtml(getString(R.string.html_text_dev_feature_upgrade), HtmlCompat.FROM_HTML_MODE_LEGACY)).append(" ")
                builder.setPositiveButton(android.R.string.ok) { _, _ -> dismiss() }
            } else {
             */
                stringBuilder.append(HtmlCompat.fromHtml(getString(R.string.html_text_dev_feature), HtmlCompat.FROM_HTML_MODE_LEGACY)).append("\n\n")
                        .append(HtmlCompat.fromHtml(getString(R.string.html_text_dev_feature_contibute), HtmlCompat.FROM_HTML_MODE_LEGACY)).append(" ")
                        .append(HtmlCompat.fromHtml(getString(R.string.html_text_dev_feature_encourage), HtmlCompat.FROM_HTML_MODE_LEGACY))
                builder.setPositiveButton(R.string.contribute) { _, _ ->
                    context?.openUrl(R.string.contribution_url)
                }
            //}
            builder.setMessage(stringBuilder)
            // Create the AlertDialog object and return it
            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }
}
