package com.kunzisoft.keepass.settings.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;

import tech.jgross.keepass.R;

public class InputTextExplanationPreference extends DialogPreference {

    protected String explanation;

    public InputTextExplanationPreference(Context context) {
        this(context, null);
    }

    public InputTextExplanationPreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.dialogPreferenceStyle);
    }

    public InputTextExplanationPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, defStyleAttr);
    }

    public InputTextExplanationPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.explanationDialog,
                0, 0);
        try {
            setExplanation(a.getString(R.styleable.explanationDialog_explanations));
        } finally {
            a.recycle();
        }
    }

    @Override
    public int getDialogLayoutResource() {
        return R.layout.pref_dialog_input_text_explanation;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
