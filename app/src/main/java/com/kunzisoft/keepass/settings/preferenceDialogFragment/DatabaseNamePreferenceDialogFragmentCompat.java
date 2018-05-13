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

import com.kunzisoft.keepass.database.action.OnFinishRunnable;
import com.kunzisoft.keepass.tasks.SaveDatabaseProgressTaskDialogFragment;

public class DatabaseNamePreferenceDialogFragmentCompat extends InputDatabaseSavePreferenceDialogFragmentCompat {

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

            String newName = getInputText();
            String oldName = database.getName();
            database.assignName(newName);

            Handler handler = new Handler();
            setAfterSaveDatabase(new AfterNameSave((AppCompatActivity) getActivity(), handler, newName, oldName));
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
            if (mActivity != null) {
                mActivity.runOnUiThread(() -> {
                    String nameToShow = mNewName;

                    if (!mSuccess) {
                        displayMessage(mActivity);
                        database.assignName(mOldName);
                    }

                    getPreference().setSummary(nameToShow);
                    SaveDatabaseProgressTaskDialogFragment.stop(mActivity);
                });
            }

            super.run();
        }
    }
}
