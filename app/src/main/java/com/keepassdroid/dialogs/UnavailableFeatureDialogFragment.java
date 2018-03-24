package com.keepassdroid.dialogs;

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
                .append("\n");
        if(Build.VERSION.SDK_INT < minVersionRequired) {
            message.append(getString(R.string.unavailable_feature_version,
                    androidNameFromApiNumber(Build.VERSION.SDK_INT),
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
        StringBuilder builder = new StringBuilder();
        Field[] fields = Build.VERSION_CODES.class.getFields();
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
                builder.append(fieldName).append(" ");
                break;
            }
        }
        builder.append("(API ");
        builder.append(apiNumber).append(")");
        return builder.toString();
    }
}
