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
package com.keepassdroid.settings;


import android.content.Context;
import android.os.Handler;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.keepass.R;
import com.keepassdroid.Database;
import com.keepassdroid.ProgressTask;
import com.keepassdroid.app.App;
import com.keepassdroid.database.PwDatabaseV3;
import com.keepassdroid.database.edit.OnFinish;
import com.keepassdroid.database.edit.SaveDB;

public class RoundsPreference extends DialogPreference {
	
	private PwDatabaseV3 mPM;
	private TextView mRoundsView;

	@Override
	protected View onCreateDialogView() {
		View view =  super.onCreateDialogView();
		
		mRoundsView = (TextView) view.findViewById(R.id.rounds);
		
		Database db = App.getDB();
		mPM = db.mPM;
		int numRounds = mPM.mNumKeyEncRounds;
		mRoundsView.setText(Integer.toString(numRounds));
		
		return view;
	}

	public RoundsPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public RoundsPreference(Context context, AttributeSet attrs, int defStyle) {
	   super(context, attrs, defStyle);
   }

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		if ( positiveResult ) {
			int rounds;
			
			try {
				String strRounds = mRoundsView.getText().toString(); 
				rounds = Integer.parseInt(strRounds);
			} catch (NumberFormatException e) {
				Toast.makeText(getContext(), R.string.error_rounds_not_number, Toast.LENGTH_LONG).show();
				return;
			}
			
			if ( rounds < 1 ) {
				rounds = 1;
			}
			
			int oldRounds = mPM.mNumKeyEncRounds;
			mPM.mNumKeyEncRounds = rounds;
			
			Handler handler = new Handler();
			SaveDB save = new SaveDB(App.getDB(), new AfterSave(getContext(), handler, oldRounds));
			ProgressTask pt = new ProgressTask(getContext(), save, R.string.saving_database);
			pt.run();
			
		}

	}
	
	private class AfterSave extends OnFinish {
		private int mOldRounds;
		private Context mCtx;
		
		public AfterSave(Context ctx, Handler handler, int oldRounds) {
			super(handler);
			
			mCtx = ctx;
			mOldRounds = oldRounds;
		}

		@Override
		public void run() {
			if ( mSuccess ) {
				OnPreferenceChangeListener listner = getOnPreferenceChangeListener();
				if ( listner != null ) {
					listner.onPreferenceChange(RoundsPreference.this, null);
				}
			} else {
				displayMessage(mCtx);
				mPM.mNumKeyEncRounds = mOldRounds;
			}
			
			super.run();
		}
		
	}

}
