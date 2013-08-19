/*
 * Copyright 2009-2013 Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.fileselect;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLDecoder;

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.keepass.R;
import com.keepassdroid.AboutDialog;
import com.keepassdroid.GroupActivity;
import com.keepassdroid.PasswordActivity;
import com.keepassdroid.ProgressTask;
import com.keepassdroid.SetPasswordDialog;
import com.keepassdroid.app.App;
import com.keepassdroid.database.edit.CreateDB;
import com.keepassdroid.database.edit.FileOnFinish;
import com.keepassdroid.intents.Intents;
import com.keepassdroid.settings.AppSettingsActivity;
import com.keepassdroid.utils.Interaction;
import com.keepassdroid.utils.Util;
import com.keepassdroid.view.FileNameView;

public class FileSelectActivity extends ListActivity {

	private static final int CMENU_CLEAR = Menu.FIRST;
	
	public static final int FILE_BROWSE = 1;
	public static final int GET_CONTENT = 2;
	
	private RecentFileHistory fileHistory;

	private boolean recentMode = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		fileHistory = App.getFileHistory();

		if (fileHistory.hasRecentFiles()) {
			recentMode = true;
			setContentView(R.layout.file_selection);
		} else {
			setContentView(R.layout.file_selection_no_recent);
		}

		// Open button
		Button openButton = (Button) findViewById(R.id.open);
		openButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				String fileName = Util.getEditText(FileSelectActivity.this,
						R.id.file_filename);

				try {
					PasswordActivity.Launch(FileSelectActivity.this, fileName);
				} catch (FileNotFoundException e) {
					Toast.makeText(FileSelectActivity.this,
							R.string.FileNotFound, Toast.LENGTH_LONG).show();
				}

			}
		});

		// Create button
		Button createButton = (Button) findViewById(R.id.create);
		createButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				String filename = Util.getEditText(FileSelectActivity.this,
						R.id.file_filename);

				// Make sure file name exists
				if (filename.length() == 0) {
					Toast
							.makeText(FileSelectActivity.this,
									R.string.error_filename_required,
									Toast.LENGTH_LONG).show();
					return;
				}

				// Try to create the file
				File file = new File(filename);
				try {
					if (file.exists()) {
						Toast.makeText(FileSelectActivity.this,
								R.string.error_database_exists,
								Toast.LENGTH_LONG).show();
						return;
					}
					File parent = file.getParentFile();
					
					if ( parent == null || (parent.exists() && ! parent.isDirectory()) ) {
						Toast.makeText(FileSelectActivity.this,
								R.string.error_invalid_path,
								Toast.LENGTH_LONG).show();
						return;
					}
					
					if ( ! parent.exists() ) {
						// Create parent dircetory
						if ( ! parent.mkdirs() ) {
							Toast.makeText(FileSelectActivity.this,
									R.string.error_could_not_create_parent,
									Toast.LENGTH_LONG).show();
							return;
							
						}
					}
					
					file.createNewFile();
				} catch (IOException e) {
					Toast.makeText(
							FileSelectActivity.this,
							getText(R.string.error_file_not_create) + " "
									+ e.getLocalizedMessage(),
							Toast.LENGTH_LONG).show();
					return;
				}

				// Prep an object to collect a password once the database has
				// been created
				CollectPassword password = new CollectPassword(
						new LaunchGroupActivity(filename));

				// Create the new database
				CreateDB create = new CreateDB(filename, password, true);
				ProgressTask createTask = new ProgressTask(
						FileSelectActivity.this, create,
						R.string.progress_create);
				createTask.run();

			}

		});
		
		ImageButton browseButton = (ImageButton) findViewById(R.id.browse_button);
		browseButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_GET_CONTENT);
				i.setType("file/*");
				
				try {
					startActivityForResult(i, GET_CONTENT);
				} catch (ActivityNotFoundException e) {
					lookForOpenIntentsFilePicker();
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

		fillData();
		
		registerForContextMenu(getListView());
		
		// Load default database
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String fileName = prefs.getString(PasswordActivity.KEY_DEFAULT_FILENAME, "");
		
		if (fileName.length() > 0) {
			File db = new File(fileName);
			
			if (db.exists()) {
				try {
					PasswordActivity.Launch(FileSelectActivity.this, fileName);
				} catch (Exception e) {
					// Ignore exception
				}
			}
		}
	}

	private class LaunchGroupActivity extends FileOnFinish {
		private String mFilename;

		public LaunchGroupActivity(String filename) {
			super(null);

			mFilename = filename;
		}

		@Override
		public void run() {
			if (mSuccess) {
				// Add to recent files
				fileHistory.createFile(mFilename, getFilename());

				GroupActivity.Launch(FileSelectActivity.this);

			} else {
				File file = new File(mFilename);
				file.delete();
			}
		}
	}

	private class CollectPassword extends FileOnFinish {

		public CollectPassword(FileOnFinish finish) {
			super(finish);
		}

		@Override
		public void run() {
			SetPasswordDialog password = new SetPasswordDialog(FileSelectActivity.this, mOnFinish);
			password.show();
		}

	}

	private void fillData() {
		// Set the initial value of the filename
		EditText filename = (EditText) findViewById(R.id.file_filename);
		filename.setText(Environment.getExternalStorageDirectory().getAbsolutePath() + getString(R.string.default_file_path));
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.file_row, R.id.file_filename, fileHistory.getDbList());
		setListAdapter(adapter);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		String fileName = fileHistory.getDatabaseAt(position);
		String keyFile = fileHistory.getKeyfileAt(position);

		try {
			PasswordActivity.Launch(this, fileName, keyFile);
		} catch (FileNotFoundException e) {
			Toast.makeText(this, R.string.FileNotFound, Toast.LENGTH_LONG)
					.show();
		}
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
		else if (requestCode == GET_CONTENT && resultCode == RESULT_OK) {
			if (data != null) {
				Uri uri = data.getData();
				if (uri != null) {
					filename = uri.getPath();
				}
			}
		}
		
		if (filename != null) {
			EditText fn = (EditText) findViewById(R.id.file_filename);
			fn.setText(filename);
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
		
		FileNameView fnv = (FileNameView) findViewById(R.id.file_select);
		fnv.updateExternalStorageWarning();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.fileselect, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_donate:
			try {
				Util.gotoUrl(this, R.string.donate_url);
			} catch (ActivityNotFoundException e) {
				Toast.makeText(this, R.string.error_failed_to_launch_link, Toast.LENGTH_LONG).show();
				return false;
			}
			
			return true;
			
		case R.id.menu_about:
			AboutDialog dialog = new AboutDialog(this);
			dialog.show();
			return true;
			
		case R.id.menu_app_settings:
			AppSettingsActivity.Launch(this);
			return true;
		}

		return super.onOptionsItemSelected(item);
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
			fileHistory.deleteFile(filename);
			
			refreshList();
			
			return true;
		}
		
		return false;
	}
	
	private void refreshList() {
		@SuppressWarnings("unchecked")
		ArrayAdapter<String> adapter = (ArrayAdapter<String>) getListAdapter();
		adapter.notifyDataSetChanged();
	}

}
