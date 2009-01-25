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

import java.util.UUID;

import org.phoneid.keepassj2me.PwEntry;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class EntryActivity extends Activity {
	public static final String KEY_ENTRY = "entry";
	
	private static final int MENU_PASS = Menu.FIRST;
	private static final int MENU_COPY_URL = Menu.FIRST + 1;
	private static final int MENU_COPY_USER = Menu.FIRST + 2;
	private static final int MENU_COPY_PASS = Menu.FIRST + 3;
	
	
	public static void Launch(Activity act, PwEntry pw) {
		Intent i = new Intent(act, EntryActivity.class);
		
		i.putExtra(KEY_ENTRY, pw.uuid);
		
		act.startActivity(i);
	}
	
	private PwEntry mEntry;
	private int mId;
	private boolean showPassword = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.entry_view);
		
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
		
	}
	
	private void populateText(int viewId, String text) {
		TextView tv = (TextView) findViewById(viewId);
		tv.setText(text);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(0, MENU_PASS, 0, R.string.menu_show_password);
		menu.findItem(MENU_PASS).setIcon(android.R.drawable.ic_menu_view);
		menu.add(0, MENU_COPY_URL, 0, R.string.menu_copy_url);
		menu.findItem(MENU_COPY_URL).setIcon(android.R.drawable.ic_menu_upload);
		menu.add(0, MENU_COPY_USER, 0, R.string.menu_copy_user);
		menu.findItem(MENU_COPY_USER).setIcon(android.R.drawable.ic_menu_set_as);
		menu.add(0, MENU_COPY_PASS, 0, R.string.menu_copy_pass);
		menu.findItem(MENU_COPY_PASS).setIcon(android.R.drawable.ic_menu_agenda);
		
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
			
		case MENU_COPY_URL:
			copyToClipboard(mEntry.url);
			return true;
		case MENU_COPY_USER:
			copyToClipboard(mEntry.username);
			return true;
		case MENU_COPY_PASS:
			copyToClipboard(new String(mEntry.getPassword()));
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	private void copyToClipboard(String text) {
		ClipboardManager clipboard = (ClipboardManager)  getSystemService(CLIPBOARD_SERVICE);
		clipboard.setText(text);
	}

}
