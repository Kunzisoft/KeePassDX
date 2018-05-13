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

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.database.action.OnFinishRunnable;
import com.kunzisoft.keepass.tasks.SaveDatabaseProgressTaskDialogFragment;

public class ParallelismPreferenceDialogFragmentCompat extends InputDatabaseSavePreferenceDialogFragmentCompat {

    public static ParallelismPreferenceDialogFragmentCompat newInstance(
            String key) {
        final ParallelismPreferenceDialogFragmentCompat
                fragment = new ParallelismPreferenceDialogFragmentCompat();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        setExplanationText(R.string.parallelism_explanation);
        setInputText(database.getParallelismAsString());
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if ( positiveResult ) {
            assert getContext() != null;
            int parallelism;

            try {
                String stringParallelism = getInputText();
                parallelism = Integer.parseInt(stringParallelism);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), R.string.error_rounds_not_number, Toast.LENGTH_LONG).show(); // TODO change error
                return;
            }

            if ( parallelism < 1 ) {
                parallelism = 1;
            }

            int oldParallelism = database.getParallelism();
            database.setParallelism(parallelism);

            Handler handler = new Handler();
            setAfterSaveDatabase(new AfterParallelismSave((AppCompatActivity) getActivity(), handler, parallelism, oldParallelism));
        }

        super.onDialogClosed(positiveResult);
    }

    private class AfterParallelismSave extends OnFinishRunnable {

        private int mNewParallelism;
        private int mOldParallelism;
        private AppCompatActivity mActivity;

        AfterParallelismSave(AppCompatActivity ctx, Handler handler, int newParallelism, int oldParallelism) {
            super(handler);

            mActivity = ctx;
            mNewParallelism = newParallelism;
            mOldParallelism = oldParallelism;
        }

        @Override
        public void run() {
            if (mActivity != null) {
                mActivity.runOnUiThread(() -> {
                    int parallelismToShow = mNewParallelism;

                    if (!mSuccess) {
                        displayMessage(mActivity);
                        database.setParallelism(mOldParallelism);
                    }

                    getPreference().setSummary(String.valueOf(parallelismToShow));
                    SaveDatabaseProgressTaskDialogFragment.stop(mActivity);
                });
            }

            super.run();
        }
    }
}
