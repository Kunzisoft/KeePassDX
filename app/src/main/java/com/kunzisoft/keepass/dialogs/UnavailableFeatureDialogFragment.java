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
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.kunzisoft.keepass.R;

import java.lang.reflect.Field;

public class UnavailableFeatureDialogFragment extends DialogFragment {

    private static final String MIN_REQUIRED_VERSION_ARG = "MIN_REQUIRED_VERSION_ARG";
    private int minVersionRequired = Build.VERSION_CODES.M;

    public static UnavailableFeatureDialogFragment getInstance(int minVersionRequired) {
        UnavailableFeatureDialogFragment fragment = new UnavailableFeatureDialogFragment();
        Bundle args = new Bundle();
        args.putInt(MIN_REQUIRED_VERSION_ARG, minVersionRequired);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getArguments() != null && getArguments().containsKey(MIN_REQUIRED_VERSION_ARG))
            minVersionRequired = getArguments().getInt(MIN_REQUIRED_VERSION_ARG);

        assert getActivity() != null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View rootView = inflater.inflate(R.layout.unavailable_feature, null);
        TextView messageView = rootView.findViewById(R.id.unavailable_feature_message);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        SpannableStringBuilder message = new SpannableStringBuilder();
        message.append(getString(R.string.unavailable_feature_text))
                .append("\n\n");
        if(Build.VERSION.SDK_INT < minVersionRequired) {
            message.append(getString(R.string.unavailable_feature_version,
                    androidNameFromApiNumber(Build.VERSION.SDK_INT, Build.VERSION.RELEASE),
                    androidNameFromApiNumber(minVersionRequired)));
            message.append("\n\n")
                    .append(Html.fromHtml("<a href=\"https://source.android.com/setup/build-numbers\">CodeNames</a>"));
        } else
            message.append(getString(R.string.unavailable_feature_hardware));

        messageView.setText(message);
        messageView.setMovementMethod(LinkMovementMethod.getInstance());

        builder.setView(rootView)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> { });
        return builder.create();
    }

    private String androidNameFromApiNumber(int apiNumber) {
        return androidNameFromApiNumber(apiNumber, "");
    }

    private String androidNameFromApiNumber(int apiNumber, String releaseVersion) {
        StringBuilder builder = new StringBuilder();
        Field[] fields = Build.VERSION_CODES.class.getFields();
        String apiName = "";
        for (Field field : fields) {
            String fieldName = field.getName();
            int fieldValue = -1;
            try {
                fieldValue = field.getInt(new Object());
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
            if (fieldValue == apiNumber) {
                apiName = fieldName;
            }
        }
        if (apiName.isEmpty()) {
            String[] mapper = new String[]{
                    "ANDROID BASE", "ANDROID BASE 1.1", "CUPCAKE", "DONUT",
                    "ECLAIR", "ECLAIR_0_1", "ECLAIR_MR1", "FROYO",
                    "GINGERBREAD", "GINGERBREAD_MR1", "HONEYCOMB", "HONEYCOMB_MR1",
                    "HONEYCOMB_MR2", "ICE_CREAM_SANDWICH", "ICE_CREAM_SANDWICH_MR1", "JELLY_BEAN",
                    "JELLY_BEAN", "JELLY_BEAN", "KITKAT", "KITKAT",
                    "LOLLIPOOP", "LOLLIPOOP_MR1", "MARSHMALLOW", "NOUGAT",
                    "NOUGAT", "OREO", "OREO"};
            int index = apiNumber - 1;
            apiName = index < mapper.length ? mapper[index] : "UNKNOWN_VERSION";
        }
        if (releaseVersion.isEmpty()) {
            String[] versions = new String[]{
                    "1.0", "1.1", "1.5", "1.6",
                    "2.0", "2.0.1", "2.1", "2.2.X",
                    "2.3", "2.3.3", "3.0", "3.1",
                    "3.2.0", "4.0.1", "4.0.3", "4.1.0",
                    "4.2.0", "4.3.0", "4.4", "4.4",
                    "5.0", "5.1", "6.0", "7.0",
                    "7.1", "8.0.0", "8.1.0"};
            int index = apiNumber - 1;
            releaseVersion = index < versions.length ? versions[index] : "UNKNOWN_VERSION";
        }

        builder.append("\n\t");
        if (!apiName.isEmpty())
            builder.append(apiName).append(" ");
        if (!releaseVersion.isEmpty())
            builder.append(releaseVersion).append(" ");
        builder.append("(API ").append(apiNumber).append(")");
        builder.append("\n");
        return builder.toString();
    }
}
