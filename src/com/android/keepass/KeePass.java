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

import org.bouncycastle1.crypto.InvalidCipherTextException;
import org.phoneid.keepassj2me.ImporterV3;
import org.phoneid.keepassj2me.PwEntry;
import org.phoneid.keepassj2me.PwGroup;
import org.phoneid.keepassj2me.PwManager;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.*;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.UUID;
import java.util.Vector;

public class KeePass extends Activity {
	


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.password);

		Button confirmButton = (Button) findViewById(R.id.pass_ok);
		confirmButton.setOnClickListener(new ClickHandler(this));
		
		loadDefaultPrefs();
		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		// Clear password on Database state
		setEditText(R.id.pass_password, "");
		Database.clear(); 
	}

	@Override
	protected void onStop() {
		super.onStop();
		
	}

	private void loadDefaultPrefs() {
		SharedPreferences settings = getPreferences(MODE_PRIVATE);
		String lastfn = settings.getString("lastFile", "");
		
		if (lastfn == "") {
			lastfn = "/sdcard/keepass/keepass.kdb";
		}
		
		setEditText(R.id.pass_filename, lastfn);
	}
	
	private void saveDefaultPrefs() {
		SharedPreferences settings = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("lastFile", getEditText(R.id.pass_filename));
		editor.commit();
	}
	
	
	private void errorMessage(CharSequence text)
	{
		Toast.makeText(this, text, Toast.LENGTH_LONG).show();
	}
	
	private void errorMessage(int resId)
	{
		Toast.makeText(this, resId, Toast.LENGTH_LONG).show();
	}
	
	private class ClickHandler implements View.OnClickListener {
		private Activity mAct;
				
		ClickHandler(Activity act) {
			mAct = act;
		}
		
		public void onClick(View view) {
			int result = Database.LoadData(getEditText(R.id.pass_filename),getEditText(R.id.pass_password));
			
			switch (result) {
			case 0:
				saveDefaultPrefs();
				GroupActivity.Launch(mAct, null);
				break;
			case -1:
				errorMessage("Unknown error.");
				break;
			default:
				errorMessage(result);
				break;
			}
			
		}

	}
	
	private String getEditText(int resId) {
		EditText te =  (EditText) findViewById(resId);
		assert(te == null);
		
		if (te != null) {
			return te.getText().toString();
		} else {
			return "";
		}
	}
	
	private void setEditText(int resId, String str) {
		EditText te =  (EditText) findViewById(resId);
		assert(te == null);
		
		if (te != null) {
			te.setText(str);
		}
	}
	
}
