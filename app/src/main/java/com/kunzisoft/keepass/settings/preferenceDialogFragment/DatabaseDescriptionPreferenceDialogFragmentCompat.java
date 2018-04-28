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

import com.kunzisoft.keepass.database.edit.OnFinish;

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

        setInputText(database.getDescription());
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if ( positiveResult ) {
            assert getContext() != null;

            String newDescription = getInputText();
            String oldDescription = database.getDescription();
            database.assignDescription(newDescription);

            Handler handler = new Handler();
            setAfterSaveDatabase(new AfterDescriptionSave(getContext(), handler, newDescription, oldDescription));
        }

        super.onDialogClosed(positiveResult);
    }

    private class AfterDescriptionSave extends OnFinish {

        private String mNewDescription;
        private String mOldDescription;
        private Context mCtx;

        AfterDescriptionSave(Context ctx, Handler handler, String newDescription, String oldDescription) {
            super(handler);

            mCtx = ctx;
            mNewDescription = newDescription;
            mOldDescription = oldDescription;
        }

        @Override
        public void run() {
            String descriptionToShow = mNewDescription;

            if (!mSuccess) {
                displayMessage(mCtx);
                database.assignDescription(mOldDescription);
            }

            getPreference().setSummary(descriptionToShow);

            super.run();
        }
    }
}
