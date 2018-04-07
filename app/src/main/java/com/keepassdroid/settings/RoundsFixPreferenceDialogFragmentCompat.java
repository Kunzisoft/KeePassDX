/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass Libre.
 *
 *  KeePass Libre is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass Libre is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass Libre.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.settings;

import android.os.Bundle;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import tech.jgross.keepass.R;

public class RoundsFixPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat {

    private TextView mRoundsView;

    public static RoundsFixPreferenceDialogFragmentCompat newInstance(
            String key) {
        final RoundsFixPreferenceDialogFragmentCompat
                fragment = new RoundsFixPreferenceDialogFragmentCompat();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        TextView textDescriptionView = view.findViewById(R.id.rounds_explanation);
        mRoundsView = view.findViewById(R.id.rounds);

        DialogPreference preference = getPreference();
        if (preference instanceof RoundsPreference) {
            textDescriptionView.setText(((RoundsPreference) preference).getExplanations());
            long numRounds = ((RoundsPreference) preference).getRounds();
            mRoundsView.setText(String.valueOf(numRounds));
        }
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if ( positiveResult ) {
            long rounds;
            try {
                String strRounds = mRoundsView.getText().toString();
                rounds = Long.valueOf(strRounds);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), R.string.error_rounds_not_number, Toast.LENGTH_LONG).show();
                return;
            }

            DialogPreference preference = getPreference();
            if (preference instanceof RoundsPreference) {
                RoundsPreference roundsPreference = (RoundsPreference) preference;
                // This allows the client to ignore the user value.
                if (roundsPreference.callChangeListener(rounds)) {
                    // Save the value
                    roundsPreference.setRounds(rounds);
                }
            }
        }
    }
}
