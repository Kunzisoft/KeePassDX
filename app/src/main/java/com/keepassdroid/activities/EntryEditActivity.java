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
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.keepassdroid.app.App;
import com.keepassdroid.database.Database;
import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.PwDate;
import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.PwGroupId;
import com.keepassdroid.database.PwIconStandard;
import com.keepassdroid.database.edit.AddEntry;
import com.keepassdroid.database.edit.OnFinish;
import com.keepassdroid.database.edit.RunnableOnFinish;
import com.keepassdroid.database.edit.UpdateEntry;
import com.keepassdroid.database.security.ProtectedString;
import com.keepassdroid.dialogs.GeneratePasswordDialogFragment;
import com.keepassdroid.dialogs.IconPickerDialogFragment;
import com.keepassdroid.icons.Icons;
import com.keepassdroid.settings.PreferencesUtil;
import com.keepassdroid.tasks.ProgressTask;
import com.keepassdroid.utils.MenuUtil;
import com.keepassdroid.utils.Types;
import com.keepassdroid.utils.Util;
import com.keepassdroid.view.EntryEditCustomField;
import com.kunzisoft.keepass.R;

import java.util.UUID;

public class EntryEditActivity extends LockingHideActivity
		implements IconPickerDialogFragment.IconPickerListener,
        GeneratePasswordDialogFragment.GeneratePasswordListener {

    private static final String TAG = EntryEditActivity.class.getName();

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

    // Views
    private ScrollView scrollView;
    private EditText entryTitleView;
    private EditText entryUserNameView;
    private EditText entryUrlView;
    private EditText entryPasswordView;
    private EditText entryConfirmationPasswordView;
    private EditText entryCommentView;
    private ViewGroup entryExtraFieldsContainer;

	/**
	 * launch EntryEditActivity to update an existing entry
	 * @param act from activity
	 * @param pw Entry to update
	 */
	public static void Launch(Activity act, PwEntry pw) {
        if (LockingActivity.checkTimeIsAllowedOrFinish(act)) {
            Intent intent = new Intent(act, EntryEditActivity.class);
            intent.putExtra(KEY_ENTRY, Types.UUIDtoBytes(pw.getUUID()));
            act.startActivityForResult(intent, ADD_OR_UPDATE_ENTRY_REQUEST_CODE);
        }
	}

	/**
	 * launch EntryEditActivity to add a new entry
	 * @param act from activity
	 * @param pwGroup Group who will contains new entry
	 */
	public static void Launch(Activity act, PwGroup pwGroup) {
        if (LockingActivity.checkTimeIsAllowedOrFinish(act)) {
            Intent intent = new Intent(act, EntryEditActivity.class);
            intent.putExtra(KEY_PARENT, pwGroup.getId());
            act.startActivityForResult(intent, ADD_OR_UPDATE_ENTRY_REQUEST_CODE);
        }
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.entry_edit);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.app_name));
        setSupportActionBar(toolbar);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        scrollView = findViewById(R.id.entry_scroll);
        scrollView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);

        entryTitleView = findViewById(R.id.entry_title);
        entryUserNameView = findViewById(R.id.entry_user_name);
        entryUrlView = findViewById(R.id.entry_url);
        entryPasswordView = findViewById(R.id.entry_password);
        entryConfirmationPasswordView = findViewById(R.id.entry_confpassword);
        entryCommentView = findViewById(R.id.entry_comment);
        entryExtraFieldsContainer = findViewById(R.id.advanced_container);
		
		// Likely the app has been killed exit the activity
		Database db = App.getDB();
		if ( ! db.getLoaded() ) {
			finish();
			return;
		}

		Intent intent = getIntent();
		byte[] uuidBytes = intent.getByteArrayExtra(KEY_ENTRY);

		PwDatabase pm = db.getPwDatabase();
		if ( uuidBytes == null ) {
            PwGroupId parentId = (PwGroupId) intent.getSerializableExtra(KEY_PARENT);
			PwGroup parent = pm.getGroupByGroupId(parentId);
			mEntry = PwEntry.getInstance(parent);
			mIsNew = true;
		} else {
			UUID uuid = Types.bytestoUUID(uuidBytes);
			mEntry = pm.getEntryByUUIDId(uuid);
			mIsNew = false;
			fillData();
		}

		View iconButton = findViewById(R.id.icon_button);
		iconButton.setOnClickListener(v ->
                IconPickerDialogFragment.launch(EntryEditActivity.this));

		// Generate password button
		View generatePassword = findViewById(R.id.generate_button);
		generatePassword.setOnClickListener(v -> {
            GeneratePasswordDialogFragment generatePasswordDialogFragment = new GeneratePasswordDialogFragment();
            generatePasswordDialogFragment.show(getSupportFragmentManager(), "PasswordGeneratorFragment");
        });
		
		// Save button
		View save = findViewById(R.id.entry_save);
		save.setOnClickListener(v -> {
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
        });


		if (mEntry.allowExtraFields()) {
            View add = findViewById(R.id.add_new_field);
            add.setVisibility(View.VISIBLE);
            add.setOnClickListener(v -> {
                EntryEditCustomField ees = new EntryEditCustomField(EntryEditActivity.this);
                ees.setData("", new ProtectedString(false, ""));
                entryExtraFieldsContainer.addView(ees);

                // Scroll bottom
                scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
            });
        }
	}
	
	protected boolean validateBeforeSaving() {
		// Require title
		String title = entryTitleView.getText().toString();
		if ( title.length() == 0 ) {
			Toast.makeText(this, R.string.error_title_required, Toast.LENGTH_LONG).show();
			return false;
		}
		
		// Validate password
		String pass = entryPasswordView.getText().toString();
		String conf = entryConfirmationPasswordView.getText().toString();
		if ( ! pass.equals(conf) ) {
			Toast.makeText(this, R.string.error_pass_match, Toast.LENGTH_LONG).show();
			return false;
		}

		// Validate extra fields
        if (mEntry.allowExtraFields()) {
            for (int i = 0; i < entryExtraFieldsContainer.getChildCount(); i++) {
                EntryEditCustomField entryEditCustomField = (EntryEditCustomField) entryExtraFieldsContainer.getChildAt(i);
                String key = entryEditCustomField.getLabel();
                if (key == null || key.length() == 0) {
                    Toast.makeText(this, R.string.error_string_key, Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        }

        return true;
	}
	
	protected PwEntry populateNewEntry() {
        PwDatabase db = App.getDB().getPwDatabase();

        PwEntry newEntry = mEntry.clone();

        newEntry.startToManageFieldReferences(db);

        newEntry.createBackup(db);

        newEntry.setLastAccessTime(new PwDate());
        newEntry.setLastModificationTime(new PwDate());

        newEntry.setTitle(entryTitleView.getText().toString());
        if(mSelectedIconID != -1)
            // or TODO icon factory newEntry.setIcon(App.getDB().pm.iconFactory.getIcon(mSelectedIconID));
            newEntry.setIcon(new PwIconStandard(mSelectedIconID));
        else {
            if (mIsNew) {
                newEntry.setIcon(App.getDB().getPwDatabase().getIconFactory().getFirstIcon());
            }
            else {
                // Keep previous icon, if no new one was selected
                newEntry.setIcon(mEntry.getIconStandard());
            }
        }
        newEntry.setUrl(entryUrlView.getText().toString());
        newEntry.setUsername(entryUserNameView.getText().toString());
        newEntry.setNotes(entryCommentView.getText().toString());
        newEntry.setPassword(entryPasswordView.getText().toString());

        if (newEntry.allowExtraFields()) {
            // Delete all extra strings
            newEntry.removeAllCustomFields();
            // Add extra fields from views
            for (int i = 0; i < entryExtraFieldsContainer.getChildCount(); i++) {
                EntryEditCustomField view = (EntryEditCustomField) entryExtraFieldsContainer.getChildAt(i);
                String key = view.getLabel();
                String value = view.getValue();
                boolean protect = view.isProtected();
                newEntry.addExtraField(key, new ProtectedString(protect, value));
            }
        }

        newEntry.endToManageFieldReferences();

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
		ImageButton currIconButton = findViewById(R.id.icon_button);
		App.getDB().getDrawFactory().assignDrawableTo(currIconButton, getResources(), mEntry.getIcon());

		// Don't start the field reference manager, we want to see the raw ref
        mEntry.endToManageFieldReferences();

        entryTitleView.setText(mEntry.getTitle());
        entryUserNameView.setText(mEntry.getUsername());
        entryUrlView.setText(mEntry.getUrl());
        String password = mEntry.getPassword();
        entryPasswordView.setText(password);
        entryConfirmationPasswordView.setText(password);
        entryCommentView.setText(mEntry.getNotes());

        boolean visibilityFontActivated = PreferencesUtil.fieldFontIsInVisibility(this);
        if (visibilityFontActivated) {
            Util.applyFontVisibilityTo(entryUserNameView);
            Util.applyFontVisibilityTo(entryPasswordView);
            Util.applyFontVisibilityTo(entryConfirmationPasswordView);
            Util.applyFontVisibilityTo(entryCommentView);
        }

		if (mEntry.allowExtraFields()) {
            LinearLayout container = findViewById(R.id.advanced_container);
            mEntry.getFields().doActionToAllCustomProtectedField((key, value) -> {
                EntryEditCustomField entryEditCustomField = new EntryEditCustomField(EntryEditActivity.this);
                entryEditCustomField.setData(key, value);
                entryEditCustomField.setFontVisibility(visibilityFontActivated);
                container.addView(entryEditCustomField);
            });
        }
	}

    @Override
    public void iconPicked(Bundle bundle) {
        mSelectedIconID = bundle.getInt(IconPickerDialogFragment.KEY_ICON_ID);
        ImageButton currIconButton = findViewById(R.id.icon_button);
        currIconButton.setImageResource(Icons.iconToResId(mSelectedIconID));
    }

    @Override
    public void acceptPassword(Bundle bundle) {
        String generatedPassword = bundle.getString(GeneratePasswordDialogFragment.KEY_PASSWORD_ID);
        entryPasswordView.setText(generatedPassword);
        entryConfirmationPasswordView.setText(generatedPassword);
    }

    @Override
    public void cancelPassword(Bundle bundle) {
        // Do nothing here
    }

	@Override
	public void finish() {
	    // Assign entry callback as a result in all case
        try {
            if (mCallbackNewEntry != null) {
                Bundle bundle = new Bundle();
                Intent intentEntry = new Intent();
                bundle.putSerializable(ADD_OR_UPDATE_ENTRY_KEY, mCallbackNewEntry);
                intentEntry.putExtras(bundle);
                if (mIsNew) {
                    setResult(ADD_ENTRY_RESULT_CODE, intentEntry);
                } else {
                    setResult(UPDATE_ENTRY_RESULT_CODE, intentEntry);
                }
            }
            super.finish();
        } catch (Exception e) {
            // Exception when parcelable can't be done
            Log.e(TAG, "Cant add entry as result", e);
        }
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
