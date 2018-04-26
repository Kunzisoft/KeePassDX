package com.kunzisoft.keepass.settings.preferenceDialogFragment;

import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.view.View;
import android.widget.TextView;

import com.kunzisoft.keepass.R;

public abstract class InputPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat {

    private TextView textExplanationView;

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        textExplanationView = view.findViewById(R.id.explanation_text);
    }

    public String getExplanationText() {
        if (textExplanationView != null)
            return textExplanationView.getText().toString();
        else
            return "";
    }

    public void setExplanationText(String explanationText) {
        if (textExplanationView != null && explanationText != null)
            this.textExplanationView.setText(explanationText);
    }
}
