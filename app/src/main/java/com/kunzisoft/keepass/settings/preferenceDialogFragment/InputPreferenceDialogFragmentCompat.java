package com.kunzisoft.keepass.settings.preferenceDialogFragment;

import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.kunzisoft.keepass.R;

public abstract class InputPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat {

    private EditText inputTextView;
    private TextView textExplanationView;

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        inputTextView = view.findViewById(R.id.input_text);
        textExplanationView = view.findViewById(R.id.explanation_text);
    }

    public String getInputText() {
        return this.inputTextView.getText().toString();
    }

    public void setInputText(String inputText) {
        this.inputTextView.setText(inputText);
        this.inputTextView.setSelection(this.inputTextView.getText().length());
    }

    public String getExplanationText() {
        if (textExplanationView != null)
            return textExplanationView.getText().toString();
        else
            return "";
    }

    public void setExplanationText(String explanationText) {
        if (textExplanationView != null)
            this.textExplanationView.setText(explanationText);
    }
}
