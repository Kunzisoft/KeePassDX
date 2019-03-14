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
import android.support.v7.preference.Preference;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.crypto.keyDerivation.KdfEngine;
import com.kunzisoft.keepass.tasks.ActionRunnable;
import com.kunzisoft.keepass.settings.preferencedialogfragment.adapter.ListRadioItemAdapter;

import org.jetbrains.annotations.Nullable;

public class DatabaseKeyDerivationPreferenceDialogFragmentCompat extends DatabaseSavePreferenceDialogFragmentCompat
        implements ListRadioItemAdapter.RadioItemSelectedCallback<KdfEngine> {

    private KdfEngine kdfEngineSelected;
    private Preference roundPreference;
    private Preference memoryPreference;
    private Preference parallelismPreference;

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

        setExplanationText(R.string.kdf_explanation);

        RecyclerView recyclerView = view.findViewById(R.id.pref_dialog_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ListRadioItemAdapter<KdfEngine> kdfAdapter = new ListRadioItemAdapter<>(getActivity());
        kdfAdapter.setRadioItemSelectedCallback(this);
        recyclerView.setAdapter(kdfAdapter);

        kdfEngineSelected = getDatabase().getKdfEngine();
        kdfAdapter.setItems(getDatabase().getAvailableKdfEngines(), kdfEngineSelected);
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if ( positiveResult
                && getDatabase().allowKdfModification()) {
            assert getContext() != null;

            if (kdfEngineSelected != null) {
                KdfEngine newKdfEngine = kdfEngineSelected;
                KdfEngine oldKdfEngine = getDatabase().getKdfEngine();
                getDatabase().assignKdfEngine(newKdfEngine);

                setAfterSaveDatabaseRunnable(new AfterDescriptionSave((AppCompatActivity) getActivity(), newKdfEngine, oldKdfEngine));
            }
        }

        super.onDialogClosed(positiveResult);
    }

    public void setRoundPreference(Preference preference) {
        this.roundPreference = preference;
    }

    public void setMemoryPreference(Preference preference) {
        this.memoryPreference = preference;
    }

    public void setParallelismPreference(Preference preference) {
        this.parallelismPreference = preference;
    }

    @Override
    public void onItemSelected(KdfEngine item) {
        kdfEngineSelected = item;
    }

    private class AfterDescriptionSave extends ActionRunnable {

        private KdfEngine mNewKdfEngine;
        private KdfEngine mOldKdfEngine;
        private AppCompatActivity mActivity;

        AfterDescriptionSave(AppCompatActivity activity, KdfEngine newKdfEngine, KdfEngine oldKdfEngine) {
            super();

            this.mActivity = activity;
            this.mNewKdfEngine = newKdfEngine;
            this.mOldKdfEngine = oldKdfEngine;
        }

        @Override
        public void onFinishRun(boolean isSuccess, @Nullable String message) {
            if (mActivity != null) {
                mActivity.runOnUiThread(() -> {
                    KdfEngine kdfEngineToShow = mNewKdfEngine;

                    if (!isSuccess) {
                        displayMessage(mActivity);
                        getDatabase().assignKdfEngine(mOldKdfEngine);
                    }

                    getPreference().setSummary(kdfEngineToShow.getName(mActivity.getResources()));
                    if (roundPreference != null)
                        roundPreference.setSummary(String.valueOf(kdfEngineToShow.getDefaultKeyRounds()));

                    // Disable memory and parallelism if not available
                    if (memoryPreference != null) {
                        memoryPreference.setSummary(String.valueOf(kdfEngineToShow.getDefaultMemoryUsage()));
                    }
                    if (parallelismPreference != null) {
                        parallelismPreference.setSummary(String.valueOf(kdfEngineToShow.getDefaultParallelism()));
                    }
                });
            }
        }
    }
}
