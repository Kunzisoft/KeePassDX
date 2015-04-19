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
package com.keepassdroid.view;

import com.keepassdroid.assets.TypefaceFactory;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;

public class PasswordTextViewSelect extends TextViewSelect {

	public PasswordTextViewSelect(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}

	public PasswordTextViewSelect(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PasswordTextViewSelect(Context context) {
		super(context);
	}

	private Typeface getTypeface(Typeface tf) {
		Typeface tfOverride = TypefaceFactory.getTypeface(getContext(), "fonts/DejaVuSansMono.ttf");
		
		if (tfOverride != null) {
			return tfOverride;
		}
		
		return tf;
	}

	@Override
	public void setTypeface(Typeface tf, int style) {
		super.setTypeface(getTypeface(tf), style);
	}

	@Override
	public void setTypeface(Typeface tf) {
		super.setTypeface(getTypeface(tf));
	}

}
