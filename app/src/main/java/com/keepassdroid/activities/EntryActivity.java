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
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
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
import com.keepassdroid.database.exception.SamsungClipboardException;
import com.keepassdroid.intents.Intents;
import com.keepassdroid.settings.PreferencesUtil;
import com.keepassdroid.tasks.UIToastTask;
import com.keepassdroid.utils.EmptyUtils;
import com.keepassdroid.utils.MenuUtil;
import com.keepassdroid.utils.Types;
import com.keepassdroid.utils.Util;
import com.keepassdroid.view.EntryContentsView;
import com.kunzisoft.keepass.KeePass;
import com.kunzisoft.keepass.R;

import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static com.keepassdroid.settings.PreferencesUtil.isClipboardNotificationsEnable;

public class EntryActivity extends LockCloseHideActivity {
	public static final String KEY_ENTRY = "entry";

	public static final int NOTIFY_USERNAME = 1;
	public static final int NOTIFY_PASSWORD = 2;

	private ImageView titleIconView;
    private TextView titleView;
	private EntryContentsView entryContentsView;
	
	protected PwEntry mEntry;
	private Timer mTimer = new Timer();
	private boolean mShowPassword;
	private NotificationManager mNM;
	private BroadcastReceiver mIntentReceiver;
	protected boolean readOnly = false;

    public static void Launch(Activity act, PwEntry pw) {
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

		setResult(KeePass.EXIT_NORMAL);

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
        edit.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                EntryEditActivity.Launch(EntryActivity.this, mEntry);
            }

        });
        if (readOnly) {
            edit.setVisibility(View.GONE);
        }

		// If notifications enabled in settings
		if (isClipboardNotificationsEnable(getApplicationContext())) {
            // Notification Manager
            mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            if (mEntry.getPassword().length() > 0) {
                // only show notification if password is available
                Notification password = getNotification(Intents.COPY_PASSWORD, R.string.copy_password);
                mNM.notify(NOTIFY_PASSWORD, password);
            }

            if (mEntry.getUsername().length() > 0) {
                // only show notification if username is available
                Notification username = getNotification(Intents.COPY_USERNAME, R.string.copy_username);
                mNM.notify(NOTIFY_USERNAME, username);
            }
        }
			
		mIntentReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if ( action != null) {
					if (action.equals(Intents.COPY_USERNAME)) {
						String username = mEntry.getUsername();
						if (username.length() > 0) {
							timeoutCopyToClipboard(username);
						}
					} else if (action.equals(Intents.COPY_PASSWORD)) {
						String password = mEntry.getPassword();
						if (password.length() > 0) {
							timeoutCopyToClipboard(password);
						}
					}
				}
			}
		};
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intents.COPY_USERNAME);
		filter.addAction(Intents.COPY_PASSWORD);
		registerReceiver(mIntentReceiver, filter);
	}
	
	@Override
	protected void onDestroy() {
		// These members might never get initialized if the app timed out
		if ( mIntentReceiver != null ) {
			unregisterReceiver(mIntentReceiver);
		}
		
		if ( mNM != null ) {
			try {
			    mNM.cancelAll();
			} catch (SecurityException e) {
				// Some android devices give a SecurityException when trying to cancel notifications without the WAKE_LOCK permission,
				// we'll ignore these.
			}
		}
		
		super.onDestroy();
	}

	private Notification getNotification(String intentText, int descResId) {

		String desc = getString(descResId);

		Intent intent = new Intent(intentText);
		PendingIntent pending = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

		// no longer supported for api level >22
		// notify.setLatestEventInfo(this, getString(R.string.app_name), desc, pending);
		// so instead using compat builder and create new notification
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		Notification notify = builder.setContentIntent(pending).setContentText(desc).setContentTitle(getString(R.string.app_name))
				.setSmallIcon(R.drawable.notify).setTicker(desc).setWhen(System.currentTimeMillis()).build();

		return notify;
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
        entryContentsView.assignUserNameCopyListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				timeoutCopyToClipboard(mEntry.getUsername(true, App.getDB().pm),
                        getString(R.string.copy_field, getString(R.string.entry_user_name)));
			}
		});

		entryContentsView.assignPassword(mEntry.getPassword(true, pm));
		entryContentsView.assignPasswordCopyListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				timeoutCopyToClipboard(mEntry.getPassword(true, App.getDB().pm),
                        getString(R.string.copy_field, getString(R.string.entry_password)));
			}
		});

        entryContentsView.assignURL(mEntry.getUrl(true, pm));

        entryContentsView.setHiddenPasswordStyle(!mShowPassword);
        entryContentsView.assignComment(mEntry.getNotes(true, pm));

        // Assign custom fields
		if (mEntry.allowExtraFields()) {
			entryContentsView.clearExtraFields();
			for (Map.Entry<String, String> field : mEntry.getExtraFields(pm).entrySet()) {
                final String label = field.getKey();
                final String value = field.getValue();
				entryContentsView.addExtraField(label, value, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        timeoutCopyToClipboard(value, getString(R.string.copy_field, label));
                    }
                });
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
		if (!entryContentsView.isPasswordPresent()) {
		    togglePassword.setVisible(false);
        } else {
            changeShowPasswordIcon(togglePassword);
        }
		
		MenuItem gotoUrl = menu.findItem(R.id.menu_goto_url);
		
		// In API >= 11 onCreateOptionsMenu may be called before onCreate completes
		// so mEntry may not be set
		if (mEntry == null) {
			gotoUrl.setVisible(false);
		}
		else {
			String url = mEntry.getUrl();
			if (EmptyUtils.isNullOrEmpty(url)) {
				// disable button if url is not available
				gotoUrl.setVisible(false);
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
                setResult(KeePass.EXIT_LOCK);
                finish();
                return true;

            case android.R.id.home :
                finish(); // close this activity and return to preview activity (if there is any)
        }

		return super.onOptionsItemSelected(item);
	}

	private void timeoutCopyToClipboard(String text) {
        timeoutCopyToClipboard(text, "");
    }

	private void timeoutCopyToClipboard(String text, String toastString) {
        if (!toastString.isEmpty())
            Toast.makeText(EntryActivity.this, toastString, Toast.LENGTH_LONG).show();

		try {
			Util.copyToClipboard(this, text);
		} catch (SamsungClipboardException e) {
			showSamsungDialog();
			return;
		}
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String sClipClear = prefs.getString(getString(R.string.clipboard_timeout_key), getString(R.string.clipboard_timeout_default));
		
		long clipClearTime = Long.parseLong(sClipClear);
		
		if ( clipClearTime > 0 ) {
			mTimer.schedule(new ClearClipboardTask(this, text), clipClearTime);
		}
	}

    @Override
    public void finish() {
        // Transit data in previous Activity after an update
        Intent intent = new Intent();
        intent.putExtra(EntryEditActivity.ADD_OR_UPDATE_ENTRY_KEY, mEntry);
        setResult(EntryEditActivity.UPDATE_ENTRY_RESULT_CODE, intent);
        super.finish();
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
				try {
					Util.copyToClipboard(mCtx, "");
					uiThreadCallback.post(new UIToastTask(mCtx, R.string.ClearClipboard));
				} catch (SamsungClipboardException e) {
					uiThreadCallback.post(new UIToastTask(mCtx, R.string.clipboard_error_clear));
				}
			}
		}
	}
	
	private void showSamsungDialog() {
		String text = getString(R.string.clipboard_error).concat(System.getProperty("line.separator")).concat(getString(R.string.clipboard_error_url));
		SpannableString s = new SpannableString(text);
		TextView tv = new TextView(this);
		tv.setText(s);
		tv.setAutoLinkMask(RESULT_OK);
		tv.setMovementMethod(LinkMovementMethod.getInstance());
		Linkify.addLinks(s, Linkify.WEB_URLS);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.clipboard_error_title)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
			.setView(tv)
			.show();
		
	}
}
