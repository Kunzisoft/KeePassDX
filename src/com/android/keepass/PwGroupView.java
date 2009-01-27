/*
 * Copyright 2009 Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
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
package com.android.keepass;

import org.phoneid.keepassj2me.PwGroup;

import android.app.Activity;
import android.graphics.Color;
import android.util.TypedValue;
import android.widget.TextView;

public class PwGroupView extends PwItemView {
	
	private PwGroup mPw;
	private Activity mAct;
	
	public PwGroupView(Activity act, PwGroup pw) {
		super(act, pw.name);
		mAct = act;
		mPw = pw;
		
		getTextView().setTextColor(Color.BLUE);
		
	}
	
	public void setGroup(PwGroup pw) {
		super.setTitle(pw.name);
		
		mPw = pw;
	}

	@Override
	void onClick() {
		GroupActivity.Launch(mAct, mPw);
	
	}


}