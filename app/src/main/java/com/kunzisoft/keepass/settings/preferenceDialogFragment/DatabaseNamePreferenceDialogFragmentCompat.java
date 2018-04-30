package com.kunzisoft.keepass.settings.preferenceDialogFragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import com.kunzisoft.keepass.database.action.OnFinishRunnable;

public class DatabaseNamePreferenceDialogFragmentCompat extends DatabaseSavePreferenceDialogFragmentCompat {

    public static DatabaseNamePreferenceDialogFragmentCompat newInstance(
            String key) {
        final DatabaseNamePreferenceDialogFragmentCompat
                fragment = new DatabaseNamePreferenceDialogFragmentCompat();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        setInputText(database.getName());
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if ( positiveResult ) {
            assert getContext() != null;

            String dbName = getInputText();
            String oldName = database.getName();
            database.assignName(dbName);

            Handler handler = new Handler();
            setAfterSaveDatabase(new AfterNameSave(getContext(), handler, dbName, oldName));
        }

        super.onDialogClosed(positiveResult);
    }

    private class AfterNameSave extends OnFinishRunnable {

        private String mNewName;
        private String mOldName;
        private Context mCtx;

        AfterNameSave(Context ctx, Handler handler, String newName, String oldName) {
            super(handler);

            mCtx = ctx;
            mNewName = newName;
            mOldName = oldName;
        }

        @Override
        public void run() {
            String nameToShow = mNewName;

            if (!mSuccess) {
                displayMessage(mCtx);
                database.assignName(mOldName);
            }

            getPreference().setSummary(nameToShow);

            super.run();
        }
    }
}
