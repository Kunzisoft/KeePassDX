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
package com.keepassdroid.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.kunzisoft.keepass.R;
import com.keepassdroid.icons.Icons;
import com.keepassdroid.stylish.StylishActivity;


public class IconPickerDialogFragment extends DialogFragment {
	public static final String KEY_ICON_ID = "icon_id";
	private IconPickerListener iconPickerListener;

	public static void Launch(StylishActivity activity)	{
        // Create an instance of the dialog fragment and show it
        IconPickerDialogFragment dialog = new IconPickerDialogFragment();
        dialog.show(activity.getSupportFragmentManager(), "NoticeDialogFragment");
	}

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            iconPickerListener = (IconPickerListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement " + IconPickerListener.class.getName());
        }
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		// Get the layout inflater
		LayoutInflater inflater = getActivity().getLayoutInflater();

		// Inflate and set the layout for the dialog
		// Pass null as the parent view because its going in the dialog layout
		View root = inflater.inflate(R.layout.icon_picker, null);
		builder.setView(root);

		GridView currIconGridView = (GridView) root.findViewById(R.id.IconGridView);
		currIconGridView.setAdapter(new ImageAdapter(this.getContext()));

		currIconGridView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				Bundle bundle = new Bundle();
                bundle.putInt(KEY_ICON_ID, position);
                iconPickerListener.iconPicked(bundle);

				dismiss();
			}
		});

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                IconPickerDialogFragment.this.getDialog().cancel();
            }
        });

		return builder.create();
	}
   
	public class ImageAdapter extends BaseAdapter {
		private Context context;

		public ImageAdapter(Context c)	{
			context = c;
		}

		public int getCount() 	{
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
   	
		public View getView(int position, View convertView, ViewGroup parent) {
			View currView;
			if(convertView == null) {
				LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				currView = li.inflate(R.layout.icon, parent, false);
			}
			else {
				currView = convertView;
			}

			TextView tv = (TextView) currView.findViewById(R.id.icon_text);
			tv.setText("" + position);
			ImageView iv = (ImageView) currView.findViewById(R.id.icon_image);
			iv.setImageResource(Icons.iconToResId(position));

			return currView;
		}
   }

   public interface IconPickerListener {
	    void iconPicked(Bundle bundle);
   }
}
