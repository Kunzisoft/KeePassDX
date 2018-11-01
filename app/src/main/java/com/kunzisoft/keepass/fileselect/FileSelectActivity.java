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
package com.kunzisoft.keepass.fileselect;

import android.Manifest;
import android.app.Activity;
import android.app.assist.AssistStructure;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.activities.GroupActivity;
import com.kunzisoft.keepass.app.App;
import com.kunzisoft.keepass.autofill.AutofillHelper;
import com.kunzisoft.keepass.database.action.CreateDatabaseRunnable;
import com.kunzisoft.keepass.database.action.FileOnFinishRunnable;
import com.kunzisoft.keepass.database.exception.ContentFileNotFoundException;
import com.kunzisoft.keepass.dialogs.AssignMasterKeyDialogFragment;
import com.kunzisoft.keepass.dialogs.CreateFileDialogFragment;
import com.kunzisoft.keepass.password.AssignPasswordHelper;
import com.kunzisoft.keepass.password.PasswordActivity;
import com.kunzisoft.keepass.selection.EntrySelectionHelper;
import com.kunzisoft.keepass.settings.PreferencesUtil;
import com.kunzisoft.keepass.stylish.StylishActivity;
import com.kunzisoft.keepass.tasks.ProgressTaskDialogFragment;
import com.kunzisoft.keepass.tasks.UpdateProgressTaskStatus;
import com.kunzisoft.keepass.utils.EmptyUtils;
import com.kunzisoft.keepass.utils.MenuUtil;
import com.kunzisoft.keepass.utils.UriUtil;

import net.cachapa.expandablelayout.ExpandableLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLDecoder;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class FileSelectActivity extends StylishActivity implements
        CreateFileDialogFragment.DefinePathDialogListener,
        AssignMasterKeyDialogFragment.AssignPasswordDialogListener,
		FileSelectAdapter.FileItemOpenListener,
        FileSelectAdapter.FileSelectClearListener,
        FileSelectAdapter.FileInformationShowListener {

    private static final String TAG = "FileSelectActivity";

    private static final String EXTRA_STAY = "EXTRA_STAY";

    private FileSelectAdapter mAdapter;
	private View fileListContainer;
	private View createButtonView;
	private View browseButtonView;
	private View openButtonView;

	private RecentFileHistory fileHistory;

	// TODO Consultation Mode
	private boolean consultationMode = false;

	private boolean entrySelectionMode;
    private AutofillHelper autofillHelper;

    private View fileSelectExpandableButton;
    private ExpandableLayout fileSelectExpandable;
	private EditText openFileNameView;

	private AssignPasswordHelper assignPasswordHelper;
	private Uri databaseUri;

	private KeyFileHelper keyFileHelper;

    private String defaultPath;

	public static void launch(Activity activity) {
		Intent intent = new Intent(activity, FileSelectActivity.class);
		// only to avoid visible flickering when redirecting
		activity.startActivityForResult(intent, RESULT_CANCELED);
	}

    public static void launchForKeyboardResult(Activity activity) {
        Intent intent = new Intent(activity, FileSelectActivity.class);
        EntrySelectionHelper.addEntrySelectionModeExtraInIntent(intent);
        activity.startActivityForResult(intent, EntrySelectionHelper.ENTRY_SELECTION_RESPONSE_REQUEST_CODE);
    }

	@RequiresApi(api = Build.VERSION_CODES.O)
	public static void launchForAutofillResult(Activity activity, AssistStructure assistStructure) {
		if ( assistStructure != null ) {
			Intent intent = new Intent(activity, FileSelectActivity.class);
			AutofillHelper.addAssistStructureExtraInIntent(intent, assistStructure);
            activity.startActivityForResult(intent, AutofillHelper.AUTOFILL_RESPONSE_REQUEST_CODE);
        } else {
            launch(activity);
        }
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		fileHistory = App.getFileHistory();

        setContentView(R.layout.file_selection);
        fileListContainer = findViewById(R.id.container_file_list);

		Toolbar toolbar = findViewById(R.id.toolbar);
		toolbar.setTitle("");
		setSupportActionBar(toolbar);

        openFileNameView = findViewById(R.id.file_filename);

        // Set the initial value of the filename
        defaultPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                + getString(R.string.database_file_path_default)
                + getString(R.string.database_file_name_default)
                + getString(R.string.database_file_extension_default);
        openFileNameView.setHint(R.string.open_link_database);

        // Button to expand file selection
        fileSelectExpandableButton = findViewById(R.id.file_select_expandable_button);
        fileSelectExpandable = findViewById(R.id.file_select_expandable);
        fileSelectExpandableButton.setOnClickListener(view -> {
            if (fileSelectExpandable.isExpanded())
                fileSelectExpandable.collapse();
            else
                fileSelectExpandable.expand();
        });

        // History list
        RecyclerView mListFiles = findViewById(R.id.file_list);
		mListFiles.setLayoutManager(new LinearLayoutManager(this));

        entrySelectionMode = EntrySelectionHelper.isIntentInEntrySelectionMode(getIntent());
		// To retrieve info for AutoFill
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            autofillHelper = new AutofillHelper();
            autofillHelper.retrieveAssistStructure(getIntent());
        }

		// Open button
		openButtonView = findViewById(R.id.open_database);
        openButtonView.setOnClickListener(v -> {
		    String fileName = openFileNameView.getText().toString();
            if (fileName.isEmpty())
                fileName = defaultPath;
            launchPasswordActivityWithPath(fileName);
        });

		// Create button
		createButtonView = findViewById(R.id.create_database);
        createButtonView .setOnClickListener(v ->
                FileSelectActivityPermissionsDispatcher
                        .openCreateFileDialogFragmentWithPermissionCheck(FileSelectActivity.this)
		);

        keyFileHelper = new KeyFileHelper(this);
		browseButtonView = findViewById(R.id.browse_button);
        browseButtonView.setOnClickListener(keyFileHelper.getOpenFileOnClickViewListener(
                () -> Uri.parse("file://" + openFileNameView.getText().toString())));

		// Construct adapter with listeners
		mAdapter = new FileSelectAdapter(FileSelectActivity.this, fileHistory.getDbList());
		mAdapter.setOnItemClickListener(this);
		mAdapter.setFileSelectClearListener(this);
		mAdapter.setFileInformationShowListener(this);
		mListFiles.setAdapter(mAdapter);

		// Load default database if not an orientation change
        if (! (savedInstanceState != null
                && savedInstanceState.containsKey(EXTRA_STAY)
                && savedInstanceState.getBoolean(EXTRA_STAY, false)) ) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String fileName = prefs.getString(PasswordActivity.KEY_DEFAULT_FILENAME, "");

            if (fileName.length() > 0) {
                Uri dbUri = UriUtil.parseDefaultFile(fileName);
                String scheme = null;
                if (dbUri != null)
                    scheme = dbUri.getScheme();

                if (!EmptyUtils.isNullOrEmpty(scheme) && scheme.equalsIgnoreCase("file")) {
                    String path = dbUri.getPath();
                    File db = new File(path);

                    if (db.exists()) {
                        launchPasswordActivityWithPath(path);
                    }
                } else {
                    if (dbUri != null)
                        launchPasswordActivityWithPath(dbUri.toString());
                }
            }
        }

        // For the first time show education
        checkAndPerformedEducation();
	}

	private void launchPasswordActivityWithPath(String path) {
        try {
            AssistStructure assistStructure = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assistStructure = autofillHelper.getAssistStructure();
                if (assistStructure != null) {
					PasswordActivity.launchForAutofillResult(FileSelectActivity.this,
                            path,
                            assistStructure);
				}
			}
			if (assistStructure == null) {
                if (entrySelectionMode) {
                    PasswordActivity.launchForKeyboardResult(FileSelectActivity.this, path);
                } else {
                    PasswordActivity.launch(FileSelectActivity.this, path);
                }
            }
            // Delete flickering for kitkat <=
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                overridePendingTransition(0, 0);
        } catch (ContentFileNotFoundException e) {
            String error = getString(R.string.file_not_found_content);
            Toast.makeText(FileSelectActivity.this,
                    error, Toast.LENGTH_LONG).show();
            Log.e(TAG, error, e);
        } catch (FileNotFoundException e) {
            String error = getString(R.string.file_not_found);
            Toast.makeText(FileSelectActivity.this,
                    error, Toast.LENGTH_LONG).show();
            Log.e(TAG, error, e);
        } catch (Exception e) {
            Log.e(TAG, "Can't launch PasswordActivity", e);
        }
    }

    private void updateExternalStorageWarning() {
        // To show errors
        int warning = -1;
        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
            warning = R.string.read_only_warning;
        } else if (!state.equals(Environment.MEDIA_MOUNTED)) {
            warning = R.string.warning_unmounted;
        }

        TextView labelWarningView = findViewById(R.id.label_warning);
        if (warning != -1) {
            labelWarningView.setText(warning);
            labelWarningView.setVisibility(View.VISIBLE);
        } else {
            labelWarningView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateExternalStorageWarning();
        updateFileListVisibility();
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Check and display learning views
     * Displays the explanation for a database creation then a database selection
     */
    private void checkAndPerformedEducation() {

        // If no recent files
        if ( !fileHistory.hasRecentFiles() ) {
            // Try to open the creation base education
            if (!PreferencesUtil.isEducationCreateDatabasePerformed(this) ) {

                TapTargetView.showFor(this,
                        TapTarget.forView(createButtonView,
                                getString(R.string.education_create_database_title),
                                getString(R.string.education_create_database_summary))
                                .icon(ContextCompat.getDrawable(this, R.drawable.ic_database_plus_white_24dp))
                                .textColorInt(Color.WHITE)
                                .tintTarget(true)
                                .cancelable(true),
                        new TapTargetView.Listener() {
                            @Override
                            public void onTargetClick(TapTargetView view) {
                                super.onTargetClick(view);
                                FileSelectActivityPermissionsDispatcher
                                        .openCreateFileDialogFragmentWithPermissionCheck(FileSelectActivity.this);
                            }

                            @Override
                            public void onOuterCircleClick(TapTargetView view) {
                                super.onOuterCircleClick(view);
                                view.dismiss(false);
                                // But if the user cancel, it can also select a database
                                checkAndPerformedEducationForSelection();
                            }
                        });
                PreferencesUtil.saveEducationPreference(FileSelectActivity.this,
                        R.string.education_create_db_key);
            }
        }
        else
            checkAndPerformedEducationForSelection();
    }

    /**
     * Check and display learning views
     * Displays the explanation for a database selection
     */
    private void checkAndPerformedEducationForSelection() {
        if (PreferencesUtil.isEducationScreensEnabled(this)) {

            if (!PreferencesUtil.isEducationSelectDatabasePerformed(this)
                    && browseButtonView != null) {

                TapTargetView.showFor(FileSelectActivity.this,
                        TapTarget.forView(browseButtonView,
                                getString(R.string.education_select_database_title),
                                getString(R.string.education_select_database_summary))
                                .icon(ContextCompat.getDrawable(this, R.drawable.ic_folder_white_24dp))
                                .textColorInt(Color.WHITE)
                                .tintTarget(true)
                                .cancelable(true),
                        new TapTargetView.Listener() {
                            @Override
                            public void onTargetClick(TapTargetView view) {
                                super.onTargetClick(view);
                                keyFileHelper.getOpenFileOnClickViewListener().onClick(view);
                            }

                            @Override
                            public void onOuterCircleClick(TapTargetView view) {
                                super.onOuterCircleClick(view);
                                view.dismiss(false);

                                if (!PreferencesUtil.isEducationOpenLinkDatabasePerformed(FileSelectActivity.this)) {

                                    TapTargetView.showFor(FileSelectActivity.this,
                                            TapTarget.forView(fileSelectExpandableButton,
                                                    getString(R.string.education_open_link_database_title),
                                                    getString(R.string.education_open_link_database_summary))
                                                    .icon(ContextCompat.getDrawable(FileSelectActivity.this, R.drawable.ic_link_white_24dp))
                                                    .textColorInt(Color.WHITE)
                                                    .tintTarget(true)
                                                    .cancelable(true),
                                            new TapTargetView.Listener() {
                                                @Override
                                                public void onTargetClick(TapTargetView view) {
                                                    super.onTargetClick(view);
                                                    // Do nothing here
                                                }

                                                @Override
                                                public void onOuterCircleClick(TapTargetView view) {
                                                    super.onOuterCircleClick(view);
                                                    view.dismiss(false);
                                                }
                                            });
                                    PreferencesUtil.saveEducationPreference(FileSelectActivity.this,
                                            R.string.education_open_link_db_key);
                                }
                            }
                        });
                PreferencesUtil.saveEducationPreference(FileSelectActivity.this,
                        R.string.education_select_db_key);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // only to keep the current activity
        outState.putBoolean(EXTRA_STAY, true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        FileSelectActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public void openCreateFileDialogFragment() {
        CreateFileDialogFragment createFileDialogFragment = new CreateFileDialogFragment();
        createFileDialogFragment.show(getSupportFragmentManager(), "createFileDialogFragment");
    }

	private void updateFileListVisibility() {
	    if(mAdapter.getItemCount() == 0)
            fileListContainer.setVisibility(View.INVISIBLE);
	    else
            fileListContainer.setVisibility(View.VISIBLE);
    }

    /**
     * Create file for database
     * @return If not created, return false
     */
	private boolean createDatabaseFile(Uri path) {

	    String pathString = URLDecoder.decode(path.getPath());
		// Make sure file name exists
		if (pathString.length() == 0) {
		    Log.e(TAG, getString(R.string.error_filename_required));
			Toast.makeText(FileSelectActivity.this,
					R.string.error_filename_required,
					Toast.LENGTH_LONG).show();
			return false;
		}

		// Try to create the file
		File file = new File(pathString);
		try {
			if (file.exists()) {
                Log.e(TAG, getString(R.string.error_database_exists) + " " + file);
				Toast.makeText(FileSelectActivity.this,
						R.string.error_database_exists,
						Toast.LENGTH_LONG).show();
				return false;
			}
			File parent = file.getParentFile();

			if ( parent == null || (parent.exists() && ! parent.isDirectory()) ) {
                Log.e(TAG, getString(R.string.error_invalid_path) + " " + file);
				Toast.makeText(FileSelectActivity.this,
						R.string.error_invalid_path,
						Toast.LENGTH_LONG).show();
				return false;
			}

			if ( ! parent.exists() ) {
				// Create parent directory
				if ( ! parent.mkdirs() ) {
                    Log.e(TAG, getString(R.string.error_could_not_create_parent) + " " + parent);
					Toast.makeText(FileSelectActivity.this,
							R.string.error_could_not_create_parent,
							Toast.LENGTH_LONG).show();
					return false;
				}
			}

			return file.createNewFile();
		} catch (IOException e) {
            Log.e(TAG, getString(R.string.error_could_not_create_parent) + " " + e.getLocalizedMessage());
            e.printStackTrace();
			Toast.makeText(
					FileSelectActivity.this,
					getText(R.string.error_file_not_create) + " "
							+ e.getLocalizedMessage(),
					Toast.LENGTH_LONG).show();
			return false;
		}
	}

	@Override
	public boolean onDefinePathDialogPositiveClick(Uri pathFile) {
        databaseUri = pathFile;
        if(createDatabaseFile(pathFile)) {
            AssignMasterKeyDialogFragment assignMasterKeyDialogFragment = new AssignMasterKeyDialogFragment();
            assignMasterKeyDialogFragment.show(getSupportFragmentManager(), "passwordDialog");
            return true;
        } else
            return false;
	}

	@Override
	public boolean onDefinePathDialogNegativeClick(Uri pathFile) {
        return true;
	}

	@Override
	public void onAssignKeyDialogPositiveClick(
			boolean masterPasswordChecked, String masterPassword,
			boolean keyFileChecked, Uri keyFile) {

		try {
			String databaseFilename = databaseUri.getPath();

			// Prep an object to collect a password once the database has
			// been created
			FileOnFinishRunnable launchActivityOnFinish = new FileOnFinishRunnable(
					new LaunchGroupActivity(databaseFilename));
			AssignPasswordOnFinish assignPasswordOnFinish =
					new AssignPasswordOnFinish(launchActivityOnFinish);

            // Initialize the password helper assigner to set the password after the database creation
            assignPasswordHelper = new AssignPasswordHelper(this,
                    masterPasswordChecked, masterPassword, keyFileChecked, keyFile);
            assignPasswordHelper.setCreateProgressDialog(false);

			// Create the new database
			CreateDatabaseRunnable createDBTask = new CreateDatabaseRunnable(FileSelectActivity.this,
					databaseFilename, assignPasswordOnFinish, true);
            createDBTask.setUpdateProgressTaskStatus(
                    new UpdateProgressTaskStatus(this,
                            ProgressTaskDialogFragment.start(
                                    getSupportFragmentManager(),
                                    R.string.progress_create)
                    ));
            new Thread(createDBTask).start();

		} catch (Exception e) {
			String error = "Unable to create database with this password and key file";
			Toast.makeText(this, error, Toast.LENGTH_LONG).show();
			Log.e(TAG, error + " " + e.getMessage());
		}
	}

	@Override
	public void onAssignKeyDialogNegativeClick(
			boolean masterPasswordChecked, String masterPassword,
			boolean keyFileChecked, Uri keyFile) {

	}

	private class AssignPasswordOnFinish extends FileOnFinishRunnable {

        AssignPasswordOnFinish(FileOnFinishRunnable fileOnFinish) {
            super(fileOnFinish);
        }

        @Override
        public void run() {
            if (mSuccess) {
                // Dont use ProgressTaskDialogFragment.stop(getSupportFragmentManager());
                // assignPasswordHelper do it
                runOnUiThread(() -> assignPasswordHelper.assignPasswordInDatabase(mOnFinish));
            }
        }
    }

	private class LaunchGroupActivity extends FileOnFinishRunnable {
		private Uri mUri;

		LaunchGroupActivity(String filename) {
			super(null);
			mUri = UriUtil.parseDefaultFile(filename);
		}

		@Override
		public void run() {
			if (mSuccess) {
			    runOnUiThread(() -> {
                    // Add to recent files
                    fileHistory.createFile(mUri, getFilename());
                    mAdapter.notifyDataSetChanged();
                    updateFileListVisibility();
                    GroupActivity.launch(FileSelectActivity.this);
                });
			}
		}
	}

	@Override
	public void onFileItemOpenListener(int itemPosition) {
		new OpenFileHistoryAsyncTask((fileName, keyFile) -> {
            // TODO ENCAPSULATE
            try {
                AssistStructure assistStructure = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    assistStructure = autofillHelper.getAssistStructure();
                    if (assistStructure != null) {
                        PasswordActivity.launchForAutofillResult(FileSelectActivity.this,
                            fileName, keyFile, assistStructure);
                    }
                }
                if (assistStructure == null) {
                    if (entrySelectionMode) {
                        PasswordActivity.launchForKeyboardResult(FileSelectActivity.this, fileName, keyFile);
                    } else {
                        PasswordActivity.launch(FileSelectActivity.this, fileName, keyFile);
                    }
                }
            } catch (ContentFileNotFoundException e) {
                Toast.makeText(FileSelectActivity.this,
                        R.string.file_not_found_content, Toast.LENGTH_LONG)
                        .show();
            } catch (FileNotFoundException e) {
                Toast.makeText(FileSelectActivity.this,
                        R.string.file_not_found, Toast.LENGTH_LONG)
                        .show();
            }
            updateFileListVisibility();
        }, fileHistory).execute(itemPosition);
	}

    @Override
    public void onClickFileInformation(FileSelectBean fileSelectBean) {
	    if (fileSelectBean != null) {
            FileInformationDialogFragment fileInformationDialogFragment =
                    FileInformationDialogFragment.newInstance(fileSelectBean);
            fileInformationDialogFragment.show(getSupportFragmentManager(), "fileInformation");
        }
    }

    @Override
    public boolean onFileSelectClearListener(final FileSelectBean fileSelectBean) {
        new DeleteFileHistoryAsyncTask(() -> {
            fileHistory.deleteFile(fileSelectBean.getFileUri());
            mAdapter.notifyDataSetChanged();
            updateFileListVisibility();
        }, fileHistory, mAdapter).execute(fileSelectBean);
        return true;
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		// Get the entry result in entry selection mode
        EntrySelectionHelper.onActivityResultSetResultAndFinish(this, requestCode, resultCode, data);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			AutofillHelper.onActivityResultSetResultAndFinish(this, requestCode, resultCode, data);
		}

		keyFileHelper.onActivityResultCallback(requestCode, resultCode, data,
                uri -> {
                    if (uri != null) {
                        if (PreferencesUtil.autoOpenSelectedFile(FileSelectActivity.this)) {
                            launchPasswordActivityWithPath(uri.toString());
                        } else {
                            fileSelectExpandable.expand(false);
                            openFileNameView.setText(uri.toString());
                        }
                    }
                });
	}

    @OnShowRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showRationaleForExternalStorage(final PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.permission_external_storage_rationale_write_database)
                .setPositiveButton(R.string.allow, (dialog, which) -> request.proceed())
                .setNegativeButton(R.string.cancel, (dialog, which) -> request.cancel())
                .show();
    }

    @OnPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showDeniedForExternalStorage() {
        Toast.makeText(this, R.string.permission_external_storage_denied, Toast.LENGTH_SHORT).show();
    }

    @OnNeverAskAgain(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showNeverAskForExternalStorage() {
        Toast.makeText(this, R.string.permission_external_storage_never_ask, Toast.LENGTH_SHORT).show();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuUtil.defaultMenuInflater(getMenuInflater(), menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return MenuUtil.onDefaultMenuOptionsItemSelected(this, item)
				&& super.onOptionsItemSelected(item);
	}
}
