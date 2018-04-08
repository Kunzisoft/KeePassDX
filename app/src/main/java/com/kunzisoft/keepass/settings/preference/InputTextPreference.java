package com.kunzisoft.keepass.settings.preference;

import android.content.Context;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;

import tech.jgross.keepass.R;

public class InputTextPreference extends DialogPreference {

    public InputTextPreference(Context context) {
        this(context, null);
    }

    public InputTextPreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.dialogPreferenceStyle);
    }

    public InputTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, defStyleAttr);
    }

    public InputTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public int getDialogLayoutResource() {
        return R.layout.pref_dialog_input_text;
    }
}
