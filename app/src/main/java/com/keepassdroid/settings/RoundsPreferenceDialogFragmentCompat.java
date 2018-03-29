package com.keepassdroid.settings;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.keepassdroid.database.Database;
import com.keepassdroid.tasks.ProgressTask;
import com.keepassdroid.app.App;
import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.edit.OnFinish;
import com.keepassdroid.database.edit.SaveDB;
import com.kunzisoft.keepass.R;

public class RoundsPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat {

    private PwDatabase mPM;
    private TextView mRoundsView;

    public static RoundsPreferenceDialogFragmentCompat newInstance(
            String key) {
        final RoundsPreferenceDialogFragmentCompat
                fragment = new RoundsPreferenceDialogFragmentCompat();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if ( positiveResult ) {
            long rounds;

            try {
                String strRounds = mRoundsView.getText().toString();
                rounds = Long.parseLong(strRounds);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), R.string.error_rounds_not_number, Toast.LENGTH_LONG).show();
                return;
            }

            if ( rounds < 1 ) {
                rounds = 1;
            }

            long oldRounds = mPM.getNumRounds();
            try {
                mPM.setNumRounds(rounds);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), R.string.error_rounds_too_large, Toast.LENGTH_LONG).show();
                mPM.setNumRounds(Integer.MAX_VALUE);
            }

            Handler handler = new Handler();
            SaveDB save = new SaveDB(getContext(), App.getDB(), new AfterSave(getContext(), handler, oldRounds));
            ProgressTask pt = new ProgressTask(getContext(), save, R.string.saving_database);
            pt.run();

        }
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mRoundsView = view.findViewById(R.id.rounds);

        // Get the time from the related Preference
        Database db = App.getDB();
        mPM = db.getPwDatabase();
        long numRounds = mPM.getNumRounds();

        DialogPreference preference = getPreference();
        if (preference instanceof RoundsPreference) {
            numRounds = ((RoundsPreference) preference).getRounds();
        }

        mRoundsView.setText(String.valueOf(numRounds));
    }

    private class AfterSave extends OnFinish {
        private long mOldRounds;
        private Context mCtx;

        public AfterSave(Context ctx, Handler handler, long oldRounds) {
            super(handler);

            mCtx = ctx;
            mOldRounds = oldRounds;
        }

        @Override
        public void run() {
            if (!mSuccess) {
                displayMessage(mCtx);
                mPM.setNumRounds(mOldRounds);
            }

            super.run();
        }
    }
}
