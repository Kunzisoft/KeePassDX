/*
 * 
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
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.keepassdroid.app.App;
import com.keepassdroid.compat.ActivityCompat;
import com.keepassdroid.database.Database;
import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.PwEntry;
import com.keepassdroid.notifications.NotificationCopyingService;
import com.keepassdroid.notifications.NotificationField;
import com.keepassdroid.password.PasswordActivity;
import com.keepassdroid.settings.PreferencesUtil;
import com.keepassdroid.timeout.ClipboardHelper;
import com.keepassdroid.utils.EmptyUtils;
import com.keepassdroid.utils.MenuUtil;
import com.keepassdroid.utils.Types;
import com.keepassdroid.utils.Util;
import com.keepassdroid.view.EntryContentsView;
import com.kunzisoft.keepass.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static com.keepassdroid.settings.PreferencesUtil.isClipboardNotificationsEnable;

public class EntryActivity extends LockingHideActivity {
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
        Intent intent = new Intent(act, EntryActivity.class);
        intent.putExtra(KEY_ENTRY, Types.UUIDtoBytes(pw.getUUID()));
        act.startActivityForResult(intent, EntryEditActivity.ADD_OR_UPDATE_ENTRY_REQUEST_CODE);
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        setContentView(R.layout.entry_view);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        assert getSupportActionBar() != null;
		getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

		Database db = App.getDB();
		// Likely the app has been killed exit the activity 
		if ( ! db.Loaded() ) {
			finish();
			return;
		}
		readOnly = db.readOnly;

        mShowPassword = !PreferencesUtil.isPasswordMask(this);

		// Get Entry from UUID
		Intent i = getIntent();
		UUID uuid = Types.bytestoUUID(i.getByteArrayExtra(KEY_ENTRY));
		mEntry = db.pm.entries.get(uuid);
		if (mEntry == null) {
			Toast.makeText(this, R.string.entry_not_found, Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		
		// Refresh Menu contents in case onCreateMenuOptions was called before mEntry was set
		ActivityCompat.invalidateOptionsMenu(this);
		
		// Update last access time.
		mEntry.touch(false, false);

        // Get views
        titleIconView = (ImageView) findViewById(R.id.entry_icon);
        titleView = (TextView) findViewById(R.id.entry_title);
        entryContentsView = (EntryContentsView) findViewById(R.id.entry_contents);
        entryContentsView.applyFontVisibilityToFields(PreferencesUtil.fieldFontIsInVisibility(this));

		fillData();

		// Setup Edit Buttons
        View edit = findViewById(R.id.entry_edit);
        edit.setOnClickListener(v -> EntryEditActivity.Launch(EntryActivity.this, mEntry));
        if (readOnly) {
            edit.setVisibility(View.GONE);
        }

        // Init the clipboard helper
        clipboardHelper = new ClipboardHelper(this);
        firstLaunchOfActivity = true;
	}

    @Override
    protected void onResume() {
        super.onResume();

        // If notifications enabled in settings
        // Don't if application timeout
        if (firstLaunchOfActivity && !App.isShutdown() && isClipboardNotificationsEnable(getApplicationContext())) {
            if (mEntry.getPassword().length() > 0) {
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
                notificationFields.add(
                        new NotificationField(
                                NotificationField.NotificationFieldId.PASSWORD,
                                mEntry.getPassword(),
                                getResources()));
                // Add notifications
                intent.putParcelableArrayListExtra(NotificationCopyingService.EXTRA_FIELDS, notificationFields);

                startService(intent);
            }
        }
        firstLaunchOfActivity = false;
    }

    private void populateTitle(Drawable drawIcon, String text) {
        titleIconView.setImageDrawable(drawIcon);
        titleView.setText(text);
    }

	protected void fillData() {
		Database db = App.getDB();
		PwDatabase pm = db.pm;

		// Assign title
        populateTitle(db.drawFactory.getIconDrawable(getResources(), mEntry.getIcon()),
                mEntry.getTitle(true, pm));

        // Assign basic fields
        entryContentsView.assignUserName(mEntry.getUsername(true, pm));
        entryContentsView.assignUserNameCopyListener(view ->
                clipboardHelper.timeoutCopyToClipboard(mEntry.getUsername(true, App.getDB().pm),
                getString(R.string.copy_field, getString(R.string.entry_user_name)))
        );

		entryContentsView.assignPassword(mEntry.getPassword(true, pm));
		entryContentsView.assignPasswordCopyListener(view ->
                clipboardHelper.timeoutCopyToClipboard(mEntry.getPassword(true, App.getDB().pm),
                        getString(R.string.copy_field, getString(R.string.entry_password)))
        );

        entryContentsView.assignURL(mEntry.getUrl(true, pm));

        entryContentsView.setHiddenPasswordStyle(!mShowPassword);
        entryContentsView.assignComment(mEntry.getNotes(true, pm));

        // Assign custom fields
		if (mEntry.allowExtraFields()) {
			entryContentsView.clearExtraFields();
			for (Map.Entry<String, String> field : mEntry.getExtraFields(pm).entrySet()) {
                final String label = field.getKey();
                final String value = field.getValue();
				entryContentsView.addExtraField(label, value, view ->
                        clipboardHelper.timeoutCopyToClipboard(value, getString(R.string.copy_field, label)));
			}
		}

        // Assign dates
        entryContentsView.assignCreationDate(mEntry.getCreationTime());
        entryContentsView.assignModificationDate(mEntry.getLastModificationTime());
        entryContentsView.assignLastAccessDate(mEntry.getLastAccessTime());
		Date expires = mEntry.getExpiryTime();
		if ( mEntry.expires() ) {
			entryContentsView.assignExpiresDate(expires);
		} else {
            entryContentsView.assignExpiresDate(getString(R.string.never));
		}
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

		MenuItem togglePassword = menu.findItem(R.id.menu_toggle_pass);
		if (entryContentsView != null && togglePassword != null) {
            if (!entryContentsView.isPasswordPresent()) {
                togglePassword.setVisible(false);
            } else {
                changeShowPasswordIcon(togglePassword);
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
                App.setShutdown();
                setResult(PasswordActivity.RESULT_EXIT_LOCK);
                finish();
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
