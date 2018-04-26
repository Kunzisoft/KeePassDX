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
package com.kunzisoft.keepass.settings.preferenceDialogFragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.database.edit.OnFinish;

public class RoundsPreferenceDialogFragmentCompat extends InputDatabaseSavePreferenceDialogFragmentCompat {

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

        setExplanationText(getString(R.string.rounds_explanation));
        setInputText(database.getNumberKeyEncryptionRoundsAsString());
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if ( positiveResult ) {
            assert getContext() != null;
            long rounds;

            try {
                String strRounds = getInputText();
                rounds = Long.parseLong(strRounds);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), R.string.error_rounds_not_number, Toast.LENGTH_LONG).show();
                return;
            }

            if ( rounds < 1 ) {
                rounds = 1;
            }

            long oldRounds = database.getNumberKeyEncryptionRounds();
            try {
                database.setNumberKeyEncryptionRounds(rounds);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), R.string.error_rounds_too_large, Toast.LENGTH_LONG).show();
                database.setNumberKeyEncryptionRounds(Integer.MAX_VALUE);
            }

            Handler handler = new Handler();
            setAfterSaveDatabase(new AfterRoundSave(getContext(), handler, rounds, oldRounds));
        }

        super.onDialogClosed(positiveResult);
    }

    private class AfterRoundSave extends OnFinish {

        private long mNewRounds;
        private long mOldRounds;
        private Context mCtx;

        AfterRoundSave(Context ctx, Handler handler, long newRounds, long oldRounds) {
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
                database.setNumberKeyEncryptionRounds(mOldRounds);
            }

            getPreference().setSummary(String.valueOf(roundsToShow));

            super.run();
        }
    }
}
