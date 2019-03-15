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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.database.element.PwEncryptionAlgorithm;
import com.kunzisoft.keepass.tasks.ActionRunnable;
import com.kunzisoft.keepass.settings.preferencedialogfragment.adapter.ListRadioItemAdapter;

import org.jetbrains.annotations.Nullable;

public class DatabaseEncryptionAlgorithmPreferenceDialogFragmentCompat extends DatabaseSavePreferenceDialogFragmentCompat
        implements ListRadioItemAdapter.RadioItemSelectedCallback<PwEncryptionAlgorithm> {

    private PwEncryptionAlgorithm algorithmSelected;

    public static DatabaseEncryptionAlgorithmPreferenceDialogFragmentCompat newInstance(
            String key) {
        final DatabaseEncryptionAlgorithmPreferenceDialogFragmentCompat
                fragment = new DatabaseEncryptionAlgorithmPreferenceDialogFragmentCompat();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        setExplanationText(R.string.encryption_explanation);

        RecyclerView recyclerView = view.findViewById(R.id.pref_dialog_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ListRadioItemAdapter<PwEncryptionAlgorithm> encryptionAlgorithmAdapter = new ListRadioItemAdapter<>(getActivity());
        encryptionAlgorithmAdapter.setRadioItemSelectedCallback(this);
        recyclerView.setAdapter(encryptionAlgorithmAdapter);

        algorithmSelected = getDatabase().getEncryptionAlgorithm();
        encryptionAlgorithmAdapter.setItems(getDatabase().getAvailableEncryptionAlgorithms(), algorithmSelected);
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if ( positiveResult
                && getDatabase().allowEncryptionAlgorithmModification()) {
            assert getContext() != null;

            if (algorithmSelected != null) {
                PwEncryptionAlgorithm newAlgorithm = algorithmSelected;
                PwEncryptionAlgorithm oldAlgorithm = getDatabase().getEncryptionAlgorithm();
                getDatabase().assignEncryptionAlgorithm(newAlgorithm);

                setAfterSaveDatabaseRunnable(new AfterDescriptionSave((AppCompatActivity) getActivity(), newAlgorithm, oldAlgorithm));
            }
        }

        super.onDialogClosed(positiveResult);
    }

    @Override
    public void onItemSelected(PwEncryptionAlgorithm item) {
        this.algorithmSelected = item;
    }

    private class AfterDescriptionSave extends ActionRunnable {

        private PwEncryptionAlgorithm mNewAlgorithm;
        private PwEncryptionAlgorithm mOldAlgorithm;
        private AppCompatActivity mActivity;

        AfterDescriptionSave(AppCompatActivity activity, PwEncryptionAlgorithm newAlgorithm, PwEncryptionAlgorithm oldAlgorithm) {
            super();

            mActivity = activity;
            mNewAlgorithm = newAlgorithm;
            mOldAlgorithm = oldAlgorithm;
        }

        @Override
        public void onFinishRun(boolean isSuccess, @Nullable String message) {
            if (mActivity != null) {
                mActivity.runOnUiThread(() -> {
                    PwEncryptionAlgorithm algorithmToShow = mNewAlgorithm;

                    if (!isSuccess) {
                        displayMessage(mActivity);
                        getDatabase().assignEncryptionAlgorithm(mOldAlgorithm);
                    }
                    getPreference().setSummary(algorithmToShow.getName(mActivity.getResources()));
                });
            }
        }
    }
}
