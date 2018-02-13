/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.keepassdroid.app.App;
import com.keepassdroid.database.Database;
import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.PwEntryV3;
import com.keepassdroid.database.PwEntryV4;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.PwGroupId;
import com.keepassdroid.database.PwGroupV3;
import com.keepassdroid.database.PwGroupV4;
import com.keepassdroid.database.PwIconStandard;
import com.keepassdroid.database.edit.AddEntry;
import com.keepassdroid.database.edit.OnFinish;
import com.keepassdroid.database.edit.RunnableOnFinish;
import com.keepassdroid.database.edit.UpdateEntry;
import com.keepassdroid.fragments.GeneratePasswordDialogFragment;
import com.keepassdroid.fragments.IconPickerDialogFragment;
import com.keepassdroid.icons.Icons;
import com.keepassdroid.tasks.ProgressTask;
import com.keepassdroid.utils.MenuUtil;
import com.keepassdroid.utils.Types;
import com.keepassdroid.utils.Util;
import com.kunzisoft.keepass.KeePass;
import com.kunzisoft.keepass.R;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public abstract class EntryEditActivity extends LockCloseHideActivity
		implements IconPickerDialogFragment.IconPickerListener,
        GeneratePasswordDialogFragment.GeneratePasswordListener {

    // Keys for current Activity
	public static final String KEY_ENTRY = "entry";
	public static final String KEY_PARENT = "parent";

	// Keys for callback
	public static final int ADD_ENTRY_RESULT_CODE = 31;
	public static final int UPDATE_ENTRY_RESULT_CODE = 32;
	public static final int ADD_OR_UPDATE_ENTRY_REQUEST_CODE = 7129;
	public static final String ADD_OR_UPDATE_ENTRY_KEY = "ADD_OR_UPDATE_ENTRY_KEY";

	protected PwEntry mEntry;
	protected PwEntry mCallbackNewEntry;
	protected boolean mIsNew;
	protected int mSelectedIconID = -1;

	/**
	 * Launch EntryEditActivity to update an existing entry
	 * @param act from activity
	 * @param pw Entry to update
	 */
	public static void Launch(Activity act, PwEntry pw) {
		Intent i;
		if (pw instanceof PwEntryV3) {
			i = new Intent(act, EntryEditActivityV3.class);
		}
		else if (pw instanceof PwEntryV4) {
			i = new Intent(act, EntryEditActivityV4.class);
		}
		else {
			throw new RuntimeException("Not yet implemented.");
		}
		
		i.putExtra(KEY_ENTRY, Types.UUIDtoBytes(pw.getUUID()));
		
		act.startActivityForResult(i, ADD_OR_UPDATE_ENTRY_REQUEST_CODE);
	}

	/**
	 * Launch EntryEditActivity to add a new entry
	 * @param act from activity
	 * @param pw Group who will contains new entry
	 */
	public static void Launch(Activity act, PwGroup pw) {
		Intent i;
		if (pw instanceof PwGroupV3) {
			i = new Intent(act, EntryEditActivityV3.class);
			EntryEditActivityV3.putParentId(i, KEY_PARENT, (PwGroupV3)pw);
		}
		else if (pw instanceof PwGroupV4) {
			i = new Intent(act, EntryEditActivityV4.class);
			EntryEditActivityV4.putParentId(i, KEY_PARENT, (PwGroupV4)pw);
		}
		else {
			throw new RuntimeException("Not yet implemented.");
		}

		act.startActivityForResult(i, ADD_OR_UPDATE_ENTRY_REQUEST_CODE);
	}
	
	protected abstract PwGroupId getParentGroupId(Intent i, String key);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.entry_edit);
		setResult(KeePass.EXIT_NORMAL);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.app_name));
        setSupportActionBar(toolbar);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
		
		// Likely the app has been killed exit the activity
		Database db = App.getDB();
		if ( ! db.Loaded() ) {
			finish();
			return;
		}

		Intent i = getIntent();
		byte[] uuidBytes = i.getByteArrayExtra(KEY_ENTRY);

		PwDatabase pm = db.pm;
		if ( uuidBytes == null ) {

			PwGroupId parentId = getParentGroupId(i, KEY_PARENT);
			PwGroup parent = pm.groups.get(parentId);
			mEntry = PwEntry.getInstance(parent);
			mIsNew = true;
			
		} else {
			UUID uuid = Types.bytestoUUID(uuidBytes);
			mEntry = pm.entries.get(uuid);
			mIsNew = false;
			fillData();
		} 
	
		View scrollView = findViewById(R.id.entry_scroll);
		scrollView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);

		View iconButton = findViewById(R.id.icon_button);
		iconButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				IconPickerDialogFragment.Launch(EntryEditActivity.this);
			}
		});

		// Generate password button
		View generatePassword = findViewById(R.id.generate_button);
		generatePassword.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				GeneratePasswordDialogFragment generatePasswordDialogFragment = new GeneratePasswordDialogFragment();
				generatePasswordDialogFragment.show(getSupportFragmentManager(), "PasswordGeneratorFragment");
			}
		});
		
		// Save button
		View save = findViewById(R.id.entry_save);
		save.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				if (!validateBeforeSaving()) {
					return;
				}

                mCallbackNewEntry = populateNewEntry();

				OnFinish onFinish = new AfterSave();
                EntryEditActivity act = EntryEditActivity.this;
                RunnableOnFinish task;
				if ( mIsNew ) {
					task = new AddEntry(act, App.getDB(), mCallbackNewEntry, onFinish);
				} else {
					task = new UpdateEntry(act, App.getDB(), mEntry, mCallbackNewEntry, onFinish);
				}
				ProgressTask pt = new ProgressTask(act, task, R.string.saving_database);
				pt.run();
			}
			
		});
	}
	
	protected boolean validateBeforeSaving() {
		// Require title
		String title = Util.getEditText(this, R.id.entry_title);
		if ( title.length() == 0 ) {
			Toast.makeText(this, R.string.error_title_required, Toast.LENGTH_LONG).show();
			return false;
		}
		
		// Validate password
		String pass = Util.getEditText(this, R.id.entry_password);
		String conf = Util.getEditText(this, R.id.entry_confpassword);
		if ( ! pass.equals(conf) ) {
			Toast.makeText(this, R.string.error_pass_match, Toast.LENGTH_LONG).show();
			return false;
		}
		
		return true;
	}
	
	protected PwEntry populateNewEntry() {
		return populateNewEntry(null);
	}
	
	protected PwEntry populateNewEntry(PwEntry entry) {
		PwEntry newEntry;
		if (entry == null) {
			newEntry = mEntry.clone(true);
		} 
		else {
			newEntry = entry;
		}
		
		Date now = Calendar.getInstance().getTime(); 
		newEntry.setLastAccessTime(now);
		newEntry.setLastModificationTime(now);
		
		PwDatabase db = App.getDB().pm;
		newEntry.setTitle(Util.getEditText(this, R.id.entry_title), db);
		if(mSelectedIconID != -1)
		    newEntry.setIcon(new PwIconStandard(mSelectedIconID));
		newEntry.setUrl(Util.getEditText(this, R.id.entry_url), db);
		newEntry.setUsername(Util.getEditText(this, R.id.entry_user_name), db);
		newEntry.setNotes(Util.getEditText(this, R.id.entry_comment), db);
		newEntry.setPassword(Util.getEditText(this, R.id.entry_password), db);
		
		return newEntry;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		MenuInflater inflater = getMenuInflater();
		MenuUtil.donationMenuInflater(inflater, menu);
		
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch ( item.getItemId() ) {
			case R.id.menu_donate:
				return MenuUtil.onDonationItemSelected(this);

			case android.R.id.home:
				finish();
		}
		
		return super.onOptionsItemSelected(item);
	}

	protected void fillData() {
		ImageButton currIconButton = (ImageButton) findViewById(R.id.icon_button);
		App.getDB().drawFactory.assignDrawableTo(currIconButton, getResources(), mEntry.getIcon());

		populateText(R.id.entry_title, mEntry.getTitle());
		populateText(R.id.entry_user_name, mEntry.getUsername());
		populateText(R.id.entry_url, mEntry.getUrl());
		
		String password = mEntry.getPassword();
		populateText(R.id.entry_password, password);
		populateText(R.id.entry_confpassword, password);

		populateText(R.id.entry_comment, mEntry.getNotes());
	}

	private void populateText(int viewId, String text) {
		TextView tv = (TextView) findViewById(viewId);
		tv.setText(text);
	}

    @Override
    public void iconPicked(Bundle bundle) {
        mSelectedIconID = bundle.getInt(IconPickerDialogFragment.KEY_ICON_ID);
        ImageButton currIconButton = (ImageButton) findViewById(R.id.icon_button);
        currIconButton.setImageResource(Icons.iconToResId(mSelectedIconID));
    }

    @Override
    public void acceptPassword(Bundle bundle) {
        String generatedPassword = bundle.getString(GeneratePasswordDialogFragment.KEY_PASSWORD_ID);
        EditText password = (EditText) findViewById(R.id.entry_password);
        EditText confPassword = (EditText) findViewById(R.id.entry_confpassword);

        password.setText(generatedPassword);
        confPassword.setText(generatedPassword);
    }

    @Override
    public void cancelPassword(Bundle bundle) {
        // Do nothing here
    }

	@Override
	public void finish() {
	    // Assign entry callback as a result in all case
		Intent intentEntry = new Intent();
		if ( mIsNew ) {
			intentEntry.putExtra(ADD_OR_UPDATE_ENTRY_KEY, mCallbackNewEntry);
			setResult(ADD_ENTRY_RESULT_CODE, intentEntry);
		} else {
			intentEntry.putExtra(ADD_OR_UPDATE_ENTRY_KEY, mCallbackNewEntry);
			setResult(UPDATE_ENTRY_RESULT_CODE, intentEntry);
		}

		super.finish();
	}

	private final class AfterSave extends OnFinish {

		AfterSave() {
			super(new Handler());
		}

		@Override
		public void run() {
			if ( mSuccess ) {
				finish();
			} else {
				displayMessage(EntryEditActivity.this);
			}
		}
	}

}
