/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.fingerprint;

import android.app.Dialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;

import com.kunzisoft.keepass.R;

@RequiresApi(api = Build.VERSION_CODES.M)
public class FingerPrintExplanationDialog extends DialogFragment {

    private FingerPrintAnimatedVector fingerPrintAnimatedVector;

    @NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
        assert getActivity() != null;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View rootView = inflater.inflate(R.layout.fingerprint_explanation, null);

        View fingerprintSettingWayTextView = rootView.findViewById(R.id.fingerprint_setting_way_text);
        fingerprintSettingWayTextView.setOnClickListener(
                view -> startActivity(new Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)));

        fingerPrintAnimatedVector =
                new FingerPrintAnimatedVector(getActivity(),
                        rootView.findViewById(R.id.fingerprint_image));

        builder.setView(rootView)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {

                });
        return builder.create();
	}

    @Override
    public void onResume() {
        super.onResume();
        fingerPrintAnimatedVector.startScan();
    }

    @Override
    public void onPause() {
        super.onPause();
        fingerPrintAnimatedVector.stopScan();
    }
}
