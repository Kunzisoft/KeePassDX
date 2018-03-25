/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
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
package com.keepassdroid.fileselect;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.keepassdroid.utils.Util;
import com.kunzisoft.keepass.R;

public class BrowserDialog extends DialogFragment {

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		// Get the layout inflater
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View root = inflater.inflate(R.layout.browser_install, null);
		builder.setView(root)
				.setNegativeButton(R.string.cancel, (dialog, id) -> { });

		Button market = root.findViewById(R.id.install_market);
		market.setOnClickListener((view) -> {
			Util.gotoUrl(getContext(), R.string.filemanager_play_store);
			dismiss();
		});

		Button web = root.findViewById(R.id.install_web);
		web.setOnClickListener(view -> {
            Util.gotoUrl(getContext(), R.string.filemanager_f_droid);
            dismiss();
        });

		return builder.create();
	}

}
