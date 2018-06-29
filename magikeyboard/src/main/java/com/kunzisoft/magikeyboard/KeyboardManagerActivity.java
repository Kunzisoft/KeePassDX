package com.kunzisoft.magikeyboard;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

/**
 * Activity to show keyboard manager
 */
public class KeyboardManagerActivity extends AppCompatActivity {

    private View rootView;

    enum DialogState {
        PICKING, CHOSEN
    }

    private DialogState mState;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.empty);
        rootView = findViewById(R.id.root_view);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(mState == DialogState.PICKING) {
            mState = DialogState.CHOSEN;
        }
        else if(mState == DialogState.CHOSEN) {
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        rootView.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imeManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imeManager != null) {
                    imeManager.showInputMethodPicker();
                }
                mState = DialogState.PICKING;
            }
        }, 100);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Close the back activity
        finish();
    }
}
