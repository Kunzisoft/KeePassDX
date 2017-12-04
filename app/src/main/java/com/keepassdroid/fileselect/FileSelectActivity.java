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
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.keepassdroid.AssignPasswordDialog;
import com.keepassdroid.CreateFileDialog;
import com.keepassdroid.GroupActivity;
import com.keepassdroid.PasswordActivity;
import com.keepassdroid.ProgressTask;
import com.keepassdroid.app.App;
import com.keepassdroid.compat.ContentResolverCompat;
import com.keepassdroid.compat.StorageAF;
import com.keepassdroid.database.edit.CreateDB;
import com.keepassdroid.database.edit.FileOnFinish;
import com.keepassdroid.database.exception.ContentFileNotFoundException;
import com.keepassdroid.intents.Intents;
import com.keepassdroid.stylish.StylishActivity;
import com.keepassdroid.utils.EmptyUtils;
import com.keepassdroid.utils.Interaction;
import com.keepassdroid.utils.MenuUtil;
import com.keepassdroid.utils.UriUtil;
import com.keepassdroid.utils.Util;
import com.keepassdroid.view.AssignPasswordHelper;
import com.keepassdroid.view.FileNameView;
import com.kunzisoft.keepass.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URLDecoder;

public class FileSelectActivity extends StylishActivity implements
		CreateFileDialog.DefinePathDialogListener ,
		AssignPasswordDialog.AssignPasswordDialogListener {

    private static final String TAG = "FileSelectActivity";

	private static final int MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE = 111;
	private ListView mList;
	private BaseAdapter mAdapter;

	private static final int CMENU_CLEAR = Menu.FIRST;
	
	public static final int FILE_BROWSE = 1;
	public static final int GET_CONTENT = 2;
	public static final int OPEN_DOC = 3;

	private RecentFileHistory fileHistory;

	private boolean recentMode = false;

	private EditText openFileNameView;

	private AssignPasswordHelper assignPasswordHelper;
	private Uri databaseUri;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		fileHistory = App.getFileHistory();

		setContentView(R.layout.file_selection);
		if (fileHistory.hasRecentFiles()) {
			recentMode = true;
		} else {
		    findViewById(R.id.file_listtop).setVisibility(View.INVISIBLE);
        }

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		toolbar.setTitle(getString(R.string.app_name));
		setSupportActionBar(toolbar);

		mList = (ListView)findViewById(R.id.file_list);

		mList.setOnItemClickListener(
            new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                    onListItemClick((ListView)parent, v, position, id);
                }
            }
		);

		// Open button
		View openButton = findViewById(R.id.open_database);
		openButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				String fileName = Util.getEditText(FileSelectActivity.this,
						R.id.file_filename);
				try {
					PasswordActivity.Launch(FileSelectActivity.this, fileName);
				}
				catch (ContentFileNotFoundException e) {
					Toast.makeText(FileSelectActivity.this,
							R.string.file_not_found_content, Toast.LENGTH_LONG).show();
				}
				catch (FileNotFoundException e) {
					Toast.makeText(FileSelectActivity.this,
							R.string.FileNotFound, Toast.LENGTH_LONG).show();
				}
			}
		});

		// Create button
		View createButton = findViewById(R.id.create_database);
		createButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
                CreateFileDialog createFileDialog = new CreateFileDialog();
                createFileDialog.show(getSupportFragmentManager(), "createFileDialog");
			}
		});
		
		View browseButton = findViewById(R.id.browse_button);
		browseButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				if (StorageAF.useStorageFramework(FileSelectActivity.this)) {
					Intent i = new Intent(StorageAF.ACTION_OPEN_DOCUMENT);
					i.addCategory(Intent.CATEGORY_OPENABLE);
					i.setType("*/*");
					i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|
							Intent.FLAG_GRANT_WRITE_URI_PERMISSION|
							Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
					startActivityForResult(i, OPEN_DOC);
				}
				else {
					Intent i;
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
						i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
					} else {
						i = new Intent(Intent.ACTION_GET_CONTENT);
					}
					i.addCategory(Intent.CATEGORY_OPENABLE);
					i.setType("*/*");

					try {
						startActivityForResult(i, GET_CONTENT);
					} catch (ActivityNotFoundException e) {
						lookForOpenIntentsFilePicker();
					} catch (SecurityException e) {
						lookForOpenIntentsFilePicker();
					}
				}
			}
			
			private void lookForOpenIntentsFilePicker() {
				if (Interaction.isIntentAvailable(FileSelectActivity.this, Intents.OPEN_INTENTS_FILE_BROWSE)) {
					Intent i = new Intent(Intents.OPEN_INTENTS_FILE_BROWSE);
					i.setData(Uri.parse("file://" + Util.getEditText(FileSelectActivity.this, R.id.file_filename)));
					try {
						startActivityForResult(i, FILE_BROWSE);
					} catch (ActivityNotFoundException e) {
						showBrowserDialog();
					}
				} else {
					showBrowserDialog();
				}
			}
			
			private void showBrowserDialog() {
				BrowserDialog diag = new BrowserDialog(FileSelectActivity.this);
				diag.show();
			}
		});

        // Set the initial value of the filename
        openFileNameView = (EditText) findViewById(R.id.file_filename);
        String defaultPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                + getString(R.string.database_file_path_default)
                + getString(R.string.database_file_name_default)
                + getString(R.string.database_file_extension_default);
        openFileNameView.setText(defaultPath);

		fillData();
		
		registerForContextMenu(mList);
		
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
						PasswordActivity.Launch(FileSelectActivity.this, path);
					} catch (Exception e) {
						// Ignore exception
					}
				}
			}
			else {
				try {
					PasswordActivity.Launch(FileSelectActivity.this, dbUri.toString());
				} catch (Exception e) {
					// Ignore exception
				}
			}
		}
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
	public void onDefinePathDialogPositiveClick(Uri pathFile) {
        databaseUri = pathFile;
        if(createDatabaseFile(pathFile)) {
            AssignPasswordDialog assignPasswordDialog = new AssignPasswordDialog();
            assignPasswordDialog.show(getSupportFragmentManager(), "passwordDialog");
        }
	}

	@Override
	public void onDefinePathDialogNegativeClick(Uri pathFile) {

	}

	@Override
	public void onAssignKeyDialogPositiveClick(String masterPassword, Uri keyFile) {
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
	public void onAssignKeyDialogNegativeClick(String masterPassword, Uri keyFile) {

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
				GroupActivity.Launch(FileSelectActivity.this);
			}
		}
	}

	private void fillData() {
        mAdapter = new ArrayAdapter<>(FileSelectActivity.this, R.layout.file_row, R.id.file_filename, fileHistory.getDbList());
        mList.setAdapter(mAdapter);
	}

	protected void onListItemClick(ListView l, View v, int position, long id) {
		new OpenFileHistoryAsyncTask(this, fileHistory).execute(position);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		fillData();
		
		String filename = null;
		if (requestCode == FILE_BROWSE && resultCode == RESULT_OK) {
			filename = data.getDataString();
			if (filename != null) {
				if (filename.startsWith("file://")) {
					filename = filename.substring(7);
				}
				
				filename = URLDecoder.decode(filename);
			}
			
		}
		else if ((requestCode == GET_CONTENT || requestCode == OPEN_DOC) && resultCode == RESULT_OK) {
			if (data != null) {
				Uri uri = data.getData();
				if (uri != null) {
					if (StorageAF.useStorageFramework(this)) {
						try {
							// try to persist read and write permissions
							ContentResolver resolver = getContentResolver();
							ContentResolverCompat.takePersistableUriPermission(resolver, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
							ContentResolverCompat.takePersistableUriPermission(resolver, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
						} catch (Exception e) {
							// nop
						}
					}
					if (requestCode == GET_CONTENT) {
						uri = UriUtil.translate(this, uri);
					}
					filename = uri.toString();
				}
			}
		}

		if (filename != null) {
			openFileNameView.setText(filename);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		// check for storage permission
		checkStoragePermission();
		
		// Check to see if we need to change modes
		if ( fileHistory.hasRecentFiles() != recentMode ) {
			// Restart the activity
			Intent intent = getIntent();
			startActivity(intent);
			finish();
		}
		
		FileNameView fnv = (FileNameView) findViewById(R.id.file_select);
		fnv.updateExternalStorageWarning();
	}

	private void checkStoragePermission() {
		// Here, thisActivity is the current activity
		if (ContextCompat.checkSelfPermission(FileSelectActivity.this,
											  Manifest.permission.WRITE_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED) {

			// Should we show an explanation?
			//if (ActivityCompat.shouldShowRequestPermissionRationale(FileSelectActivity.this,
			//														Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

				// Show an explanation to the user *asynchronously* -- don't block
				// this thread waiting for the user's response! After the user
				// sees the explanation, try again to request the permission.

			//} else {

				// No explanation needed, we can request the permission.

				ActivityCompat.requestPermissions(FileSelectActivity.this,
												  new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
												  MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE);

				// MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
				// app-defined int constant. The callback method gets the
				// result of the request.
			//}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		switch (requestCode) {
			case MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {

					// permission was granted, yay! Do the
					// contacts-related task you need to do.

				} else {

					// permission denied, boo! Disable the
					// functionality that depends on this permission.
				}
				return;
			}

			// other 'case' lines to check for other
			// permissions this app might request
		}
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

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		menu.add(0, CMENU_CLEAR, 0, R.string.remove_from_filelist);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		super.onContextItemSelected(item);
		
		if ( item.getItemId() == CMENU_CLEAR ) {
			AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) item.getMenuInfo();
			
			TextView tv = (TextView) acmi.targetView;
			String filename = tv.getText().toString();
            new DeleteFileHistoryAsyncTask(fileHistory, mAdapter).execute(filename);
			return true;
		}
		
		return false;
	}

	private static class OpenFileHistoryAsyncTask extends  AsyncTask<Integer, Void, Void> {

	    private WeakReference<Activity> weakActivity;
        private RecentFileHistory fileHistory;
	    private String fileName;
        private String keyFile;

        OpenFileHistoryAsyncTask(Activity activity, RecentFileHistory fileHistory) {
            this.weakActivity = new WeakReference<>(activity);
            this.fileHistory = fileHistory;
        }

        protected Void doInBackground(Integer... args) {
            int position = args[0];
            fileName = fileHistory.getDatabaseAt(position);
            keyFile = fileHistory.getKeyfileAt(position);
            return null;
        }

        protected void onPostExecute(Void v) {
            try {
                PasswordActivity.Launch(weakActivity.get(), fileName, keyFile);
            }
            catch (ContentFileNotFoundException e) {
                Toast.makeText(weakActivity.get(), R.string.file_not_found_content, Toast.LENGTH_LONG)
                        .show();
            }
            catch (FileNotFoundException e) {
                Toast.makeText(weakActivity.get(), R.string.FileNotFound, Toast.LENGTH_LONG)
                        .show();
            }
        }
    }

	private static class DeleteFileHistoryAsyncTask extends AsyncTask<String, Void, Void> {

	    private RecentFileHistory fileHistory;
	    private BaseAdapter adapter;

	    DeleteFileHistoryAsyncTask(RecentFileHistory fileHistory, BaseAdapter adapter) {
	        this.fileHistory = fileHistory;
	        this.adapter = adapter;
        }

        protected java.lang.Void doInBackground(String... args) {
            fileHistory.deleteFile(Uri.parse(args[0]));
            return null;
        }

        protected void onPostExecute(Void v) {
            adapter.notifyDataSetChanged();
        }
    }

}
