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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.bouncycastle.crypto.InvalidCipherTextException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.keepass.fileselect.FileDbHelper;
import com.android.keepass.intents.TimeoutIntents;
import com.android.keepass.keepasslib.InvalidKeyFileException;

public class PasswordActivity extends Activity {

	private static final int MENU_ABOUT = Menu.FIRST;
	private static final String KEY_FILENAME = "fileName";
	private static final String KEY_KEYFILE = "keyFile";

	private String mFileName;
	private String mKeyFile;
	private ProgressDialog mPd;
	private boolean mIsDialogUp = false;
	
	public static void Launch(Activity act, String fileName) throws FileNotFoundException {
		Launch(act,fileName,"");
	}
	
	public static void Launch(Activity act, String fileName, String keyFile) throws FileNotFoundException {
		File dbFile = new File(fileName);
		if ( ! dbFile.exists() ) {
			throw new FileNotFoundException();
		}
		
		Intent i = new Intent(act, PasswordActivity.class);
		i.putExtra(KEY_FILENAME, fileName);
		i.putExtra(KEY_KEYFILE, keyFile);
		
		act.startActivityForResult(i, 0);
		
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (resultCode == KeePass.EXIT_LOCK) {
			setResult(KeePass.EXIT_LOCK);
			finish();
		}
		
		Database.clear(); 
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	
		Intent i = getIntent();
		mFileName = i.getStringExtra(KEY_FILENAME);
		mKeyFile = i.getStringExtra(KEY_KEYFILE);
		
		setContentView(R.layout.password);
		populateView();

		Button confirmButton = (Button) findViewById(R.id.pass_ok);
		confirmButton.setOnClickListener(new ClickHandler(this));
	}
	
	private void populateView() {
		setEditText(R.id.pass_filename, mFileName);
		setEditText(R.id.pass_keyfile, mKeyFile);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		// Clear password on Database state
		setEditText(R.id.pass_password, "");
		
		sendBroadcast(new Intent(TimeoutIntents.CANCEL));
	}

	@Override
	protected void onStop() {
		super.onStop();
		
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		sendBroadcast(new Intent(TimeoutIntents.START));
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
			String pass = getEditText(R.id.pass_password);
			String key = getEditText(R.id.pass_keyfile);
			if ( pass.length() == 0 && key.length() == 0 ) {
				errorMessage(R.string.error_nopass);
				return;
			}
			
			String fileName = getEditText(R.id.pass_filename);
			
			mPd = ProgressDialog.show(mAct, "Working...", "Loading database", true, false);
			mIsDialogUp = true;
			Thread bkgLoad = new Thread(new BackgroundLoad(fileName, pass, key));
			bkgLoad.start();
			
		}			
	}
	
	private void saveFileData(String fileName, String key) {
		FileDbHelper db = new FileDbHelper(this);
		db.open();
		
		db.createFile(fileName, key);
		
		db.close();
	}
	
	private String getEditText(int resId) {
		return Util.getEditText(this, resId);
	}
	
	private void setEditText(int resId, String str) {
		TextView te =  (TextView) findViewById(resId);
		assert(te == null);
		
		if (te != null) {
			te.setText(str);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(0, MENU_ABOUT, 0, R.string.menu_about);
		menu.findItem(MENU_ABOUT).setIcon(android.R.drawable.ic_menu_help);
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch ( item.getItemId() ) {
		case MENU_ABOUT:
			AboutDialog dialog = new AboutDialog(this);
			dialog.show();
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}

	private final Handler uiHandler = new Handler();
	
	private final class AfterLoad implements Runnable {
		private boolean mLaunch;
		private CharSequence mMsg;
		
		public AfterLoad() {
			mLaunch = true;
			mMsg = "";
		}
		
		public AfterLoad(CharSequence errorMsg) {
			mLaunch = false;
			mMsg = errorMsg;
		}
		
		public AfterLoad(int resId) {
			mLaunch = false;
			mMsg = PasswordActivity.this.getText(resId);
		}
		
		@Override
		public void run() {
			mPd.dismiss();
			mIsDialogUp = false;
			
			if ( mMsg.length() > 0 ) {
				Toast.makeText(PasswordActivity.this, mMsg, Toast.LENGTH_LONG).show();
			}
			
			if ( mLaunch ) {
				GroupActivity.Launch(PasswordActivity.this, null);
			}
		}
	}
	
	private final class BackgroundLoad implements Runnable {
		private String mFileName;
		private String mPass;
		private String mKey;
		
		public BackgroundLoad(String fileName, String pass, String key) {
			mFileName = fileName;
			mPass = pass;
			mKey = key;
		}
		
		@Override
		public void run() {
			try {
				Database.LoadData(PasswordActivity.this, mFileName, mPass, mKey);
				saveFileData(mFileName, mKey);
				uiHandler.post(new AfterLoad());
				
				
			} catch (InvalidCipherTextException e) {
				uiHandler.post(new AfterLoad(R.string.InvalidPassword));
			} catch (FileNotFoundException e) {
				uiHandler.post(new AfterLoad(R.string.FileNotFound));
			} catch (IOException e) {
				uiHandler.post(new AfterLoad("Unknown error."));
			} catch (InvalidKeyFileException e) {
				uiHandler.post(new AfterLoad(e.getMessage()));
			}
			
		}
	}
	
}
