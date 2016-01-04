/*
 * Copyright 2010-2011 Brian Pellin.
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
package com.keepassdroid.view;

import android.content.Context;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.keepass.R;

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
		
		TextView tv = (TextView) findViewById(R.id.label_warning);
		TextView label = (TextView) findViewById(R.id.label_open_by_filename);
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		
		if (warning != -1) {
			tv.setText(warning);
			tv.setVisibility(VISIBLE);
			
			lp.addRule(RelativeLayout.BELOW, R.id.label_warning);
		} else {
			tv.setVisibility(INVISIBLE);
		}
		
		label.setLayoutParams(lp);
	}
}
