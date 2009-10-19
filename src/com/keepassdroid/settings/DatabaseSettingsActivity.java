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

import org.phoneid.keepassj2me.PwManager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.keepass.R;
import com.keepassdroid.ProgressTask;
import com.keepassdroid.app.App;
import com.keepassdroid.database.OnFinish;
import com.keepassdroid.database.SaveDB;

public class DatabaseSettingsActivity extends Activity {

	PwManager mPM;
	
	public static void Launch(Activity act) {
		Intent i = new Intent(act, DatabaseSettingsActivity.class);
		
		act.startActivity(i);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.database_settings);
		
		// Setup database reference
		mPM = App.getDB().mPM;
		
		if ( mPM == null ) {
			Toast.makeText(this, R.string.error_database_settings, Toast.LENGTH_LONG).show();
			finish();
		}
		
		// Set encryption rounds
		int rounds = mPM.numKeyEncRounds;
		EditText editRounds = (EditText) findViewById(R.id.rounds);
		editRounds.setText(Integer.toString(rounds));
		
		Button cancel = (Button) findViewById(R.id.cancel);
		cancel.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				finish();
				
			}
			
		});
		
		Button save = (Button) findViewById(R.id.save);
		save.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				int rounds;
				
				try {
					EditText editRounds = (EditText) findViewById(R.id.rounds);
					String strRounds = editRounds.getText().toString(); 
					rounds = Integer.parseInt(strRounds);
				} catch (NumberFormatException e) {
					Toast.makeText(DatabaseSettingsActivity.this, R.string.error_rounds_not_number, Toast.LENGTH_LONG).show();
					return;
				}
				
				int oldRounds = mPM.numKeyEncRounds;
				mPM.numKeyEncRounds = rounds;
				
				Handler handler = new Handler();
				SaveDB save = new SaveDB(App.getDB(), new AfterSave(handler, oldRounds));
				ProgressTask pt = new ProgressTask(DatabaseSettingsActivity.this, save, R.string.saving_database);
				pt.run();
			}
		});
	}
	
	private class AfterSave extends OnFinish {
		private int mOldRounds;
		
		public AfterSave(Handler handler, int oldRounds) {
			super(handler);
			
			mOldRounds = oldRounds;
		}

		@Override
		public void run() {
			if ( mSuccess ) {
				finish();
			} else {
				displayMessage(DatabaseSettingsActivity.this);
				mPM.numKeyEncRounds = mOldRounds;
			}
			
			super.run();
		}
		
	}

}
