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

        setInputText(getDatabase().getName());
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if ( positiveResult ) {
            assert getContext() != null;

            String newName = getInputText();
            String oldName = getDatabase().getName();
            getDatabase().assignName(newName);

            setAfterSaveDatabaseRunnable(new AfterNameSave((AppCompatActivity) getActivity(), newName, oldName));
        }

        super.onDialogClosed(positiveResult);
    }

    private class AfterNameSave extends ActionRunnable {

        private String mNewName;
        private String mOldName;
        private AppCompatActivity mActivity;

        AfterNameSave(AppCompatActivity ctx, String newName, String oldName) {
            super();

            mActivity = ctx;
            mNewName = newName;
            mOldName = oldName;
        }

		@Override
		public void onFinishRun(boolean isSuccess, @Nullable String message) {
			if (mActivity != null) {
				mActivity.runOnUiThread(() -> {
					String nameToShow = mNewName;

					if (!isSuccess) {
						displayMessage(mActivity);
						getDatabase().assignName(mOldName);
					}

					getPreference().setSummary(nameToShow);
				});
			}
		}
	}
}
