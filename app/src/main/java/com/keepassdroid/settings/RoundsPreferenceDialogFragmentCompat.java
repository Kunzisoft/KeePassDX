package com.keepassdroid.settings;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.keepassdroid.app.App;
import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.edit.OnFinish;
import com.keepassdroid.database.edit.SaveDB;
import tech.jgross.keepass.R;
import com.keepassdroid.tasks.ProgressTask;

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
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mRoundsView = view.findViewById(R.id.rounds);

        // Get the number or rounds from the related Preference
        mPM = App.getDB().getPwDatabase();
        long numRounds = mPM.getNumberKeyEncryptionRounds();

        mRoundsView.setText(String.valueOf(numRounds));
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if ( positiveResult ) {
            assert getContext() != null;
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

            long oldRounds = mPM.getNumberKeyEncryptionRounds();
            try {
                mPM.setNumberKeyEncryptionRounds(rounds);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), R.string.error_rounds_too_large, Toast.LENGTH_LONG).show();
                mPM.setNumberKeyEncryptionRounds(Integer.MAX_VALUE);
            }

            Handler handler = new Handler();
            SaveDB save = new SaveDB(getContext(), App.getDB(), new AfterSave(getContext(), handler, rounds, oldRounds));
            ProgressTask pt = new ProgressTask(getContext(), save, R.string.saving_database);
            pt.run();
        }
    }

    private class AfterSave extends OnFinish {

        private long mNewRounds;
        private long mOldRounds;
        private Context mCtx;

        public AfterSave(Context ctx, Handler handler, long newRounds, long oldRounds) {
            super(handler);

            mCtx = ctx;
            mNewRounds = newRounds;
            mOldRounds = oldRounds;
        }

        @Override
        public void run() {
            long roundsToShow = mNewRounds;

            if (!mSuccess) {
                displayMessage(mCtx);
                mPM.setNumberKeyEncryptionRounds(mOldRounds);
            }

            getPreference().setSummary(String.valueOf(roundsToShow));

            super.run();
        }
    }
}
