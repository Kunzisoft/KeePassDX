/*
 * 
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
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
package com.kunzisoft.keepass.activities;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.app.App;
import com.kunzisoft.keepass.database.Database;
import com.kunzisoft.keepass.database.ExtraFields;
import com.kunzisoft.keepass.database.PwDatabase;
import com.kunzisoft.keepass.database.PwEntry;
import com.kunzisoft.keepass.database.security.ProtectedString;
import com.kunzisoft.keepass.notifications.NotificationCopyingService;
import com.kunzisoft.keepass.notifications.NotificationField;
import com.kunzisoft.keepass.settings.PreferencesUtil;
import com.kunzisoft.keepass.timeout.ClipboardHelper;
import com.kunzisoft.keepass.utils.EmptyUtils;
import com.kunzisoft.keepass.utils.MenuUtil;
import com.kunzisoft.keepass.utils.Types;
import com.kunzisoft.keepass.utils.Util;
import com.kunzisoft.keepass.view.EntryContentsView;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import static com.kunzisoft.keepass.settings.PreferencesUtil.isClipboardNotificationsEnable;

public class EntryActivity extends LockingHideActivity {
    private final static String TAG = EntryActivity.class.getName();

	public static final String KEY_ENTRY = "entry";

	private ImageView titleIconView;
    private TextView titleView;
	private EntryContentsView entryContentsView;
	
	protected PwEntry mEntry;
	private boolean mShowPassword;
	protected boolean readOnly = false;

	private ClipboardHelper clipboardHelper;
	private boolean firstLaunchOfActivity;

    public static void launch(Activity act, PwEntry pw) {
        if (LockingActivity.checkTimeIsAllowedOrFinish(act)) {
            Intent intent = new Intent(act, EntryActivity.class);
            intent.putExtra(KEY_ENTRY, Types.UUIDtoBytes(pw.getUUID()));
            act.startActivityForResult(intent, EntryEditActivity.ADD_OR_UPDATE_ENTRY_REQUEST_CODE);
        }
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        setContentView(R.layout.entry_view);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        assert getSupportActionBar() != null;
		getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

		Database db = App.getDB();
		// Likely the app has been killed exit the activity 
		if ( ! db.getLoaded() ) {
			finish();
			return;
		}
		readOnly = db.isReadOnly();

        mShowPassword = !PreferencesUtil.isPasswordMask(this);

		// Get Entry from UUID
		Intent i = getIntent();
		UUID uuid = Types.bytestoUUID(i.getByteArrayExtra(KEY_ENTRY));
		mEntry = db.getPwDatabase().getEntryByUUIDId(uuid);
		if (mEntry == null) {
			Toast.makeText(this, R.string.entry_not_found, Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		
		// Refresh Menu contents in case onCreateMenuOptions was called before mEntry was set
		invalidateOptionsMenu();
		
		// Update last access time.
		mEntry.touch(false, false);

        // Get views
        titleIconView = findViewById(R.id.entry_icon);
        titleView = findViewById(R.id.entry_title);
        entryContentsView = findViewById(R.id.entry_contents);
        entryContentsView.applyFontVisibilityToFields(PreferencesUtil.fieldFontIsInVisibility(this));

        // Init the clipboard helper
        clipboardHelper = new ClipboardHelper(this);
        firstLaunchOfActivity = true;
	}

    @Override
    protected void onResume() {
        super.onResume();

        // Fill data in resume to update from EntryEditActivity
        fillData();
        invalidateOptionsMenu();

        // Start to manage field reference to copy a value from ref
        mEntry.startToManageFieldReferences(App.getDB().getPwDatabase());

        // If notifications enabled in settings
        // Don't if application timeout
        if (firstLaunchOfActivity && !App.isShutdown() && isClipboardNotificationsEnable(getApplicationContext())) {
            if (mEntry.getUsername().length() > 0
                    || (mEntry.getPassword().length() > 0 && PreferencesUtil.allowCopyPassword(this))
                    || mEntry.containsCustomFields()) {
                // username already copied, waiting for user's action before copy password.
                Intent intent = new Intent(this, NotificationCopyingService.class);
                intent.setAction(NotificationCopyingService.ACTION_NEW_NOTIFICATION);
                if (mEntry.getTitle() != null)
                    intent.putExtra(NotificationCopyingService.EXTRA_ENTRY_TITLE, mEntry.getTitle());
                // Construct notification fields
                ArrayList<NotificationField> notificationFields = new ArrayList<>();
                // Add username if exists to notifications
                if (mEntry.getUsername().length() > 0)
                    notificationFields.add(
                            new NotificationField(
                                    NotificationField.NotificationFieldId.USERNAME,
                                    mEntry.getUsername(),
                                    getResources()));
                // Add password to notifications
                if (PreferencesUtil.allowCopyPassword(this)) {
                    if (mEntry.getPassword().length() > 0)
                        notificationFields.add(
                                new NotificationField(
                                        NotificationField.NotificationFieldId.PASSWORD,
                                        mEntry.getPassword(),
                                        getResources()));
                }
                // Add extra fields
                if (mEntry.allowExtraFields()) {
                    try {
                        mEntry.getFields().doActionToAllCustomProtectedField(new ExtraFields.ActionProtected() {
                            private int anonymousFieldNumber = 0;
                            @Override
                            public void doAction(String key, ProtectedString value) {
                                notificationFields.add(
                                        new NotificationField(
                                                NotificationField.NotificationFieldId.getAnonymousFieldId()[anonymousFieldNumber],
                                                value.toString(),
                                                key,
                                                getResources()));
                                anonymousFieldNumber++;
                            }
                        });
                    } catch (ArrayIndexOutOfBoundsException e) {
                        Log.w(TAG, "Only " + NotificationField.NotificationFieldId.getAnonymousFieldId().length +
                                " anonymous notifications are available");
                    }
                }
                // Add notifications
                intent.putParcelableArrayListExtra(NotificationCopyingService.EXTRA_FIELDS, notificationFields);

                startService(intent);
            }
            mEntry.endToManageFieldReferences();
        }
        firstLaunchOfActivity = false;
    }

    private void populateTitle(Drawable drawIcon, String text) {
        titleIconView.setImageDrawable(drawIcon);
        titleView.setText(text);
    }

	protected void fillData() {
		Database db = App.getDB();
		PwDatabase pm = db.getPwDatabase();

		mEntry.startToManageFieldReferences(pm);

		// Assign title
        populateTitle(db.getDrawFactory().getIconDrawable(getResources(), mEntry.getIcon()),
                mEntry.getTitle());

        // Assign basic fields
        entryContentsView.assignUserName(mEntry.getUsername());
        entryContentsView.assignUserNameCopyListener(view ->
                clipboardHelper.timeoutCopyToClipboard(mEntry.getUsername(),
                getString(R.string.copy_field, getString(R.string.entry_user_name)))
        );

		entryContentsView.assignPassword(mEntry.getPassword());
		if (PreferencesUtil.allowCopyPassword(this)) {
            entryContentsView.assignPasswordCopyListener(view ->
                    clipboardHelper.timeoutCopyToClipboard(mEntry.getPassword(),
                            getString(R.string.copy_field, getString(R.string.entry_password)))
            );
        }

        entryContentsView.assignURL(mEntry.getUrl());

        entryContentsView.setHiddenPasswordStyle(!mShowPassword);
        entryContentsView.assignComment(mEntry.getNotes());

        // Assign custom fields
		if (mEntry.allowExtraFields()) {
			entryContentsView.clearExtraFields();

			mEntry.getFields().doActionToAllCustomProtectedField((label, value) ->

                    entryContentsView.addExtraField(label, value, view ->
                        clipboardHelper.timeoutCopyToClipboard(
                                value.toString(),
                                getString(R.string.copy_field, label)
                        )
            ));
		}

        // Assign dates
        entryContentsView.assignCreationDate(mEntry.getCreationTime().getDate());
        entryContentsView.assignModificationDate(mEntry.getLastModificationTime().getDate());
        entryContentsView.assignLastAccessDate(mEntry.getLastAccessTime().getDate());
		Date expires = mEntry.getExpiryTime().getDate();
		if ( mEntry.isExpires() ) {
			entryContentsView.assignExpiresDate(expires);
		} else {
            entryContentsView.assignExpiresDate(getString(R.string.never));
		}

        mEntry.endToManageFieldReferences();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case EntryEditActivity.ADD_OR_UPDATE_ENTRY_REQUEST_CODE:
                fillData();
                break;
        }
	}

	private void changeShowPasswordIcon(MenuItem togglePassword) {
		if ( mShowPassword ) {
			togglePassword.setTitle(R.string.menu_hide_password);
			togglePassword.setIcon(R.drawable.ic_visibility_off_white_24dp);
		} else {
			togglePassword.setTitle(R.string.menu_showpass);
			togglePassword.setIcon(R.drawable.ic_visibility_white_24dp);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		MenuInflater inflater = getMenuInflater();
        MenuUtil.donationMenuInflater(inflater, menu);
		inflater.inflate(R.menu.entry, menu);
		inflater.inflate(R.menu.database_lock, menu);

        if (readOnly) {
            MenuItem edit =  menu.findItem(R.id.menu_edit);
            if (edit != null)
                edit.setVisible(false);
        }

		MenuItem togglePassword = menu.findItem(R.id.menu_toggle_pass);
		if (entryContentsView != null && togglePassword != null) {
            if (entryContentsView.isPasswordPresent() || entryContentsView.atLeastOneFieldProtectedPresent()) {
                changeShowPasswordIcon(togglePassword);
            } else {
                togglePassword.setVisible(false);
            }
        }
		
		MenuItem gotoUrl = menu.findItem(R.id.menu_goto_url);
		if (gotoUrl != null) {
            // In API >= 11 onCreateOptionsMenu may be called before onCreate completes
            // so mEntry may not be set
            if (mEntry == null) {
                gotoUrl.setVisible(false);
            } else {
                String url = mEntry.getUrl();
                if (EmptyUtils.isNullOrEmpty(url)) {
                    // disable button if url is not available
                    gotoUrl.setVisible(false);
                }
            }
        }
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch ( item.getItemId() ) {
            case R.id.menu_donate:
                return MenuUtil.onDonationItemSelected(this);

            case R.id.menu_toggle_pass:
                mShowPassword = !mShowPassword;
                changeShowPasswordIcon(item);
                entryContentsView.setHiddenPasswordStyle(!mShowPassword);
                return true;

            case R.id.menu_edit:
                EntryEditActivity.launch(EntryActivity.this, mEntry);
                return true;
			
            case R.id.menu_goto_url:
                String url;
                url = mEntry.getUrl();

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
			
            case R.id.menu_lock:
                lockAndExit();
                return true;

            case android.R.id.home :
                finish(); // close this activity and return to preview activity (if there is any)
        }

		return super.onOptionsItemSelected(item);
	}


    @Override
    public void finish() {
        // Transit data in previous Activity after an update
		/*
		TODO Slowdown when add entry as result
        Intent intent = new Intent();
        intent.putExtra(EntryEditActivity.ADD_OR_UPDATE_ENTRY_KEY, mEntry);
        setResult(EntryEditActivity.UPDATE_ENTRY_RESULT_CODE, intent);
        */
        super.finish();
    }
}
