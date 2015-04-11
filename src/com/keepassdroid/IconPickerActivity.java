/*
 * Copyright 2010 Brian Pellin.
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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.keepass.R;
import com.keepassdroid.icons.Icons;

public class IconPickerActivity extends LockCloseActivity
{
	public static final String KEY_ICON_ID = "icon_id";

	public static void Launch(Activity act)
	{
		Intent i = new Intent(act, IconPickerActivity.class);
		act.startActivityForResult(i, 0);
	}

   @Override
   public void onCreate(Bundle savedInstanceState)
   {
   	super.onCreate(savedInstanceState);
   	setContentView(R.layout.icon_picker);

   	GridView currIconGridView = (GridView)findViewById(R.id.IconGridView);
   	currIconGridView.setAdapter(new ImageAdapter(this));

   	currIconGridView.setOnItemClickListener(new OnItemClickListener()
   	{
			public void onItemClick(AdapterView<?> parent, View v, int position, long id)
			{
				final Intent intent = new Intent();
				
				intent.putExtra(KEY_ICON_ID, position);
				setResult(EntryEditActivity.RESULT_OK_ICON_PICKER, intent);
				
				finish();
			}
   	});
   }
   
   public class ImageAdapter extends BaseAdapter
   {
   	Context mContext;

   	public ImageAdapter(Context c)
   	{
   		mContext = c;
   	}
   	
   	public int getCount()
   	{
   		/* Return number of KeePass icons */
   		return Icons.count();
   	}
   	
   	public Object getItem(int position)
		{
			return null;
		}

		public long getItemId(int position)
		{
			return 0;
		}
   	
   	public View getView(int position, View convertView, ViewGroup parent)
   	{
   		View currView;
   		if(convertView == null)
   		{
   			LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
   			currView = li.inflate(R.layout.icon, parent, false);
   		}
   		else
   		{
   			currView = convertView;
   		}
   		
   		TextView tv = (TextView) currView.findViewById(R.id.icon_text);
   		tv.setText("" + position);
   		ImageView iv = (ImageView) currView.findViewById(R.id.icon_image);
   		iv.setImageResource(Icons.iconToResId(position));
 
   		return currView;
   	}
   }
}
