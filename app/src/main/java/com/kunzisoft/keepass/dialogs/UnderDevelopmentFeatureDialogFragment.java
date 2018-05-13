/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.dialogs;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.widget.Toast;

import com.kunzisoft.keepass.BuildConfig;
import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.utils.Util;

/**
 * Custom Dialog that asks the user to download the pro version or make a donation.
 */
public class UnderDevelopmentFeatureDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        assert getActivity() != null;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
        if (BuildConfig.CLOSED_STORE) {
            if (BuildConfig.FULL_VERSION) {
                stringBuilder.append(Html.fromHtml(getString(R.string.html_text_dev_feature_thanks))).append("\n\n")
                        .append(Html.fromHtml(getString(R.string.html_rose))).append("\n\n")
                        .append(Html.fromHtml(getString(R.string.html_text_dev_feature_work_hard))).append("\n")
                        .append(Html.fromHtml(getString(R.string.html_text_dev_feature_upgrade))).append(" ");
                builder.setPositiveButton(android.R.string.ok, (dialog, id) -> dismiss());
            } else {
                stringBuilder.append(Html.fromHtml(getString(R.string.html_text_dev_feature))).append("\n\n")
                        .append(Html.fromHtml(getString(R.string.html_text_dev_feature_buy_pro))).append("\n")
                        .append(Html.fromHtml(getString(R.string.html_text_dev_feature_encourage)));
                builder.setPositiveButton(R.string.download, (dialog, id) -> {
                    try {
                        Util.gotoUrl(getContext(), R.string.app_pro_url);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(getContext(), R.string.error_failed_to_launch_link, Toast.LENGTH_LONG).show();
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, (dialog, id) -> dismiss());
            }
        }
        else {
            stringBuilder.append(Html.fromHtml(getString(R.string.html_text_dev_feature))).append("\n\n")
                    .append(Html.fromHtml(getString(R.string.html_text_dev_feature_contibute))).append(" ")
                    .append(Html.fromHtml(getString(R.string.html_text_dev_feature_encourage)));
            builder.setPositiveButton(R.string.contribute, (dialog, id) -> {
                try {
                    Util.gotoUrl(getContext(), R.string.contribution_url);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getContext(), R.string.error_failed_to_launch_link, Toast.LENGTH_LONG).show();
                }
            });
            builder.setNegativeButton(android.R.string.cancel, (dialog, id) -> dismiss());
        }
        builder.setMessage(stringBuilder);
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
