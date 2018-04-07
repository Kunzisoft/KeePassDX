/*
 * Copyright 2018 Brian Pellin, Jeremy Jamet / Kunzisoft, Justin Gross.
 *     
 * This file is part of KeePass Libre.
 *
 *  KeePass Libre is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass Libre is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass Libre.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.dialogs;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;

import tech.jgross.keepass.R;

public class StorageAccessFrameworkDialog extends AlertDialog {

	public StorageAccessFrameworkDialog(@NonNull Context context) {
		super(context);
	}

    public StorageAccessFrameworkDialog(@NonNull Context context, int themeResId) {
		super(context, themeResId);
	}

    public StorageAccessFrameworkDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
		super(context, cancelable, cancelListener);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Context ctx = getContext();
		setMessage(ctx.getString(R.string.warning_disabling_storage_access_framework));

		super.onCreate(savedInstanceState);
	}
}
