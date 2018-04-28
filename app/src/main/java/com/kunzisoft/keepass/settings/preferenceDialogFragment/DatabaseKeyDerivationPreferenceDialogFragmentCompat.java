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
import android.support.v7.preference.Preference;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.crypto.keyDerivation.KdfEngine;
import com.kunzisoft.keepass.database.edit.OnFinish;
import com.kunzisoft.keepass.settings.preferenceDialogFragment.adapter.ListRadioItemAdapter;

public class DatabaseKeyDerivationPreferenceDialogFragmentCompat extends DatabaseSavePreferenceDialogFragmentCompat
        implements ListRadioItemAdapter.RadioItemSelectedCallback<KdfEngine> {

    private KdfEngine kdfEngineSelected;
    private Preference roundPreference;

    public static DatabaseKeyDerivationPreferenceDialogFragmentCompat newInstance(
            String key) {
        final DatabaseKeyDerivationPreferenceDialogFragmentCompat
                fragment = new DatabaseKeyDerivationPreferenceDialogFragmentCompat();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        RecyclerView recyclerView = view.findViewById(R.id.pref_dialog_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ListRadioItemAdapter<KdfEngine> kdfAdapter = new ListRadioItemAdapter<>(getActivity());
        kdfAdapter.setRadioItemSelectedCallback(this);
        recyclerView.setAdapter(kdfAdapter);

        kdfEngineSelected = database.getKdfEngine();
        kdfAdapter.setItems(database.getAvailableKdfEngines(), kdfEngineSelected);
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if ( positiveResult ) {
            assert getContext() != null;

            if (kdfEngineSelected != null) {
                KdfEngine newKdfEngine = kdfEngineSelected;
                KdfEngine oldKdfEngine = database.getKdfEngine();
                database.assignKdfEngine(newKdfEngine);

                Handler handler = new Handler();
                setAfterSaveDatabase(new AfterDescriptionSave(getContext(), handler, newKdfEngine, oldKdfEngine));
            }
        }

        super.onDialogClosed(positiveResult);
    }

    public void setRoundPreference(Preference preference) {
        this.roundPreference = preference;
    }

    @Override
    public void onItemSelected(KdfEngine item) {
        kdfEngineSelected = item;
    }

    private class AfterDescriptionSave extends OnFinish {

        private KdfEngine mNewKdfEngine;
        private KdfEngine mOldKdfEngine;
        private Context mCtx;

        AfterDescriptionSave(Context ctx, Handler handler, KdfEngine newKdfEngine, KdfEngine oldKdfEngine) {
            super(handler);

            this.mCtx = ctx;
            this.mNewKdfEngine = newKdfEngine;
            this.mOldKdfEngine = oldKdfEngine;
        }

        @Override
        public void run() {
            KdfEngine kdfEngineToShow = mNewKdfEngine;

            if (!mSuccess) {
                displayMessage(mCtx);
                database.assignKdfEngine(mOldKdfEngine);
            }

            getPreference().setSummary(kdfEngineToShow.getName(mCtx.getResources()));
            roundPreference.setSummary(String.valueOf(kdfEngineToShow.getDefaultKeyRounds()));

            super.run();
        }
    }
}
