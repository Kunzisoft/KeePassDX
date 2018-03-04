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
package com.keepassdroid.fileselect;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
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

import com.keepassdroid.activities.GroupActivity;
import com.keepassdroid.app.App;
import com.keepassdroid.database.edit.CreateDB;
import com.keepassdroid.database.edit.FileOnFinish;
import com.keepassdroid.database.exception.ContentFileNotFoundException;
import com.keepassdroid.fragments.AssignMasterKeyDialogFragment;
import com.keepassdroid.fragments.CreateFileDialogFragment;
import com.keepassdroid.password.PasswordActivity;
import com.keepassdroid.stylish.StylishActivity;
import com.keepassdroid.tasks.ProgressTask;
import com.keepassdroid.utils.EmptyUtils;
import com.keepassdroid.utils.MenuUtil;
import com.keepassdroid.utils.UriUtil;
import com.keepassdroid.view.AssignPasswordHelper;
import com.keepassdroid.view.FileNameView;
import com.keepassdroid.view.KeyFileHelper;
import com.kunzisoft.keepass.R;

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
		CreateFileDialogFragment.DefinePathDialogListener ,
		AssignMasterKeyDialogFragment.AssignPasswordDialogListener,
		FileSelectAdapter.FileItemOpenListener,
        FileSelectAdapter.FileSelectClearListener,
        FileSelectAdapter.FileInformationShowListener {

    private static final String TAG = "FileSelectActivity";

    private FileSelectAdapter mAdapter;
	private View fileListTitle;

	private RecentFileHistory fileHistory;

	private boolean recentMode = false;

	private EditText openFileNameView;
	private FileNameView fileNameView;

	private AssignPasswordHelper assignPasswordHelper;
	private Uri databaseUri;

	private KeyFileHelper keyFileHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		fileHistory = App.getFileHistory();

		setContentView(R.layout.file_selection);
        fileListTitle = findViewById(R.id.file_list_title);
		if (fileHistory.hasRecentFiles()) {
			recentMode = true;
		}

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		toolbar.setTitle(getString(R.string.app_name));
		setSupportActionBar(toolbar);

        openFileNameView = (EditText) findViewById(R.id.file_filename);
        fileNameView = (FileNameView) findViewById(R.id.file_select);

        // Set the initial value of the filename
        String defaultPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                + getString(R.string.database_file_path_default)
                + getString(R.string.database_file_name_default)
                + getString(R.string.database_file_extension_default);
        openFileNameView.setText(defaultPath);

        RecyclerView mListFiles = (RecyclerView) findViewById(R.id.file_list);
		mListFiles.setLayoutManager(new LinearLayoutManager(this));

		// Open button
		View openButton = findViewById(R.id.open_database);
		openButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				String fileName = openFileNameView.getText().toString();
				try {
					PasswordActivity.launch(FileSelectActivity.this, fileName);
				}
				catch (ContentFileNotFoundException e) {
					Toast.makeText(FileSelectActivity.this,
							R.string.file_not_found_content, Toast.LENGTH_LONG).show();
				}
				catch (FileNotFoundException e) {
					Toast.makeText(FileSelectActivity.this,
							R.string.file_not_found, Toast.LENGTH_LONG).show();
				}
			}
		});

		// Create button
		View createButton = findViewById(R.id.create_database);
		createButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
                FileSelectActivityPermissionsDispatcher
                        .openCreateFileDialogFragmentWithPermissionCheck(FileSelectActivity.this);
			}
		});

        keyFileHelper = new KeyFileHelper(this);
		View browseButton = findViewById(R.id.browse_button);
		browseButton.setOnClickListener(keyFileHelper.getOpenFileOnClickViewListener(
		        new KeyFileHelper.ClickDataUriCallback() {
            @Override
            public Uri onRequestIntentFilePicker() {
                return Uri.parse("file://" + openFileNameView.getText().toString());
            }
        }));

		// Construct adapter with listeners
		mAdapter = new FileSelectAdapter(FileSelectActivity.this, fileHistory.getDbList());
		mAdapter.setOnItemClickListener(this);
		mAdapter.setFileSelectClearListener(this);
		mAdapter.setFileInformationShowListener(this);
		mListFiles.setAdapter(mAdapter);

		// Load default database
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String fileName = prefs.getString(PasswordActivity.KEY_DEFAULT_FILENAME, "");

		if (fileName.length() > 0) {
			Uri dbUri = UriUtil.parseDefaultFile(fileName);
            String scheme = null;
			if (dbUri!=null)
			    scheme = dbUri.getScheme();

			if (!EmptyUtils.isNullOrEmpty(scheme) && scheme.equalsIgnoreCase("file")) {
				String path = dbUri.getPath();
				File db = new File(path);

				if (db.exists()) {
					try {
						PasswordActivity.launch(FileSelectActivity.this, path);
					} catch (Exception e) {
						// Ignore exception
					}
				}
			}
			else {
				try {
					PasswordActivity.launch(FileSelectActivity.this, dbUri.toString());
				} catch (Exception e) {
					// Ignore exception
				}
			}
		}
	}

    @Override
    protected void onResume() {
        super.onResume();

        // Check to see if we need to change modes
        if ( fileHistory.hasRecentFiles() != recentMode ) {
            // Restart the activity
            Intent intent = getIntent();
            startActivity(intent);
            finish();
        }

        fileNameView.updateExternalStorageWarning();
        updateTitleFileListView();
        mAdapter.notifyDataSetChanged();
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
		new OpenFileHistoryAsyncTask(new OpenFileHistoryAsyncTask.AfterOpenFileHistoryListener() {
			@Override
			public void afterOpenFile(String fileName, String keyFile) {
				try {
					PasswordActivity.launch(FileSelectActivity.this,
							fileName, keyFile);
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
			}
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
        new DeleteFileHistoryAsyncTask(new DeleteFileHistoryAsyncTask.AfterDeleteFileHistoryListener() {
            @Override
            public void afterDeleteFile() {
                fileHistory.deleteFile(fileSelectBean.getFileUri());
                mAdapter.notifyDataSetChanged();
                updateTitleFileListView();
            }
        }, fileHistory, mAdapter).execute(fileSelectBean);
        return true;
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

        keyFileHelper.onActivityResultCallback(requestCode, resultCode, data,
                new KeyFileHelper.KeyFileCallback() {
            @Override
            public void onKeyFileResultCallback(Uri uri) {
                if (uri != null) {
                    String filename = uri.toString();
                    openFileNameView.setText(filename);
                }
            }
        });
	}

    @OnShowRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showRationaleForExternalStorage(final PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.permission_external_storage_rationale_write_database)
                .setPositiveButton(R.string.allow, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        request.proceed();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        request.cancel();
                    }
                })
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
