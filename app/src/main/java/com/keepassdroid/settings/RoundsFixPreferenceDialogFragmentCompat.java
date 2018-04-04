package com.keepassdroid.settings;

import android.os.Bundle;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.kunzisoft.keepass.R;

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
