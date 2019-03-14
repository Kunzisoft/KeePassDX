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

import com.kunzisoft.keepass.tasks.ActionRunnable;

import org.jetbrains.annotations.Nullable;

public class DatabaseDescriptionPreferenceDialogFragmentCompat extends InputDatabaseSavePreferenceDialogFragmentCompat {

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

        setInputText(getDatabase().getDescription());
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if ( positiveResult ) {
            assert getContext() != null;

            String newDescription = getInputText();
            String oldDescription = getDatabase().getDescription();
            getDatabase().assignDescription(newDescription);

            setAfterSaveDatabaseRunnable(new AfterDescriptionSave((AppCompatActivity) getActivity(), newDescription, oldDescription));
        }

        super.onDialogClosed(positiveResult);
    }

    private class AfterDescriptionSave extends ActionRunnable {

        private AppCompatActivity mActivity;
        private String mNewDescription;
        private String mOldDescription;

        AfterDescriptionSave(AppCompatActivity ctx, String newDescription, String oldDescription) {
            super();

            mActivity = ctx;
            mNewDescription = newDescription;
            mOldDescription = oldDescription;
        }

        @Override
        public void onFinishRun(boolean isSuccess, @Nullable String message) {
            if (mActivity != null) {
                mActivity.runOnUiThread(() -> {
                    String descriptionToShow = mNewDescription;

                    if (!isSuccess) {
                        displayMessage(mActivity);
                        getDatabase().assignDescription(mOldDescription);
                    }

                    getPreference().setSummary(descriptionToShow);
                });
            }
        }
    }
}
