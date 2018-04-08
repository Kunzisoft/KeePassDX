/*
 * Copyright 2018 Brian Pellin, Jeremy Jamet / Kunzisoft, Justin Gross.
 *     
 * This file is part of KeePass Libre.
 *
 *  KeePass Libre is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass Libre is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass Libre.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.dialogs;

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

import com.kunzisoft.keepass.database.PwNode;
import com.kunzisoft.keepass.icons.Icons;
import tech.jgross.keepass.R;

public class GroupEditDialogFragment extends DialogFragment
        implements IconPickerDialogFragment.IconPickerListener {

    public static final String TAG_CREATE_GROUP = "TAG_CREATE_GROUP";

	public static final String KEY_NAME = "name";
	public static final String KEY_ICON_ID = "icon_id";

	private EditGroupListener editGroupListener;

	private TextView nameField;
    private ImageButton iconButton;
	private int mSelectedIconID;
    private View root;

    public static GroupEditDialogFragment build(PwNode group) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_NAME, group.getDisplayTitle());
        // TODO Change
        bundle.putInt(KEY_ICON_ID, group.getIcon().hashCode());
        GroupEditDialogFragment fragment = new GroupEditDialogFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            editGroupListener = (EditGroupListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement " + GroupEditDialogFragment.class.getName());
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();
        root = inflater.inflate(R.layout.group_edit, null);
        nameField = (TextView) root.findViewById(R.id.group_name);
        iconButton = (ImageButton) root.findViewById(R.id.icon_button);

        if (getArguments() != null
                && getArguments().containsKey(KEY_NAME)
                && getArguments().containsKey(KEY_ICON_ID)) {
            nameField.setText(getArguments().getString(KEY_NAME));
            populateIcon(getArguments().getInt(KEY_ICON_ID));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(root)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String name = nameField.getText().toString();
                        if ( name.length() > 0 ) {
                            Bundle bundle = new Bundle();
                            bundle.putString(KEY_NAME, name);
                            bundle.putInt(KEY_ICON_ID, mSelectedIconID);
                            editGroupListener.approveEditGroup(bundle);

                            GroupEditDialogFragment.this.getDialog().cancel();
                        }
                        else {
                            Toast.makeText(getContext(), R.string.error_no_name, Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Bundle bundle = new Bundle();
                        editGroupListener.cancelEditGroup(bundle);

                        GroupEditDialogFragment.this.getDialog().cancel();
                    }
                });

        final ImageButton iconButton = (ImageButton) root.findViewById(R.id.icon_button);
        iconButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                IconPickerDialogFragment iconPickerDialogFragment = new IconPickerDialogFragment();
                iconPickerDialogFragment.show(getFragmentManager(), "IconPickerDialogFragment");
            }
        });

        return builder.create();
    }

    private void populateIcon(int iconId) {
        iconButton.setImageResource(Icons.iconToResId(iconId));
    }

    @Override
    public void iconPicked(Bundle bundle) {
        mSelectedIconID = bundle.getInt(IconPickerDialogFragment.KEY_ICON_ID);
        populateIcon(mSelectedIconID);
    }

    public interface EditGroupListener {
        void approveEditGroup(Bundle bundle);
        void cancelEditGroup(Bundle bundle);
    }
}
