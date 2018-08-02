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
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.app.App;
import com.kunzisoft.keepass.database.Database;
import com.kunzisoft.keepass.database.ExtraFields;
import com.kunzisoft.keepass.database.PwDatabase;
import com.kunzisoft.keepass.database.PwEntry;
import com.kunzisoft.keepass.database.security.ProtectedString;
import com.kunzisoft.keepass.lock.LockingActivity;
import com.kunzisoft.keepass.lock.LockingHideActivity;
import com.kunzisoft.keepass.notifications.NotificationCopyingService;
import com.kunzisoft.keepass.notifications.NotificationField;
import com.kunzisoft.keepass.settings.PreferencesUtil;
import com.kunzisoft.keepass.settings.SettingsAutofillActivity;
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
import static com.kunzisoft.keepass.settings.PreferencesUtil.isFirstTimeAskAllowCopyPasswordAndProtectedFields;

public class EntryActivity extends LockingHideActivity {
    private final static String TAG = EntryActivity.class.getName();

	public static final String KEY_ENTRY = "entry";

	private ImageView titleIconView;
    private TextView titleView;
	private EntryContentsView entryContentsView;
    private Toolbar toolbar;
	
	protected PwEntry mEntry;
	private boolean mShowPassword;

	private ClipboardHelper clipboardHelper;
	private boolean firstLaunchOfActivity;

	private int iconColor;

    public static void launch(Activity act, PwEntry pw, boolean readOnly) {
        if (LockingActivity.checkTimeIsAllowedOrFinish(act)) {
            Intent intent = new Intent(act, EntryActivity.class);
            intent.putExtra(KEY_ENTRY, Types.UUIDtoBytes(pw.getUUID()));
            ReadOnlyHelper.putReadOnlyInIntent(intent, readOnly);
            act.startActivityForResult(intent, EntryEditActivity.ADD_OR_UPDATE_ENTRY_REQUEST_CODE);
        }
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        setContentView(R.layout.entry_view);

        toolbar = findViewById(R.id.toolbar);
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
		readOnly = db.isReadOnly() || readOnly;

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

        // Retrieve the textColor to tint the icon
        int[] attrs = {R.attr.textColorInverse};
        TypedArray ta = getTheme().obtainStyledAttributes(attrs);
        iconColor = ta.getColor(0, Color.WHITE);
		
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

        boolean containsUsernameToCopy =
                mEntry.getUsername().length() > 0;
        boolean containsPasswordToCopy =
                (mEntry.getPassword().length() > 0
                        && PreferencesUtil.allowCopyPasswordAndProtectedFields(this));
        boolean containsExtraFieldToCopy =
                (mEntry.allowExtraFields()
                        && ((mEntry.containsCustomFields()
                                && mEntry.containsCustomFieldsNotProtected())
                            || (mEntry.containsCustomFields()
                                && mEntry.containsCustomFieldsProtected()
                                && PreferencesUtil.allowCopyPasswordAndProtectedFields(this))
                        )
                );

        // If notifications enabled in settings
        // Don't if application timeout
        if (firstLaunchOfActivity && !App.isShutdown() && isClipboardNotificationsEnable(getApplicationContext())) {
            if (containsUsernameToCopy
                    || containsPasswordToCopy
                    || containsExtraFieldToCopy
                    ) {
                // username already copied, waiting for user's action before copy password.
                Intent intent = new Intent(this, NotificationCopyingService.class);
                intent.setAction(NotificationCopyingService.ACTION_NEW_NOTIFICATION);
                if (mEntry.getTitle() != null)
                    intent.putExtra(NotificationCopyingService.EXTRA_ENTRY_TITLE, mEntry.getTitle());
                // Construct notification fields
                ArrayList<NotificationField> notificationFields = new ArrayList<>();
                // Add username if exists to notifications
                if (containsUsernameToCopy)
                    notificationFields.add(
                            new NotificationField(
                                    NotificationField.NotificationFieldId.USERNAME,
                                    mEntry.getUsername(),
                                    getResources()));
                // Add password to notifications
                if (containsPasswordToCopy) {
                    notificationFields.add(
                            new NotificationField(
                                    NotificationField.NotificationFieldId.PASSWORD,
                                    mEntry.getPassword(),
                                    getResources()));
                }
                // Add extra fields
                if (containsExtraFieldToCopy) {
                    try {
                        mEntry.getFields().doActionToAllCustomProtectedField(new ExtraFields.ActionProtected() {
                            private int anonymousFieldNumber = 0;
                            @Override
                            public void doAction(String key, ProtectedString value) {
                                //If value is not protected or allowed
                                if (!value.isProtected() || PreferencesUtil.allowCopyPasswordAndProtectedFields(EntryActivity.this)) {
                                    notificationFields.add(
                                            new NotificationField(
                                                    NotificationField.NotificationFieldId.getAnonymousFieldId()[anonymousFieldNumber],
                                                    value.toString(),
                                                    key,
                                                    getResources()));
                                    anonymousFieldNumber++;
                                }
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
            mEntry.stopToManageFieldReferences();
        }
        firstLaunchOfActivity = false;
    }

    /**
     * Check and display learning views
     * Displays the explanation for copying a field and editing an entry
     */
    private void checkAndPerformedEducation(Menu menu) {
        if (PreferencesUtil.isEducationScreensEnabled(this)) {

            if (entryContentsView != null && entryContentsView.isUserNamePresent()
                    && !PreferencesUtil.isEducationCopyUsernamePerformed(this)) {
                TapTargetView.showFor(this,
                        TapTarget.forView(findViewById(R.id.entry_user_name_action_image),
                                getString(R.string.education_field_copy_title),
                                getString(R.string.education_field_copy_summary))
                                .textColorInt(Color.WHITE)
                                .tintTarget(false)
                                .cancelable(true),
                        new TapTargetView.Listener() {
                            @Override
                            public void onTargetClick(TapTargetView view) {
                                super.onTargetClick(view);
                                clipboardHelper.timeoutCopyToClipboard(mEntry.getUsername(),
                                        getString(R.string.copy_field, getString(R.string.entry_user_name)));
                            }

                            @Override
                            public void onOuterCircleClick(TapTargetView view) {
                                super.onOuterCircleClick(view);
                                view.dismiss(false);
                                // Launch autofill settings
                                startActivity(new Intent(EntryActivity.this, SettingsAutofillActivity.class));
                            }
                        });
                PreferencesUtil.saveEducationPreference(this,
                        R.string.education_copy_username_key);

            } else if (!PreferencesUtil.isEducationEntryEditPerformed(this)) {

                try {
                    TapTargetView.showFor(this,
                            TapTarget.forToolbarMenuItem(toolbar, R.id.menu_edit,
                                    getString(R.string.education_entry_edit_title),
                                    getString(R.string.education_entry_edit_summary))
                                    .textColorInt(Color.WHITE)
                                    .tintTarget(true)
                                    .cancelable(true),
                            new TapTargetView.Listener() {
                                @Override
                                public void onTargetClick(TapTargetView view) {
                                    super.onTargetClick(view);
                                    MenuItem editItem = menu.findItem(R.id.menu_edit);
                                    onOptionsItemSelected(editItem);
                                }

                                @Override
                                public void onOuterCircleClick(TapTargetView view) {
                                    super.onOuterCircleClick(view);
                                    view.dismiss(false);
                                    // Open Keepass doc to create field references
                                    Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                                            Uri.parse(getString(R.string.field_references_url)));
                                    startActivity(browserIntent);
                                }
                            });
                    PreferencesUtil.saveEducationPreference(this,
                            R.string.education_entry_edit_key);
                } catch (Exception e) {
                    // If icon not visible
                    Log.w(TAG, "Can't performed education for entry's edition");
                }
            }
        }
    }

	protected void fillData() {
		Database db = App.getDB();
		PwDatabase pm = db.getPwDatabase();

		mEntry.startToManageFieldReferences(pm);

        // Assign title icon
        db.getDrawFactory().assignDatabaseIconTo(this, titleIconView, mEntry.getIcon(), iconColor);

		// Assign title text
        titleView.setText(mEntry.getVisualTitle());

        // Assign basic fields
        entryContentsView.assignUserName(mEntry.getUsername());
        entryContentsView.assignUserNameCopyListener(view ->
                clipboardHelper.timeoutCopyToClipboard(mEntry.getUsername(),
                getString(R.string.copy_field, getString(R.string.entry_user_name)))
        );

        boolean allowCopyPassword = PreferencesUtil.allowCopyPasswordAndProtectedFields(this);
		entryContentsView.assignPassword(mEntry.getPassword(), allowCopyPassword);
		if (allowCopyPassword) {
            entryContentsView.assignPasswordCopyListener(view ->
                    clipboardHelper.timeoutCopyToClipboard(mEntry.getPassword(),
                            getString(R.string.copy_field, getString(R.string.entry_password)))
            );
        } else {
		    // If dialog not already shown
            if (isFirstTimeAskAllowCopyPasswordAndProtectedFields(this)) {
                entryContentsView.assignPasswordCopyListener(v -> {
                    String message = getString(R.string.allow_copy_password_warning) +
                            "\n\n" +
                            getString(R.string.clipboard_warning);
                    AlertDialog warningDialog = new AlertDialog.Builder(EntryActivity.this)
                            .setMessage(message).create();
                    warningDialog.setButton(AlertDialog.BUTTON1, getText(android.R.string.ok),
                            (dialog, which) -> {
                                PreferencesUtil.setAllowCopyPasswordAndProtectedFields(EntryActivity.this, true);
                                dialog.dismiss();
                                fillData();
                            });
                    warningDialog.setButton(AlertDialog.BUTTON2, getText(android.R.string.cancel),
                            (dialog, which) -> {
                                PreferencesUtil.setAllowCopyPasswordAndProtectedFields(EntryActivity.this, false);
                                dialog.dismiss();
                                fillData();
                            });
                    warningDialog.show();
                });
            } else {
                entryContentsView.assignPasswordCopyListener(null);
            }
        }

        entryContentsView.assignURL(mEntry.getUrl());

        entryContentsView.setHiddenPasswordStyle(!mShowPassword);
        entryContentsView.assignComment(mEntry.getNotes());

        // Assign custom fields
		if (mEntry.allowExtraFields()) {
			entryContentsView.clearExtraFields();

			mEntry.getFields().doActionToAllCustomProtectedField((label, value) -> {
			        boolean showAction = (!value.isProtected() || PreferencesUtil.allowCopyPasswordAndProtectedFields(EntryActivity.this));
                    entryContentsView.addExtraField(label, value, showAction, view ->
                        clipboardHelper.timeoutCopyToClipboard(
                                value.toString(),
                                getString(R.string.copy_field, label)
                        )
                    );
            });
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

        mEntry.stopToManageFieldReferences();
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
        MenuUtil.contributionMenuInflater(inflater, menu);
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

        // Show education views
        new Handler().post(() -> checkAndPerformedEducation(menu));
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch ( item.getItemId() ) {
            case R.id.menu_contribute:
                return MenuUtil.onContributionItemSelected(this);

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
