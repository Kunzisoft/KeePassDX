/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.settings;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.app.App;
import com.kunzisoft.keepass.database.PwDatabase;
import com.kunzisoft.keepass.database.edit.OnFinish;
import com.kunzisoft.keepass.database.edit.SaveDB;
import com.kunzisoft.keepass.tasks.ProgressTask;

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
