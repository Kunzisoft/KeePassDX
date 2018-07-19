package com.kunzisoft.keepass.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.kunzisoft.keepass.settings.PreferencesUtil;

public class ReadOnlyHelper {

    public static final String READ_ONLY_KEY = "READ_ONLY_KEY";

    public static final boolean READ_ONLY_DEFAULT = false;

    public static boolean retrieveReadOnlyFromInstanceStateOrPreference(Context context, Bundle savedInstanceState) {
        boolean readOnly;
        if (savedInstanceState != null
                && savedInstanceState.containsKey(READ_ONLY_KEY)) {
            readOnly = savedInstanceState.getBoolean(READ_ONLY_KEY);
        } else {
            readOnly = PreferencesUtil.enableReadOnlyDatabase(context);
        }
        return readOnly;
    }

    public static boolean retrieveReadOnlyFromInstanceStateOrArguments(Bundle savedInstanceState, Bundle arguments) {
        boolean readOnly = READ_ONLY_DEFAULT;
        if (savedInstanceState != null
                && savedInstanceState.containsKey(READ_ONLY_KEY)) {
            readOnly = savedInstanceState.getBoolean(READ_ONLY_KEY);
        } else if (arguments != null
                    && arguments.containsKey(READ_ONLY_KEY)) {
                readOnly = arguments.getBoolean(READ_ONLY_KEY);
        }
        return readOnly;
    }

    public static boolean retrieveReadOnlyFromInstanceStateOrIntent(Bundle savedInstanceState, Intent intent) {
        boolean readOnly = READ_ONLY_DEFAULT;
        if (savedInstanceState != null
                && savedInstanceState.containsKey(READ_ONLY_KEY)) {
            readOnly = savedInstanceState.getBoolean(READ_ONLY_KEY);
        } else {
            if (intent != null)
                readOnly = intent.getBooleanExtra(READ_ONLY_KEY, READ_ONLY_DEFAULT);
        }
        return readOnly;
    }

    public static void putReadOnlyInIntent(Intent intent, boolean readOnly) {
        intent.putExtra(READ_ONLY_KEY, readOnly);
    }

    public static void putReadOnlyInBundle(Bundle bundle, boolean readOnly) {
        bundle.putBoolean(READ_ONLY_KEY, readOnly);
    }

    public static void onSaveInstanceState(Bundle outState, boolean readOnly) {
        outState.putBoolean(READ_ONLY_KEY, readOnly);
    }
}
