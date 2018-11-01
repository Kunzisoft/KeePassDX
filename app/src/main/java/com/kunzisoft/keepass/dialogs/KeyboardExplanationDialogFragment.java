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
package com.kunzisoft.keepass.dialogs;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;

import com.kunzisoft.keepass.BuildConfig;
import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.utils.Util;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class KeyboardExplanationDialogFragment extends DialogFragment {

    @NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
        assert getActivity() != null;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View rootView = inflater.inflate(R.layout.keyboard_explanation, null);

        View fingerprintSettingPath1TextView = rootView.findViewById(R.id.keyboards_activate_setting_path1_text);
        fingerprintSettingPath1TextView.setOnClickListener(
                view -> launchActivateKeyboardSetting());
        View fingerprintSettingPath2TextView = rootView.findViewById(R.id.keyboards_activate_setting_path2_text);
        fingerprintSettingPath2TextView.setOnClickListener(
                view -> launchActivateKeyboardSetting());

        View containerKeyboardSwitcher = rootView.findViewById(R.id.container_keyboard_switcher);
        if(BuildConfig.CLOSED_STORE) {
            containerKeyboardSwitcher.setOnClickListener(
                    view -> Util.gotoUrl(getContext(), R.string.keyboard_switcher_play_store));
        } else {
            containerKeyboardSwitcher.setOnClickListener(
                    view -> Util.gotoUrl(getContext(), R.string.keyboard_switcher_f_droid));
        }

        builder.setView(rootView)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {});
        return builder.create();
	}

    private void launchActivateKeyboardSetting() {
        Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
