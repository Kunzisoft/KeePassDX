package com.kunzisoft.keepass.settings.preferenceDialogFragment;

import android.view.View;
import android.widget.EditText;

import com.kunzisoft.keepass.R;

public class InputDatabaseSavePreferenceDialogFragmentCompat extends DatabaseSavePreferenceDialogFragmentCompat {

    private EditText inputTextView;

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        inputTextView = view.findViewById(R.id.input_text);
    }

    public String getInputText() {
        return this.inputTextView.getText().toString();
    }

    public void setInputText(String inputText) {
        if (inputTextView != null && inputText != null) {
            this.inputTextView.setText(inputText);
            this.inputTextView.setSelection(this.inputTextView.getText().length());
        }
    }
}
