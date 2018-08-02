/*
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
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.app.App;
import com.kunzisoft.keepass.database.Database;
import com.kunzisoft.keepass.database.PwDatabase;
import com.kunzisoft.keepass.database.PwDate;
import com.kunzisoft.keepass.database.PwEntry;
import com.kunzisoft.keepass.database.PwGroup;
import com.kunzisoft.keepass.database.PwGroupId;
import com.kunzisoft.keepass.database.PwIconStandard;
import com.kunzisoft.keepass.database.PwNode;
import com.kunzisoft.keepass.database.action.RunnableOnFinish;
import com.kunzisoft.keepass.database.action.node.AddEntryRunnable;
import com.kunzisoft.keepass.database.action.node.AfterActionNodeOnFinish;
import com.kunzisoft.keepass.database.action.node.UpdateEntryRunnable;
import com.kunzisoft.keepass.database.security.ProtectedString;
import com.kunzisoft.keepass.dialogs.GeneratePasswordDialogFragment;
import com.kunzisoft.keepass.dialogs.IconPickerDialogFragment;
import com.kunzisoft.keepass.lock.LockingActivity;
import com.kunzisoft.keepass.lock.LockingHideActivity;
import com.kunzisoft.keepass.settings.PreferencesUtil;
import com.kunzisoft.keepass.tasks.SaveDatabaseProgressTaskDialogFragment;
import com.kunzisoft.keepass.tasks.UpdateProgressTaskStatus;
import com.kunzisoft.keepass.utils.MenuUtil;
import com.kunzisoft.keepass.utils.Types;
import com.kunzisoft.keepass.utils.Util;
import com.kunzisoft.keepass.view.EntryEditCustomField;

import java.util.UUID;

import javax.annotation.Nullable;

import static com.kunzisoft.keepass.dialogs.IconPickerDialogFragment.KEY_ICON_STANDARD;

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

	private Database database;

	protected PwEntry mEntry;
	protected PwEntry mCallbackNewEntry;
	protected boolean mIsNew;
	protected PwIconStandard mSelectedIconStandard;

    // Views
    private ScrollView scrollView;
    private EditText entryTitleView;
    private ImageView entryIconView;
    private EditText entryUserNameView;
    private EditText entryUrlView;
    private EditText entryPasswordView;
    private EditText entryConfirmationPasswordView;
    private View generatePasswordView;
    private EditText entryCommentView;
    private ViewGroup entryExtraFieldsContainer;
    private View addNewFieldView;
    private View saveView;
    private int iconColor;

	/**
	 * Launch EntryEditActivity to update an existing entry
     *
	 * @param act from activity
	 * @param pw Entry to update
	 */
	public static void launch(Activity act, PwEntry pw) {
        if (LockingActivity.checkTimeIsAllowedOrFinish(act)) {
            Intent intent = new Intent(act, EntryEditActivity.class);
            intent.putExtra(KEY_ENTRY, Types.UUIDtoBytes(pw.getUUID()));
            act.startActivityForResult(intent, ADD_OR_UPDATE_ENTRY_REQUEST_CODE);
        }
	}

	/**
	 * Launch EntryEditActivity to add a new entry
     *
	 * @param act from activity
	 * @param pwGroup Group who will contains new entry
	 */
	public static void launch(Activity act, PwGroup pwGroup) {
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
        setSupportActionBar(toolbar);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        scrollView = findViewById(R.id.entry_scroll);
        scrollView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);

        entryTitleView = findViewById(R.id.entry_title);
        entryIconView = findViewById(R.id.icon_button);
        entryUserNameView = findViewById(R.id.entry_user_name);
        entryUrlView = findViewById(R.id.entry_url);
        entryPasswordView = findViewById(R.id.entry_password);
        entryConfirmationPasswordView = findViewById(R.id.entry_confpassword);
        entryCommentView = findViewById(R.id.entry_comment);
        entryExtraFieldsContainer = findViewById(R.id.advanced_container);
		
		// Likely the app has been killed exit the activity
        database = App.getDB();
		if ( ! database.getLoaded() ) {
			finish();
			return;
		}

		Intent intent = getIntent();
		byte[] uuidBytes = intent.getByteArrayExtra(KEY_ENTRY);

        // Retrieve the textColor to tint the icon
        int[] attrs = {android.R.attr.textColorPrimary};
        TypedArray ta = getTheme().obtainStyledAttributes(attrs);
        iconColor = ta.getColor(0, Color.WHITE);

        mSelectedIconStandard = database.getPwDatabase().getIconFactory().getUnknownIcon();

		PwDatabase pm = database.getPwDatabase();
		if ( uuidBytes == null ) {
            PwGroupId parentId = intent.getParcelableExtra(KEY_PARENT);
			PwGroup parent = pm.getGroupByGroupId(parentId);
			mEntry = database.createEntry(parent);
			mIsNew = true;
			// Add the default icon
            database.getDrawFactory().assignDefaultDatabaseIconTo(this, entryIconView, iconColor);
		} else {
			UUID uuid = Types.bytestoUUID(uuidBytes);
			mEntry = pm.getEntryByUUIDId(uuid);
			mIsNew = false;
			fillData();
		}

        // Assign title
        setTitle((mIsNew) ? getString(R.string.add_entry) : getString(R.string.edit_entry));

		// Retrieve the icon after an orientation change
		if (savedInstanceState != null
                && savedInstanceState.containsKey(KEY_ICON_STANDARD)) {
            iconPicked(savedInstanceState);
        }

		// Add listener to the icon
        entryIconView.setOnClickListener(v ->
                IconPickerDialogFragment.launch(EntryEditActivity.this));

		// Generate password button
        generatePasswordView = findViewById(R.id.generate_button);
        generatePasswordView.setOnClickListener(v -> openPasswordGenerator());
		
		// Save button
		saveView = findViewById(R.id.entry_save);
        saveView.setOnClickListener(v -> saveEntry());


		if (mEntry.allowExtraFields()) {
            addNewFieldView = findViewById(R.id.add_new_field);
            addNewFieldView.setVisibility(View.VISIBLE);
            addNewFieldView.setOnClickListener(v -> addNewCustomField());
        }

        // Verify the education views
        checkAndPerformedEducation();
	}

    /**
     * Open the password generator fragment
     */
	private void openPasswordGenerator() {
        GeneratePasswordDialogFragment generatePasswordDialogFragment = new GeneratePasswordDialogFragment();
        generatePasswordDialogFragment.show(getSupportFragmentManager(), "PasswordGeneratorFragment");
    }

    /**
     * Add a new view to fill in the information of the customized field
     */
    private void addNewCustomField() {
        EntryEditCustomField entryEditCustomField = new EntryEditCustomField(EntryEditActivity.this);
        entryEditCustomField.setData("", new ProtectedString(false, ""));
        boolean visibilityFontActivated = PreferencesUtil.fieldFontIsInVisibility(this);
        entryEditCustomField.setFontVisibility(visibilityFontActivated);
        entryExtraFieldsContainer.addView(entryEditCustomField);

        // Scroll bottom
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    /**
     * Saves the new entry or update an existing entry in the database
     */
    private void saveEntry() {
        if (!validateBeforeSaving()) {
            return;
        }
        mCallbackNewEntry = populateNewEntry();

        // Open a progress dialog and save entry
        AfterActionNodeOnFinish onFinish = new AfterSave();
        EntryEditActivity act = EntryEditActivity.this;
        RunnableOnFinish task;
        if ( mIsNew ) {
            task = new AddEntryRunnable(act, database, mCallbackNewEntry, onFinish);
        } else {
            task = new UpdateEntryRunnable(act, database, mEntry, mCallbackNewEntry, onFinish);
        }
        task.setUpdateProgressTaskStatus(
                new UpdateProgressTaskStatus(this,
                        SaveDatabaseProgressTaskDialogFragment.start(
                                getSupportFragmentManager())
                ));
        new Thread(task).start();
    }

    /**
     * Check and display learning views
     * Displays the explanation for the icon selection, the password generator and for a new field
     */
    private void checkAndPerformedEducation() {
        if (PreferencesUtil.isEducationScreensEnabled(this)) {
            // TODO Show icon

            if (!PreferencesUtil.isEducationPasswordGeneratorPerformed(this)) {
                TapTargetView.showFor(this,
                        TapTarget.forView(generatePasswordView,
                                getString(R.string.education_generate_password_title),
                                getString(R.string.education_generate_password_summary))
                                .textColorInt(Color.WHITE)
                                .tintTarget(false)
                                .cancelable(true),
                        new TapTargetView.Listener() {
                            @Override
                            public void onTargetClick(TapTargetView view) {
                                super.onTargetClick(view);
                                openPasswordGenerator();
                            }

                            @Override
                            public void onOuterCircleClick(TapTargetView view) {
                                super.onOuterCircleClick(view);
                                view.dismiss(false);
                            }
                        });
                PreferencesUtil.saveEducationPreference(this,
                        R.string.education_password_generator_key);
            } else if (mEntry.allowExtraFields()
                    && !mEntry.containsCustomFields()
                    && !PreferencesUtil.isEducationEntryNewFieldPerformed(this)) {
                TapTargetView.showFor(this,
                        TapTarget.forView(addNewFieldView,
                                getString(R.string.education_entry_new_field_title),
                                getString(R.string.education_entry_new_field_summary))
                                .textColorInt(Color.WHITE)
                                .tintTarget(false)
                                .cancelable(true),
                        new TapTargetView.Listener() {
                            @Override
                            public void onTargetClick(TapTargetView view) {
                                super.onTargetClick(view);
                                addNewCustomField();
                            }

                            @Override
                            public void onOuterCircleClick(TapTargetView view) {
                                super.onOuterCircleClick(view);
                                view.dismiss(false);
                            }
                        });
                PreferencesUtil.saveEducationPreference(this,
                        R.string.education_entry_new_field_key);
            }
        }
    }

    /**
     * Utility class to retrieve a validation or an error with a message
     */
    private class ErrorValidation {
        static final int unknownMessage = -1;

        boolean isValidate = false;
        int messageId = unknownMessage;

        void showValidationErrorIfNeeded() {
            if (!isValidate && messageId != unknownMessage)
                Toast.makeText(EntryEditActivity.this, messageId, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Validate or not the entry form
     *
     * @return ErrorValidation An error with a message or a validation without message
     */
    protected ErrorValidation validate() {
        ErrorValidation errorValidation = new ErrorValidation();

        // Require title
        String title = entryTitleView.getText().toString();
        if ( title.length() == 0 ) {
            errorValidation.messageId = R.string.error_title_required;
            return errorValidation;
        }

        // Validate password
        String pass = entryPasswordView.getText().toString();
        String conf = entryConfirmationPasswordView.getText().toString();
        if ( ! pass.equals(conf) ) {
            errorValidation.messageId = R.string.error_pass_match;
            return errorValidation;
        }

        // Validate extra fields
        if (mEntry.allowExtraFields()) {
            for (int i = 0; i < entryExtraFieldsContainer.getChildCount(); i++) {
                EntryEditCustomField entryEditCustomField = (EntryEditCustomField) entryExtraFieldsContainer.getChildAt(i);
                String key = entryEditCustomField.getLabel();
                if (key == null || key.length() == 0) {
                    errorValidation.messageId = R.string.error_string_key;
                    return errorValidation;
                }
            }
        }

        errorValidation.isValidate = true;
        return errorValidation;
    }

    /**
     * Launch a validation with {@link #validate()} and show the error if present
     *
     * @return true if the form was validate or false if not
     */
	protected boolean validateBeforeSaving() {
        ErrorValidation errorValidation = validate();
        errorValidation.showValidationErrorIfNeeded();
        return errorValidation.isValidate;
	}
	
	protected PwEntry populateNewEntry() {
        PwDatabase db = App.getDB().getPwDatabase();

        PwEntry newEntry = mEntry.clone();

        newEntry.startToManageFieldReferences(db);

        newEntry.createBackup(db);

        newEntry.setLastAccessTime(new PwDate());
        newEntry.setLastModificationTime(new PwDate());

        newEntry.setTitle(entryTitleView.getText().toString());
        newEntry.setIconStandard(retrieveIcon());

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

        newEntry.stopToManageFieldReferences();

        return newEntry;
	}

    /**
     * Retrieve the icon by the selection, or the first icon in the list if the entry is new or the last one
     */
	private PwIconStandard retrieveIcon() {

        if (!mSelectedIconStandard.isUnknown())
            return mSelectedIconStandard;
        else {
            if (mIsNew) {
                return database.getPwDatabase().getIconFactory().getKeyIcon();
            }
            else {
                // Keep previous icon, if no new one was selected
                return mEntry.getIconStandard();
            }
        }
    }


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		MenuInflater inflater = getMenuInflater();
		MenuUtil.contributionMenuInflater(inflater, menu);
		
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch ( item.getItemId() ) {
			case R.id.menu_contribute:
				return MenuUtil.onContributionItemSelected(this);

			case android.R.id.home:
				finish();
		}
		
		return super.onOptionsItemSelected(item);
	}

	private void assignIconView() {
        database.getDrawFactory()
                .assignDatabaseIconTo(
                        this,
                        entryIconView,
                        mEntry.getIcon(),
                        iconColor);
    }

	protected void fillData() {

        assignIconView();

		// Don't start the field reference manager, we want to see the raw ref
        mEntry.stopToManageFieldReferences();

        entryTitleView.setText(mEntry.getTitle());
        entryUserNameView.setText(mEntry.getUsername());
        entryUrlView.setText(mEntry.getUrl());
        String password = mEntry.getPassword();
        entryPasswordView.setText(password);
        entryConfirmationPasswordView.setText(password);
        entryCommentView.setText(mEntry.getNotes());

        boolean visibilityFontActivated = PreferencesUtil.fieldFontIsInVisibility(this);
        if (visibilityFontActivated) {
            Util.applyFontVisibilityTo(this, entryUserNameView);
            Util.applyFontVisibilityTo(this, entryPasswordView);
            Util.applyFontVisibilityTo(this, entryConfirmationPasswordView);
            Util.applyFontVisibilityTo(this, entryCommentView);
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
        mSelectedIconStandard = bundle.getParcelable(KEY_ICON_STANDARD);
        mEntry.setIconStandard(mSelectedIconStandard);
        assignIconView();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (!mSelectedIconStandard.isUnknown()) {
            outState.putParcelable(KEY_ICON_STANDARD, mSelectedIconStandard);
            super.onSaveInstanceState(outState);
        }
    }

    @Override
    public void acceptPassword(Bundle bundle) {
        String generatedPassword = bundle.getString(GeneratePasswordDialogFragment.KEY_PASSWORD_ID);
        entryPasswordView.setText(generatedPassword);
        entryConfirmationPasswordView.setText(generatedPassword);

        checkAndPerformedEducation();
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
                bundle.putParcelable(ADD_OR_UPDATE_ENTRY_KEY, mCallbackNewEntry);
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

	private final class AfterSave extends AfterActionNodeOnFinish {

		@Override
        public void run(@Nullable PwNode oldNode, @Nullable PwNode newNode) {
		    runOnUiThread(() -> {
                if ( mSuccess ) {
                    finish();
                } else {
                    displayMessage(EntryEditActivity.this);
                }

                SaveDatabaseProgressTaskDialogFragment.stop(EntryEditActivity.this);
            });
		}
    }

}
