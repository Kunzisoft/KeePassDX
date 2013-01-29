/*
 * Copyright 2013 Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.android.keepass.R;

public class BetaWarningDialog extends AlertDialog {

	public BetaWarningDialog(Context context) {
		super(context);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Context ctx = getContext();
		setMessage(ctx.getText(R.string.beta_warning));
		
		setButton(AlertDialog.BUTTON1, ctx.getText(android.R.string.ok), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dismiss();
			}
		});
		
		setButton(AlertDialog.BUTTON2, ctx.getText(R.string.beta_dontask), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Context ctx = getContext();
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
				SharedPreferences.Editor edit = prefs.edit();
				edit.putBoolean(ctx.getString(R.string.show_beta_warning), false);
				edit.commit();
				
				dismiss();
			}
		});
		
		super.onCreate(savedInstanceState);
	}

}
