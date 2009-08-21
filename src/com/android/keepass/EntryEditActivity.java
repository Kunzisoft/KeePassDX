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

import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import org.phoneid.keepassj2me.PwEntry;
import org.phoneid.keepassj2me.PwGroup;
import org.phoneid.keepassj2me.Types;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.PasswordTransformationMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class EntryEditActivity extends LockingActivity {
	public static final String KEY_ENTRY = "entry";
	public static final String KEY_PARENT = "parent";

	private static final int MENU_PASS = Menu.FIRST;

	private PwEntry mEntry;
	private boolean mShowPassword = false;
	private boolean mIsNew;
	private ProgressDialog mPd;
	
	public static void Launch(Activity act, PwEntry pw) {
		Intent i = new Intent(act, EntryEditActivity.class);
		
		i.putExtra(KEY_ENTRY, pw.uuid);
		
		act.startActivityForResult(i, 0);
	}
	
	public static void Launch(Activity act, PwGroup parent) {
		Intent i = new Intent(act, EntryEditActivity.class);
		
		i.putExtra(KEY_PARENT, parent.groupId);
		
		act.startActivityForResult(i, 0);
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.entry_edit);
		setResult(KeePass.EXIT_NORMAL);
		
		// Likely the app has been killed exit the activity 
		if ( KeePass.db == null ) {
			finish();
		}

		Intent i = getIntent();
		byte[] uuidBytes = i.getByteArrayExtra(KEY_ENTRY);

		if ( uuidBytes == null ) {
			int groupId = i.getIntExtra(KEY_PARENT, -1);

			mEntry = new PwEntry(KeePass.db, groupId);
			mIsNew = true;
			
		} else {
			UUID uuid = Types.bytestoUUID(uuidBytes);
			assert(uuid != null);

			mEntry = KeePass.db.gEntries.get(uuid).get();
			mIsNew = false;
			
			fillData();
		} 
	
		View scrollView = findViewById(R.id.entry_scroll);
		scrollView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);

		// Save button
		Button save = (Button) findViewById(R.id.entry_save);
		save.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				EntryEditActivity act = EntryEditActivity.this;
				
				// Require title
				String title = Util.getEditText(act, R.id.entry_title);
				if ( title.length() == 0 ) {
					Toast.makeText(act, R.string.error_title_required, Toast.LENGTH_LONG).show();
					return;
				}
				
				// Validate password
				String pass = Util.getEditText(act, R.id.entry_password);
				String conf = Util.getEditText(act, R.id.entry_confpassword);
				if ( ! pass.equals(conf) ) {
					Toast.makeText(act, R.string.error_pass_match, Toast.LENGTH_LONG).show();
					return;
				}
				
				PwEntry newEntry = new PwEntry();
				
				newEntry.binaryDesc = mEntry.binaryDesc;
				newEntry.groupId = mEntry.groupId;
				newEntry.imageId = mEntry.imageId;
				newEntry.parent = mEntry.parent;
				newEntry.tCreation = mEntry.tCreation;
				newEntry.tExpire = mEntry.tExpire;
				newEntry.uuid = mEntry.uuid;
				
				Date now = Calendar.getInstance().getTime(); 
				newEntry.tLastAccess = now;
				newEntry.tLastMod = now;
				
				byte[] binaryData = mEntry.getBinaryData();
				if ( binaryData != null ) {
					newEntry.setBinaryData(binaryData, 0, binaryData.length);
				}

				newEntry.title = Util.getEditText(act, R.id.entry_title);
				newEntry.url = Util.getEditText(act, R.id.entry_url);
				newEntry.username = Util.getEditText(act, R.id.entry_user_name);
				newEntry.additional = Util.getEditText(act, R.id.entry_comment);
				byte[] password;
				try {
					password = pass.getBytes("UTF-8");
				} catch (UnsupportedEncodingException e) {
					assert false;
					password = pass.getBytes();
				}
				newEntry.setPassword(password, 0, password.length);

				if ( newEntry.title.equals(mEntry.title) ) {
					setResult(KeePass.EXIT_REFRESH);
				} else {
					setResult(KeePass.EXIT_REFRESH_TITLE);
				}

				mPd = ProgressDialog.show(EntryEditActivity.this, "Working...", "Saving database", true, false);
				Thread bkgStore = new Thread(new BackgroundUpdateEntry(mEntry, newEntry));
				bkgStore.start();

			}
			
		});
		
		// Cancel button
		Button cancel = (Button) findViewById(R.id.entry_cancel);
		cancel.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
				
			}
			
		});
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(0, MENU_PASS, 0, R.string.menu_show_password);
		menu.findItem(MENU_PASS).setIcon(android.R.drawable.ic_menu_view);
		
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch ( item.getItemId() ) {
		case MENU_PASS:
			if ( mShowPassword ) {
				item.setTitle(R.string.menu_hide_password);
				mShowPassword = false;
			} else {
				item.setTitle(R.string.menu_show_password);
				mShowPassword = true;
			}
			setPasswordStyle();
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	private void setPasswordStyle() {
		TextView password = (TextView) findViewById(R.id.entry_password);
		TextView confpassword = (TextView) findViewById(R.id.entry_confpassword);

		if ( mShowPassword ) {
			password.setTransformationMethod(null);
			confpassword.setTransformationMethod(null);

		} else {
			PasswordTransformationMethod ptm = PasswordTransformationMethod.getInstance();
			password.setTransformationMethod(ptm);
			confpassword.setTransformationMethod(ptm);
		}
	}

	private void fillData() {
		populateText(R.id.entry_title, mEntry.title);
		populateText(R.id.entry_user_name, mEntry.username);
		populateText(R.id.entry_url, mEntry.url);
		
		String password = new String(mEntry.getPassword());
		populateText(R.id.entry_password, password);
		populateText(R.id.entry_confpassword, password);
		setPasswordStyle();

		populateText(R.id.entry_comment, mEntry.additional);
	}

	private void populateText(int viewId, String text) {
		TextView tv = (TextView) findViewById(viewId);
		tv.setText(text);
	}
	
	private final Handler uiHandler = new Handler();
	
	private final class AfterSave implements Runnable {

		@Override
		public void run() {
			mPd.dismiss();
			finish();
		}
		
	}
	
	private final class BackgroundUpdateEntry implements Runnable {

		private final PwEntry mOld;
		private final PwEntry mNew;
		
		public BackgroundUpdateEntry(PwEntry oldE, PwEntry newE) {
			mOld = oldE;
			mNew = newE;
		}
		
		@Override
		public void run() {
			try {
				if ( mIsNew ) {
					KeePass.db.NewEntry(mNew);
				} else {
					KeePass.db.UpdateEntry(mOld, mNew);
				}
				
				uiHandler.post(new AfterSave());
			} catch (Exception e) {
				uiHandler.post(new UIToastTask(EntryEditActivity.this, "Failed to store database."));
				mPd.dismiss();
				setResult(KeePass.EXIT_NORMAL);
				return;
			}
		}
		
	}

}
