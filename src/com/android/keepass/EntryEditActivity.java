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

import java.util.UUID;

import org.phoneid.keepassj2me.PwEntry;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class EntryEditActivity extends LockingActivity {
	public static final String KEY_ENTRY = "entry";

	private static final int MENU_PASS = Menu.FIRST;

	private PwEntry mEntry;
	private boolean mShowPassword = false;
	
	public static void Launch(Activity act, PwEntry pw) {
		Intent i = new Intent(act, EntryEditActivity.class);
		
		i.putExtra(KEY_ENTRY, pw.uuid);
		act.startActivity(i);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.entry_edit);
		
		Intent i = getIntent();
		UUID uuid = UUID.nameUUIDFromBytes(i.getByteArrayExtra(KEY_ENTRY));
		assert(uuid != null);

		mEntry = Database.gEntries.get(uuid).get();
	
		View scrollView = findViewById(R.id.entry_scrollview);
		scrollView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
		
		
		fillData();
		
		Button save = (Button) findViewById(R.id.entry_save);
		save.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
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
		TextView comment = (TextView)findViewById(R.id.entry_comment);
		comment.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
		comment.setMovementMethod(new ScrollingMovementMethod());
		
	}

	private void populateText(int viewId, String text) {
		TextView tv = (TextView) findViewById(viewId);
		tv.setText(text);
	}

}
