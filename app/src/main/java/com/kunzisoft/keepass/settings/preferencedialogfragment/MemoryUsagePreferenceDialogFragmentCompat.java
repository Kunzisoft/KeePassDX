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
package com.kunzisoft.keepass.settings.preferencedialogfragment;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.tasks.ActionRunnable;

public class MemoryUsagePreferenceDialogFragmentCompat extends InputDatabaseSavePreferenceDialogFragmentCompat {

    public static MemoryUsagePreferenceDialogFragmentCompat newInstance(
            String key) {
        final MemoryUsagePreferenceDialogFragmentCompat
                fragment = new MemoryUsagePreferenceDialogFragmentCompat();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        setExplanationText(R.string.memory_usage_explanation);
        setInputText(getDatabase().getMemoryUsageAsString());
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if ( positiveResult ) {
            assert getContext() != null;
            long memoryUsage;

            try {
                String stringMemory = getInputText();
                memoryUsage = Long.parseLong(stringMemory);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), R.string.error_rounds_not_number, Toast.LENGTH_LONG).show(); // TODO change error
                return;
            }

            if ( memoryUsage < 1 ) {
                memoryUsage = 1;
            }

            long oldMemoryUsage = getDatabase().getMemoryUsage();
            getDatabase().setMemoryUsage(memoryUsage);

            setAfterSaveDatabaseRunnable(new AfterMemorySave((AppCompatActivity) getActivity(), memoryUsage, oldMemoryUsage));
        }

        super.onDialogClosed(positiveResult);
    }

    private class AfterMemorySave extends ActionRunnable {

        private long mNewMemory;
        private long mOldMemory;
        private AppCompatActivity mActivity;

        AfterMemorySave(AppCompatActivity ctx, long newMemory, long oldMemory) {
            super();

            mActivity = ctx;
            mNewMemory = newMemory;
            mOldMemory = oldMemory;
        }

        @Override
        public void onFinishRun(boolean isSuccess, String message) {
            if (mActivity != null) {
                mActivity.runOnUiThread(() -> {
                    long memoryToShow = mNewMemory;

                    if (!isSuccess) {
                        displayMessage(mActivity);
                        getDatabase().setMemoryUsage(mOldMemory);
                    }

                    getPreference().setSummary(String.valueOf(memoryToShow));
                });
            }
        }
    }
}
