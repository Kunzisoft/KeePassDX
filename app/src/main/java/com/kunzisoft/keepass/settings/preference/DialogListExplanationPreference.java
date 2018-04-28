package com.kunzisoft.keepass.settings.preference;

import android.content.Context;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;

import com.kunzisoft.keepass.R;

public class DialogListExplanationPreference extends DialogPreference {

    public DialogListExplanationPreference(Context context) {
        this(context, null);
    }

    public DialogListExplanationPreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.dialogPreferenceStyle);
    }

    public DialogListExplanationPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, defStyleAttr);
    }

    public DialogListExplanationPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public int getDialogLayoutResource() {
        return R.layout.pref_dialog_list_explanation;
    }
}
