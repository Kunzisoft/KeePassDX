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
package com.keepassdroid;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.phoneid.keepassj2me.PwEntry;
import org.phoneid.keepassj2me.Types;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
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

import com.android.keepass.KeePass;
import com.android.keepass.R;
import com.keepassdroid.app.App;

public class EntryActivity extends LockCloseActivity {
	public static final String KEY_ENTRY = "entry";
	public static final String KEY_REFRESH_POS = "refresh_pos";

	private static final int MENU_DONATE = Menu.FIRST;
	private static final int MENU_PASS = Menu.FIRST + 1;
	private static final int MENU_GOTO_URL = Menu.FIRST + 2;
	private static final int MENU_COPY_USER = Menu.FIRST + 3;
	private static final int MENU_COPY_PASS = Menu.FIRST + 4;
	private static final int MENU_LOCK = Menu.FIRST + 5; 
	
	private static final long CLIP_CLEAR_TIME = 5 * 60 * 1000;
	
	public static void Launch(Activity act, PwEntry pw, int pos) {
		Intent i = new Intent(act, EntryActivity.class);
		
		i.putExtra(KEY_ENTRY, pw.uuid);
		i.putExtra(KEY_REFRESH_POS, pos);
		
		act.startActivityForResult(i,0);
	}
	
	private PwEntry mEntry;
	private Timer mTimer = new Timer();
	private boolean mShowPassword = false;
	private int mPos;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.entry_view);
		setResult(KeePass.EXIT_NORMAL);

		
		Database db = App.getDB();
		// Likely the app has been killed exit the activity 
		if ( ! db.Loaded() ) {
			finish();
		}

		Intent i = getIntent();
		UUID uuid = Types.bytestoUUID(i.getByteArrayExtra(KEY_ENTRY));
		mPos = i.getIntExtra(KEY_REFRESH_POS, -1);
		assert(uuid != null);
		
		mEntry = db.gEntries.get(uuid).get();
		
		// Update last access time.
		Calendar cal = Calendar.getInstance();
		mEntry.tLastAccess = cal.getTime();
		fillData();

		View scroll = findViewById(R.id.entry_scroll);
		scroll.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
		
		Button edit = (Button) findViewById(R.id.entry_edit);
		edit.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				EntryEditActivity.Launch(EntryActivity.this, mEntry);
			}
			
		});
	}
	
	private void fillData() {
		populateText(R.id.entry_title, mEntry.title);
		populateText(R.id.entry_user_name, mEntry.username);
		populateText(R.id.entry_url, mEntry.url);
		populateText(R.id.entry_password, new String(mEntry.getPassword()));
		setPasswordStyle();
		
		DateFormat df = DateFormat.getInstance();
		populateText(R.id.entry_created, df.format(mEntry.tCreation));
		populateText(R.id.entry_modified, df.format(mEntry.tLastMod));
		populateText(R.id.entry_accessed, df.format(mEntry.tLastAccess));
		
		if ( PwEntry.IsNever(mEntry.tExpire) ) {
			populateText(R.id.entry_expires, R.string.never);
		} else {
			populateText(R.id.entry_expires, df.format(mEntry.tExpire));
		}
		populateText(R.id.entry_comment, mEntry.additional);
	}
	
	private void populateText(int viewId, int resId) {
		TextView tv = (TextView) findViewById(viewId);
		tv.setText(resId);
	}

	private void populateText(int viewId, String text) {
		TextView tv = (TextView) findViewById(viewId);
		tv.setText(text);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if ( resultCode == KeePass.EXIT_REFRESH || resultCode == KeePass.EXIT_REFRESH_TITLE ) {
			fillData();
			if ( resultCode == KeePass.EXIT_REFRESH_TITLE ) {
				Intent ret = new Intent();
				ret.putExtra(KEY_REFRESH_POS, mPos);
				setResult(KeePass.EXIT_REFRESH, ret);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(0, MENU_DONATE, 0, R.string.menu_donate);
		menu.findItem(MENU_DONATE).setIcon(android.R.drawable.ic_menu_share);

		menu.add(0, MENU_PASS, 0, R.string.show_password);
		menu.findItem(MENU_PASS).setIcon(android.R.drawable.ic_menu_view);
		menu.add(0, MENU_GOTO_URL, 0, R.string.menu_url);
		menu.findItem(MENU_GOTO_URL).setIcon(android.R.drawable.ic_menu_upload);
		menu.add(0, MENU_COPY_USER, 0, R.string.menu_copy_user);
		menu.findItem(MENU_COPY_USER).setIcon(android.R.drawable.ic_menu_set_as);
		menu.add(0, MENU_COPY_PASS, 0, R.string.menu_copy_pass);
		menu.findItem(MENU_COPY_PASS).setIcon(android.R.drawable.ic_menu_agenda);
		menu.add(0, MENU_LOCK, 0, R.string.menu_lock);
		menu.findItem(MENU_LOCK).setIcon(android.R.drawable.ic_lock_lock);
		
		return true;
	}
	
	private void setPasswordStyle() {
		TextView password = (TextView) findViewById(R.id.entry_password);

		if ( mShowPassword ) {
			password.setTransformationMethod(null);
		} else {
			password.setTransformationMethod(PasswordTransformationMethod.getInstance());
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch ( item.getItemId() ) {
		case MENU_DONATE:
			try {
				Util.gotoUrl(this, R.string.donate_url);
			} catch (ActivityNotFoundException e) {
				Toast.makeText(this, R.string.error_failed_to_launch_link, Toast.LENGTH_LONG).show();
				return false;
			}
			
			return true;
		case MENU_PASS:
			if ( mShowPassword ) {
				item.setTitle(R.string.show_password);
				mShowPassword = false;
			} else {
				item.setTitle(R.string.menu_hide_password);
				mShowPassword = true;
			}
			setPasswordStyle();

			return true;
			
		case MENU_GOTO_URL:
			String url;
			url = mEntry.url;
			
			// Default http:// if no protocol specified
			if ( ! url.contains("://") ) {
				url = "http://" + url;
			}
			
			try {
				Util.gotoUrl(this, url);
			} catch (ActivityNotFoundException e) {
				Toast.makeText(this, R.string.no_url_handler, Toast.LENGTH_LONG).show();
			}
			return true;
			
		case MENU_COPY_USER:
			timeoutCopyToClipboard(mEntry.username);
			return true;
			
		case MENU_COPY_PASS:
			timeoutCopyToClipboard(new String(mEntry.getPassword()));
			return true;
			
		case MENU_LOCK:
			App.setShutdown();
			setResult(KeePass.EXIT_LOCK);
			finish();
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	private void timeoutCopyToClipboard(String text) {
		Util.copyToClipboard(this, text);
		mTimer.schedule(new ClearClipboardTask(this, text), CLIP_CLEAR_TIME);
	}
	

	// Setup to allow the toast to happen in the foreground
	final Handler uiThreadCallback = new Handler();

	// Task which clears the clipboard, and sends a toast to the foreground.
	private class ClearClipboardTask extends TimerTask {
		
		private final String mClearText;
		private final Context mCtx;
		
		ClearClipboardTask(Context ctx, String clearText) {
			mClearText = clearText;
			mCtx = ctx;
		}
		
		@Override
		public void run() {
			String currentClip = Util.getClipboard(mCtx);
			
			if ( currentClip.equals(mClearText) ) {
				Util.copyToClipboard(mCtx, "");
				uiThreadCallback.post(new UIToastTask(mCtx, R.string.ClearClipboard));
			}
		}
	}
}
