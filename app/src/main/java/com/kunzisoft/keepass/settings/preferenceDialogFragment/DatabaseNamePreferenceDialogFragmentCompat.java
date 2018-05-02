package com.kunzisoft.keepass.settings.preferenceDialogFragment;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.kunzisoft.keepass.database.action.OnFinishRunnable;
import com.kunzisoft.keepass.tasks.SaveDatabaseProgressTaskDialogFragment;

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
            setAfterSaveDatabase(new AfterNameSave((AppCompatActivity) getActivity(), handler, dbName, oldName));
        }

        super.onDialogClosed(positiveResult);
    }

    private class AfterNameSave extends OnFinishRunnable {

        private String mNewName;
        private String mOldName;
        private AppCompatActivity mActivity;

        AfterNameSave(AppCompatActivity ctx, Handler handler, String newName, String oldName) {
            super(handler);

            mActivity = ctx;
            mNewName = newName;
            mOldName = oldName;
        }

        @Override
        public void run() {
            String nameToShow = mNewName;

            if (!mSuccess) {
                displayMessage(mActivity);
                database.assignName(mOldName);
            }


            if (mActivity != null) {
                mActivity.runOnUiThread(() -> {
                    getPreference().setSummary(nameToShow);
                    SaveDatabaseProgressTaskDialogFragment.stop(mActivity);
                });
            }

            super.run();
        }
    }
}
