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

import java.io.FileNotFoundException;

import android.app.Activity;
import android.app.ListActivity;
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
import com.keepassdroid.PasswordActivity;
import com.keepassdroid.Util;
import com.keepassdroid.intents.TimeoutIntents;

public class FileSelectActivity extends ListActivity {

	private static final int MENU_ABOUT = Menu.FIRST;
	private FileDbHelper mDbHelper;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.file_selection);
		
		Button openButton = (Button) findViewById(R.id.file_button);
		openButton.setOnClickListener(new ClickHandler(this));
		
		mDbHelper = new FileDbHelper(this);
		mDbHelper.open();
		
		fillData();
		
	}
	
	private void fillData() {
        // Get all of the rows from the database and create the item list
        Cursor filesCursor = mDbHelper.fetchAllFiles();
        startManagingCursor(filesCursor);
        
        // Create an array to specify the fields we want to display in the list (only TITLE)
        String[] from = new String[]{FileDbHelper.KEY_FILE_FILENAME};
        
        // and an array of the fields we want to bind those fields to (in this case just text1)
        int[] to = new int[]{R.id.file_filename};
        
        // Now create a simple cursor adapter and set it to display
        SimpleCursorAdapter notes = 
        	    new SimpleCursorAdapter(this, R.layout.file_row, filesCursor, from, to);
        setListAdapter(notes);
	}

	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
	
		Cursor cursor = mDbHelper.fetchFile(id);
		startManagingCursor(cursor);
		
		String fileName = cursor.getString(cursor.getColumnIndexOrThrow(FileDbHelper.KEY_FILE_FILENAME));
		String keyFile = cursor.getString(cursor.getColumnIndexOrThrow(FileDbHelper.KEY_FILE_KEYFILE));
		
		try {
			PasswordActivity.Launch(this, fileName, keyFile);
		} catch (FileNotFoundException e) {
			Toast.makeText(this, R.string.FileNotFound, Toast.LENGTH_LONG).show();
		}
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		fillData();
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		sendBroadcast(new Intent(TimeoutIntents.START));
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		sendBroadcast(new Intent(TimeoutIntents.CANCEL));
	}

	private class ClickHandler implements View.OnClickListener {
		Activity mAct;
		
		ClickHandler(Activity act) {
			mAct = act;
		}
		
		@Override
		public void onClick(View v) {
			String fileName = Util.getEditText(mAct, R.id.file_filename);
			
			try {
				PasswordActivity.Launch(mAct, fileName);
			} catch (FileNotFoundException e) {
				Toast.makeText(mAct, R.string.FileNotFound, Toast.LENGTH_LONG).show();
			}
			
		}
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(0, MENU_ABOUT, 0, R.string.menu_about);
		menu.findItem(MENU_ABOUT).setIcon(android.R.drawable.ic_menu_help);
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch ( item.getItemId() ) {
		case MENU_ABOUT:
			AboutDialog dialog = new AboutDialog(this);
			dialog.show();
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}

}
