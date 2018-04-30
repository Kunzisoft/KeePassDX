package com.kunzisoft.keepass.settings.preferenceDialogFragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import com.kunzisoft.keepass.database.action.OnFinishRunnable;

public class DatabaseDescriptionPreferenceDialogFragmentCompat extends DatabaseSavePreferenceDialogFragmentCompat {

    public static DatabaseDescriptionPreferenceDialogFragmentCompat newInstance(
            String key) {
        final DatabaseDescriptionPreferenceDialogFragmentCompat
                fragment = new DatabaseDescriptionPreferenceDialogFragmentCompat();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        setInputText(database.getDescription());
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if ( positiveResult ) {
            assert getContext() != null;

            String dbDescription = getInputText();
            String oldDescription = database.getDescription();
            database.assignDescription(dbDescription);

            Handler handler = new Handler();
            setAfterSaveDatabase(new AfterDescriptionSave(getContext(), handler, dbDescription, oldDescription));
        }

        super.onDialogClosed(positiveResult);
    }

    private class AfterDescriptionSave extends OnFinishRunnable {

        private String mNewDescription;
        private String mOldDescription;
        private Context mCtx;

        AfterDescriptionSave(Context ctx, Handler handler, String newDescription, String oldDescription) {
            super(handler);

            mCtx = ctx;
            mNewDescription = newDescription;
            mOldDescription = oldDescription;
        }

        @Override
        public void run() {
            String descriptionToShow = mNewDescription;

            if (!mSuccess) {
                displayMessage(mCtx);
                database.assignDescription(mOldDescription);
                database.assignDescription(mOldDescription);
            }

            getPreference().setSummary(descriptionToShow);

            super.run();
        }
    }
}
