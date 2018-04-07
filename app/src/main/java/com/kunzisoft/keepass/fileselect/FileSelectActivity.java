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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.activities.GroupActivity;
import com.kunzisoft.keepass.app.App;
import com.kunzisoft.keepass.autofill.AutofillHelper;
import com.kunzisoft.keepass.database.edit.CreateDB;
import com.kunzisoft.keepass.database.edit.FileOnFinish;
import com.kunzisoft.keepass.database.exception.ContentFileNotFoundException;
import com.kunzisoft.keepass.dialogs.AssignMasterKeyDialogFragment;
import com.kunzisoft.keepass.dialogs.CreateFileDialogFragment;
import com.kunzisoft.keepass.password.AssignPasswordHelper;
import com.kunzisoft.keepass.password.PasswordActivity;
import com.kunzisoft.keepass.settings.PreferencesUtil;
import com.kunzisoft.keepass.stylish.StylishActivity;
import com.kunzisoft.keepass.tasks.ProgressTask;
import com.kunzisoft.keepass.utils.EmptyUtils;
import com.kunzisoft.keepass.utils.MenuUtil;
import com.kunzisoft.keepass.utils.UriUtil;
import com.kunzisoft.keepass.view.FileNameView;

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
	private View fileListTitle;

	private RecentFileHistory fileHistory;

	// TODO Consultation Mode
	private boolean consultationMode = false;
    private AutofillHelper autofillHelper;

	private EditText openFileNameView;
	private FileNameView fileNameView;

	private AssignPasswordHelper assignPasswordHelper;
	private Uri databaseUri;

	private KeyFileHelper keyFileHelper;

    private String defaultPath;

	public static void launch(Activity activity) {
		Intent intent = new Intent(activity, FileSelectActivity.class);
		// only to avoid visible flickering when redirecting
		activity.startActivityForResult(intent, 0);
	}

	@RequiresApi(api = Build.VERSION_CODES.O)
	public static void launch(Activity activity, AssistStructure assistStructure) {
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

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			if (AutofillHelper.isIntentContainsExtraAssistStructureKey(getIntent()))
                consultationMode = true;
		}

		fileHistory = App.getFileHistory();

        setContentView(R.layout.file_selection);
        fileListTitle = findViewById(R.id.file_list_title);

		Toolbar toolbar = findViewById(R.id.toolbar);
		toolbar.setTitle(getString(R.string.app_name));
		setSupportActionBar(toolbar);

        openFileNameView = findViewById(R.id.file_filename);
        fileNameView = findViewById(R.id.file_select);

        // Set the initial value of the filename
        defaultPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                + getString(R.string.database_file_path_default)
                + getString(R.string.database_file_name_default)
                + getString(R.string.database_file_extension_default);
        openFileNameView.setHint(R.string.open_link_database);

        RecyclerView mListFiles = findViewById(R.id.file_list);
		mListFiles.setLayoutManager(new LinearLayoutManager(this));

		// To retrieve info for AutoFill
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            autofillHelper = new AutofillHelper();
            autofillHelper.retrieveAssistStructure(getIntent());
        }

		// Open button
		View openButton = findViewById(R.id.open_database);
		openButton.setOnClickListener(v -> {
		    String fileName = openFileNameView.getText().toString();
            if (fileName.isEmpty())
                fileName = defaultPath;
            launchPasswordActivityWithPath(fileName);
        });

		// Create button
		View createButton = findViewById(R.id.create_database);
		createButton.setOnClickListener(v ->
                FileSelectActivityPermissionsDispatcher
                        .openCreateFileDialogFragmentWithPermissionCheck(FileSelectActivity.this)
		);

        keyFileHelper = new KeyFileHelper(this);
		View browseButton = findViewById(R.id.browse_button);
		browseButton.setOnClickListener(keyFileHelper.getOpenFileOnClickViewListener(
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
	}

	private void launchPasswordActivityWithPath(String path) {
        try {
            AssistStructure assistStructure = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assistStructure = autofillHelper.getAssistStructure();
                if (assistStructure != null) {
					PasswordActivity.launch(FileSelectActivity.this,
                            path,
                            assistStructure);
				}
			}
			if (assistStructure == null) {
                PasswordActivity.launch(FileSelectActivity.this, path);
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

    @Override
    protected void onResume() {
        super.onResume();

        fileNameView.updateExternalStorageWarning();
        updateTitleFileListView();
        mAdapter.notifyDataSetChanged();
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

	private void updateTitleFileListView() {
	    if(mAdapter.getItemCount() == 0)
            fileListTitle.setVisibility(View.INVISIBLE);
	    else
            fileListTitle.setVisibility(View.VISIBLE);
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
			FileOnFinish launchActivityOnFinish = new FileOnFinish(
					new LaunchGroupActivity(databaseFilename));
			AssignPasswordOnFinish assignPasswordOnFinish =
					new AssignPasswordOnFinish(launchActivityOnFinish);

			// Create the new database
			CreateDB create = new CreateDB(FileSelectActivity.this,
					databaseFilename, assignPasswordOnFinish, true);

			ProgressTask createTask = new ProgressTask(
					FileSelectActivity.this, create,
					R.string.progress_create);
			createTask.run();
			assignPasswordHelper =
					new AssignPasswordHelper(this,
							masterPassword, keyFile);
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

	private class AssignPasswordOnFinish extends FileOnFinish {

        AssignPasswordOnFinish(FileOnFinish fileOnFinish) {
            super(fileOnFinish);
        }

        @Override
        public void run() {
            if (mSuccess) {
                assignPasswordHelper.assignPasswordInDatabase(mOnFinish);
            }
        }
    }

	private class LaunchGroupActivity extends FileOnFinish {
		private Uri mUri;

		LaunchGroupActivity(String filename) {
			super(null);
			mUri = UriUtil.parseDefaultFile(filename);
		}

		@Override
		public void run() {
			if (mSuccess) {
				// Add to recent files
				fileHistory.createFile(mUri, getFilename());
                mAdapter.notifyDataSetChanged();
                updateTitleFileListView();
				GroupActivity.launch(FileSelectActivity.this);
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
                        PasswordActivity.launch(FileSelectActivity.this,
                            fileName, keyFile, assistStructure);
                    }
                }
                if (assistStructure == null) {
                    PasswordActivity.launch(FileSelectActivity.this, fileName, keyFile);
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
            updateTitleFileListView();
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
            updateTitleFileListView();
        }, fileHistory, mAdapter).execute(fileSelectBean);
        return true;
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			AutofillHelper.onActivityResultSetResultAndFinish(this, requestCode, resultCode, data);
		}

		keyFileHelper.onActivityResultCallback(requestCode, resultCode, data,
                uri -> {
                    if (uri != null) {
                        if (PreferencesUtil.autoOpenSelectedFile(FileSelectActivity.this)) {
                            launchPasswordActivityWithPath(uri.toString());
                        } else {
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
