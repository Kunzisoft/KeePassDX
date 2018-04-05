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
package com.keepassdroid.dialogs;

import android.content.Context;

import com.kunzisoft.keepass.R;
import com.keepassdroid.compat.BuildCompat;

public class ReadOnlyDialog extends WarningDialog {
	
	public ReadOnlyDialog(Context context) {
		super(context, R.string.show_read_only_warning);
		
		warning = context.getString(R.string.read_only_warning);
		
		if (BuildCompat.getSdkVersion() >= BuildCompat.VERSION_KITKAT) {
			warning = warning.concat("\n\n").concat(context.getString(R.string.read_only_kitkat_warning));
		}
	}
}
