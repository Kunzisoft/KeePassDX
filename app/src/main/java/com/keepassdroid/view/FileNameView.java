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
package com.keepassdroid.view;

import android.content.Context;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.kunzisoft.keepass.R;

public class FileNameView extends RelativeLayout {

	public FileNameView(Context context) {
		this(context, null);
	}

	public FileNameView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		inflate(context);
	}
	
	private void inflate(Context context) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.file_selection_filename, this);
	}
	
	public void updateExternalStorageWarning() {
		int warning = -1;
		String state = Environment.getExternalStorageState();
		if (state.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
			warning = R.string.warning_read_only;
		} else if (!state.equals(Environment.MEDIA_MOUNTED)) {
			warning = R.string.warning_unmounted;
		}
		
		TextView tv = findViewById(R.id.label_warning);
		if (warning != -1) {
			tv.setText(warning);
			tv.setVisibility(VISIBLE);
		} else {
			tv.setVisibility(INVISIBLE);
		}
	}
}
