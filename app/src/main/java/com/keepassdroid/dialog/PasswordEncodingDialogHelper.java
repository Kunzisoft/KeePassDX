/*
 * Copyright 2015 Brian Pellin.
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
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;

import com.android.keepass.R;

public class PasswordEncodingDialogHelper {
	private AlertDialog dialog;
	
	public void show(Context ctx, DialogInterface.OnClickListener onclick) {
		show(ctx, onclick, false);
	}

	public void show(Context ctx, DialogInterface.OnClickListener onclick, boolean showCancel) {
	    AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
	    builder.setMessage(R.string.warning_password_encoding).setTitle(R.string.warning);
	    builder.setPositiveButton(android.R.string.ok, onclick);
	    
	    
	    if (showCancel) {
	    	builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});
	    	
	    }
	    
	    dialog = builder.create();
	    
	    dialog.show();
	}

}
