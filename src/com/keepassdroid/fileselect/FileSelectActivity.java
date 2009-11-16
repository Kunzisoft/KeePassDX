/*
 * Copyright 2009 Brian Pellin.
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

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.android.keepass.R;
import com.keepassdroid.AboutDialog;
import com.keepassdroid.GroupActivity;
import com.keepassdroid.PasswordActivity;
import com.keepassdroid.ProgressTask;
import com.keepassdroid.SetPasswordDialog;
import com.keepassdroid.Util;
import com.keepassdroid.database.CreateDB;
import com.keepassdroid.database.FileOnFinish;

public class FileSelectActivity extends ListActivity {

	private static final int MENU_DONATE = Menu.FIRST;
	private static final int MENU_ABOUT = Menu.FIRST + 1;
	private FileDbHelper mDbHelper;

	private boolean recentMode = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mDbHelper = new FileDbHelper(this);
		mDbHelper.open();

		if (mDbHelper.hasRecentFiles()) {
			recentMode = true;
			setContentView(R.layout.file_selection);
		} else {
			setContentView(R.layout.file_selection_no_recent);
		}

		// Open button
		Button openButton = (Button) findViewById(R.id.open);
		openButton.setOnClickListener(new View.OnClickListener() {

			@Override
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

			@Override
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
					
					if ( parent.exists() && ! parent.isDirectory() ) {
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

		fillData();

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
				FileDbHelper dbHelper = new FileDbHelper(
						FileSelectActivity.this);
				dbHelper.open();
				dbHelper.createFile(mFilename, getFilename());
				dbHelper.close();

				GroupActivity.Launch(FileSelectActivity.this, null,
						GroupActivity.ADD_GROUP_ONLY);

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
			SetPasswordDialog password = new SetPasswordDialog(
					FileSelectActivity.this, mOnFinish);
			password.show();
		}

	}

	private void fillData() {
		// Get all of the rows from the database and create the item list
		Cursor filesCursor = mDbHelper.fetchAllFiles();
		startManagingCursor(filesCursor);

		// Create an array to specify the fields we want to display in the list
		// (only TITLE)
		String[] from = new String[] { FileDbHelper.KEY_FILE_FILENAME };

		// and an array of the fields we want to bind those fields to (in this
		// case just text1)
		int[] to = new int[] { R.id.file_filename };

		// Now create a simple cursor adapter and set it to display
		SimpleCursorAdapter notes = new SimpleCursorAdapter(this,
				R.layout.file_row, filesCursor, from, to);
		setListAdapter(notes);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		Cursor cursor = mDbHelper.fetchFile(id);
		startManagingCursor(cursor);

		String fileName = cursor.getString(cursor
				.getColumnIndexOrThrow(FileDbHelper.KEY_FILE_FILENAME));
		String keyFile = cursor.getString(cursor
				.getColumnIndexOrThrow(FileDbHelper.KEY_FILE_KEYFILE));

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
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		// Check to see if we need to change modes
		if ( mDbHelper.hasRecentFiles() != recentMode ) {
			// Restart the activity
			Intent intent = getIntent();
			startActivity(intent);
			finish();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, MENU_DONATE, 0, R.string.menu_donate);
		menu.findItem(MENU_DONATE).setIcon(android.R.drawable.ic_menu_share);

		menu.add(0, MENU_ABOUT, 0, R.string.menu_about);
		menu.findItem(MENU_ABOUT).setIcon(android.R.drawable.ic_menu_help);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_DONATE:
			try {
				Util.gotoUrl(this, R.string.donate_url);
			} catch (ActivityNotFoundException e) {
				Toast.makeText(this, R.string.error_failed_to_launch_link, Toast.LENGTH_LONG).show();
				return false;
			}
			
			return true;
		case MENU_ABOUT:
			AboutDialog dialog = new AboutDialog(this);
			dialog.show();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

}
