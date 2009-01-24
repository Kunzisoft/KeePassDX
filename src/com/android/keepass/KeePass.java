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
import java.util.HashMap;
import java.util.Vector;

public class KeePass extends Activity {
	

	private PwManager mPM;
	public static HashMap<Integer, Vector> gGroups = new HashMap<Integer, Vector>();
	public static HashMap<Integer, Vector> gEntries = new HashMap<Integer, Vector>();
	public static Integer gNumEntries = new Integer(0);
	
	public static HashMap<Integer, PwEntry> gPwEntry = new HashMap<Integer, PwEntry>();
	public static Integer gNumPwEntry = new Integer(0);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.password);

		Button confirmButton = (Button) findViewById(R.id.pass_ok);
		confirmButton.setOnClickListener(new ClickHandler(this));
		
		loadDefaultPrefs();
		
		//setEditText(R.id.pass_password, "12345");
		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		setEditText(R.id.pass_password, "");
	}

	@Override
	protected void onStop() {
		super.onStop();
		
		saveDefaultPrefs();
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
	
	private boolean fillData(String filename, String password) {
		FileInputStream fis;
		try {
			fis = new FileInputStream(filename);
		} catch (FileNotFoundException e) {
			errorMessage(R.string.FileNotFound);
			return false;
		}
		
		ImporterV3 Importer = new ImporterV3();
	
		try {
			mPM = Importer.openDatabase(fis, password);
			if ( mPM != null ) {
				mPM.constructTree(null);
			}
		} catch (InvalidCipherTextException e) {
			errorMessage(R.string.InvalidPassword);
			return false;
		} catch (IOException e) {
			errorMessage("IO Error");
			return false;
		}
		
		return true;
		
	}
	
	private void errorMessage(CharSequence text)
	{
		Toast.makeText(this, text, Toast.LENGTH_LONG).show();
	}
	
	private void errorMessage(int resId)
	{
		Toast.makeText(this, resId, Toast.LENGTH_LONG).show();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		gGroups.remove(requestCode);
		gEntries.remove(requestCode);
	}
	
	private class ClickHandler implements View.OnClickListener {
		private Activity mAct;
				
		ClickHandler(Activity act) {
			mAct = act;
		}
		
		public void onClick(View view) {
			if ( fillData(getEditText(R.id.pass_filename),getEditText(R.id.pass_password)) ) {
				GroupActivity.Launch(mAct, mPM.getGrpRoots(), new Vector());
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
