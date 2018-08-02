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
package com.kunzisoft.keepass.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.ImageViewCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.database.PwIconStandard;
import com.kunzisoft.keepass.icons.IconPack;
import com.kunzisoft.keepass.icons.IconPackChooser;
import com.kunzisoft.keepass.stylish.StylishActivity;


public class IconPickerDialogFragment extends DialogFragment {

	public static final String KEY_ICON_STANDARD = "KEY_ICON_STANDARD";

	private IconPickerListener iconPickerListener;
	private IconPack iconPack;

	public static void launch(StylishActivity activity)	{
        // Create an instance of the dialog fragment and show it
        IconPickerDialogFragment dialog = new IconPickerDialogFragment();
        dialog.show(activity.getSupportFragmentManager(), "IconPickerDialogFragment");
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

		iconPack = IconPackChooser.getSelectedIconPack(getContext());

		// Inflate and set the layout for the dialog
		// Pass null as the parent view because its going in the dialog layout
		View root = inflater.inflate(R.layout.icon_picker, null);
		builder.setView(root);

		GridView currIconGridView = root.findViewById(R.id.IconGridView);
		currIconGridView.setAdapter(new ImageAdapter(this.getContext()));

		currIconGridView.setOnItemClickListener((parent, v, position, id) -> {
            Bundle bundle = new Bundle();
			bundle.putParcelable(KEY_ICON_STANDARD, new PwIconStandard(position));
			iconPickerListener.iconPicked(bundle);
            dismiss();
        });

        builder.setNegativeButton(R.string.cancel, (dialog, id) ->
				IconPickerDialogFragment.this.getDialog().cancel());

		return builder.create();
	}
   
	public class ImageAdapter extends BaseAdapter {
		private Context context;

		ImageAdapter(Context c)	{
			context = c;
		}

		public int getCount() {
			/* Return number of KeePass icons */
			return iconPack.numberOfIcons();
		}
   	
   		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position)	{
			return 0;
		}
   	
		public View getView(int position, View convertView, ViewGroup parent) {
			View currView;
			if(convertView == null) {
				LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                assert li != null;
                currView = li.inflate(R.layout.icon, parent, false);
			}
			else {
				currView = convertView;
			}
			ImageView iv = currView.findViewById(R.id.icon_image);
			iv.setImageResource(iconPack.iconToResId(position));

			// Assign color if icons are tintable
			if (iconPack.tintable()) {
				// Retrieve the textColor to tint the icon
				int[] attrs = {android.R.attr.textColor};
				assert getContext() != null;
				TypedArray ta = getContext().getTheme().obtainStyledAttributes(attrs);
				int iconColor = ta.getColor(0, Color.BLACK);
                ImageViewCompat.setImageTintList(iv, ColorStateList.valueOf(iconColor));
			}

			return currView;
		}
   }

   public interface IconPickerListener {
	    void iconPicked(Bundle bundle);
   }
}
