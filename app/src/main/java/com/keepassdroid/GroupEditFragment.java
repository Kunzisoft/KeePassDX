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
package com.keepassdroid;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.keepass.R;
import com.keepassdroid.icons.Icons;

public class GroupEditFragment extends DialogFragment
        implements IconPickerFragment.IconPickerListener {

	public static final String KEY_NAME = "name";
	public static final String KEY_ICON_ID = "icon_id";

	private CreateGroupListener createGroupListener;

	private int mSelectedIconID;
    private View root;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            createGroupListener = (CreateGroupListener) context;
            createGroupListener = (CreateGroupListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement " + GroupEditFragment.class.getName());
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        root = inflater.inflate(R.layout.group_edit, null);
        builder.setView(root)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        TextView nameField = (TextView) root.findViewById(R.id.group_name);
                        String name = nameField.getText().toString();

                        if ( name.length() > 0 ) {
                            Bundle bundle = new Bundle();
                            bundle.putString(KEY_NAME, name);
                            bundle.putInt(KEY_ICON_ID, mSelectedIconID);
                            createGroupListener.approveCreateGroup(bundle);

                            GroupEditFragment.this.getDialog().cancel();
                        }
                        else {
                            Toast.makeText(getContext(), R.string.error_no_name, Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Bundle bundle = new Bundle();
                        createGroupListener.cancelCreateGroup(bundle);

                        GroupEditFragment.this.getDialog().cancel();
                    }
                });

        final ImageButton iconButton = (ImageButton) root.findViewById(R.id.icon_button);
        iconButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                IconPickerFragment iconPickerFragment = new IconPickerFragment();
                iconPickerFragment.show(getFragmentManager(), "IconPickerFragment");
            }
        });

        return builder.create();
    }

    @Override
    public void iconPicked(Bundle bundle) {
        mSelectedIconID = bundle.getInt(IconPickerFragment.KEY_ICON_ID);
        ImageButton currIconButton = (ImageButton) root.findViewById(R.id.icon_button);
        currIconButton.setImageResource(Icons.iconToResId(mSelectedIconID));
    }

    public interface CreateGroupListener {
        void approveCreateGroup(Bundle bundle);
        void cancelCreateGroup(Bundle bundle);
    }
}
