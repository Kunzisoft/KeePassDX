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
package com.keepassdroid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.keepass.R;
import com.keepassdroid.icons.Icons;

public class GroupEditActivity extends Activity
{
	public static final String KEY_NAME = "name";
	public static final String KEY_ICON_ID = "icon_id";
	
	private int mSelectedIconID;
	
	public static void Launch(Activity act) {
		Intent i = new Intent(act, GroupEditActivity.class);
		act.startActivityForResult(i, 0);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.group_edit);
		setTitle(R.string.add_group_title);
		
		ImageButton iconButton = (ImageButton) findViewById(R.id.icon_button);
		iconButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				IconPickerActivity.Launch(GroupEditActivity.this);
			}
		});
		
		Button okButton = (Button) findViewById(R.id.ok);
		okButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				TextView nameField = (TextView) findViewById(R.id.group_name);
				String name = nameField.getText().toString();
				
				if ( name.length() > 0 )
				{
					final Intent intent = new Intent();
					
					intent.putExtra(KEY_NAME, name);
					intent.putExtra(KEY_ICON_ID, mSelectedIconID);
					setResult(Activity.RESULT_OK, intent);
					
					finish();
				} 
				else
				{
					Toast.makeText(GroupEditActivity.this, R.string.error_no_name, Toast.LENGTH_LONG).show();
				}
			}
		});

		Button cancel = (Button) findViewById(R.id.cancel);
		cancel.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				final Intent intent = new Intent();
				setResult(Activity.RESULT_CANCELED, intent);

				finish();
			}
		});
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch (resultCode)
		{
			case EntryEditActivity.RESULT_OK_ICON_PICKER:
				mSelectedIconID = data.getExtras().getInt(IconPickerActivity.KEY_ICON_ID);
				ImageButton currIconButton = (ImageButton) findViewById(R.id.icon_button);
				currIconButton.setImageResource(Icons.iconToResId(mSelectedIconID));
				break;

			case Activity.RESULT_CANCELED:
			default:
				break;
		}
	}
}
