package com.keepassdroid;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.kunzisoft.keepass.R;

public class UnavailableFeatureDialog extends DialogFragment {

    private static final String MIN_REQUIRED_VERSION_ARG = "MIN_REQUIRED_VERSION_ARG";
    private int minVersionRequired = Build.VERSION_CODES.M;

    public static UnavailableFeatureDialog getInstance(int minVersionRequired) {
        UnavailableFeatureDialog fragment = new UnavailableFeatureDialog();
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

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getString(R.string.unavailable_feature_text, Build.VERSION.SDK_INT, minVersionRequired))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) { }
                });
        return builder.create();
    }
}
