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

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.phoneid.keepassj2me.PwEntry;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class EntryActivity extends LockingActivity {
	public static final String KEY_ENTRY = "entry";
	
	private static final int MENU_PASS = Menu.FIRST;
	private static final int MENU_GOTO_URL = Menu.FIRST + 1;
	private static final int MENU_COPY_USER = Menu.FIRST + 2;
	private static final int MENU_COPY_PASS = Menu.FIRST + 3;
	private static final int MENU_LOCK = Menu.FIRST + 4; 
	
	private static final long CLIP_CLEAR_TIME = 30 * 1000;
	
	public static void Launch(Activity act, PwEntry pw) {
		Intent i = new Intent(act, EntryActivity.class);
		
		i.putExtra(KEY_ENTRY, pw.uuid);
		
		act.startActivityForResult(i,0);
	}
	
	private PwEntry mEntry;
	private Timer mTimer = new Timer();
	private boolean showPassword = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.entry_view);
		setResult(KeePass.EXIT_NORMAL);
		
		Intent i = getIntent();
		UUID uuid = UUID.nameUUIDFromBytes(i.getByteArrayExtra(KEY_ENTRY));
		assert(uuid != null);
		
		mEntry = Database.gEntries.get(uuid).get();
				
		fillData();
	}
	
	private void fillData() {
		populateText(R.id.entry_title, mEntry.title);
		populateText(R.id.entry_user_name, mEntry.username);
		populateText(R.id.entry_url, mEntry.url);
		populateText(R.id.entry_password, getString(R.string.MaskedPassword));
		populateText(R.id.entry_comment, mEntry.additional);
		TextView comment = (TextView)findViewById(R.id.entry_comment);
		comment.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
		comment.setMovementMethod(new ScrollingMovementMethod());
		
	}
	
	private void populateText(int viewId, String text) {
		TextView tv = (TextView) findViewById(viewId);
		tv.setText(text);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(0, MENU_PASS, 0, R.string.menu_show_password);
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

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch ( item.getItemId() ) {
		case MENU_PASS:
			if ( showPassword ) {
				populateText(R.id.entry_password, new String(mEntry.getPassword()));
				item.setTitle(R.string.menu_hide_password);
				showPassword = false;
			} else {
				populateText(R.id.entry_password, getString(R.string.MaskedPassword));
				item.setTitle(R.string.menu_show_password);
				showPassword = true;
			}
			return true;
			
		case MENU_GOTO_URL:
			Util.gotoUrl(this, mEntry.url);
			return true;
		case MENU_COPY_USER:
			timeoutCopyToClipboard(mEntry.username);
			return true;
		case MENU_COPY_PASS:
			timeoutCopyToClipboard(new String(mEntry.getPassword()));
			return true;
		case MENU_LOCK:
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

	// This task will be run in the UI thread
	final Runnable runInUIThread = new Runnable() {
		@Override
		public void run() {
			uiClearClipToast();
		}
	};
	
	private void uiClearClipToast() {
		Toast.makeText(this, R.string.ClearClipboard, Toast.LENGTH_SHORT).show();
	}
	
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
				uiThreadCallback.post(runInUIThread);
			}
		}
	}
}
