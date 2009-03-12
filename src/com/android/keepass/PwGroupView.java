/*
 * Copyright 2009 Brian Pellin.
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
package com.android.keepass;

import org.phoneid.keepassj2me.PwGroup;

import android.app.Activity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;


public class PwGroupView extends LinearLayout {
	
	private PwGroup mPw;
	private Activity mAct;
	
	public PwGroupView(Activity act, PwGroup pw) {
		super(act);
		mAct = act;
		mPw = pw;
		
		View gv = View.inflate(act, R.layout.group_list_entry, null);
		TextView tv = (TextView) gv.findViewById(R.id.group_text);
		tv.setText(pw.name);
		
		LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		
		addView(gv, lp);
		
	}

	void onClick() {
	
		GroupActivity.Launch(mAct, mPw);
	
	}


}