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
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.app.App;
import com.kunzisoft.keepass.database.Database;
import com.kunzisoft.keepass.database.PwGroup;
import com.kunzisoft.keepass.database.PwIcon;

import static com.kunzisoft.keepass.dialogs.GroupEditDialogFragment.EditGroupDialogAction.CREATION;
import static com.kunzisoft.keepass.dialogs.GroupEditDialogFragment.EditGroupDialogAction.UPDATE;
import static com.kunzisoft.keepass.dialogs.GroupEditDialogFragment.EditGroupDialogAction.getActionFromOrdinal;

public class GroupEditDialogFragment extends DialogFragment
        implements IconPickerDialogFragment.IconPickerListener {

    public static final String TAG_CREATE_GROUP = "TAG_CREATE_GROUP";

	public static final String KEY_NAME = "KEY_NAME";
	public static final String KEY_ICON = "KEY_ICON";
	public static final String KEY_ACTION_ID = "KEY_ACTION_ID";

	private Database database;

	private EditGroupListener editGroupListener;

    private EditGroupDialogAction editGroupDialogAction;
    private String nameGroup;
    private PwIcon iconGroup;

    private ImageView iconButton;
    private int iconColor;

    public enum EditGroupDialogAction {
        CREATION, UPDATE, NONE;

        public static EditGroupDialogAction getActionFromOrdinal(int ordinal) {
            return EditGroupDialogAction.values()[ordinal];
        }
    }

    public static GroupEditDialogFragment build() {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_ACTION_ID, CREATION.ordinal());
        GroupEditDialogFragment fragment = new GroupEditDialogFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public static GroupEditDialogFragment build(PwGroup group) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_NAME, group.getName());
        bundle.putParcelable(KEY_ICON, group.getIcon());
        bundle.putInt(KEY_ACTION_ID, UPDATE.ordinal());
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
        assert getActivity() != null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View root = inflater.inflate(R.layout.group_edit, null);
        TextView nameField = root.findViewById(R.id.group_name);
        iconButton = root.findViewById(R.id.icon_button);

        // Retrieve the textColor to tint the icon
        int[] attrs = {android.R.attr.textColorPrimary};
        TypedArray ta = getActivity().getTheme().obtainStyledAttributes(attrs);
        iconColor = ta.getColor(0, Color.WHITE);

        // Init elements
        database = App.getDB();
        editGroupDialogAction = EditGroupDialogAction.NONE;
        nameGroup = "";
        iconGroup = database.getPwDatabase().getIconFactory().getFolderIcon();

        if (savedInstanceState != null
                && savedInstanceState.containsKey(KEY_ACTION_ID)
                && savedInstanceState.containsKey(KEY_NAME)
                && savedInstanceState.containsKey(KEY_ICON)) {
            editGroupDialogAction = getActionFromOrdinal(savedInstanceState.getInt(KEY_ACTION_ID));
            nameGroup = savedInstanceState.getString(KEY_NAME);
            iconGroup = savedInstanceState.getParcelable(KEY_ICON);

        } else {

            if (getArguments() != null
                    && getArguments().containsKey(KEY_ACTION_ID))
                editGroupDialogAction = EditGroupDialogAction.getActionFromOrdinal(getArguments().getInt(KEY_ACTION_ID));

            if (getArguments() != null
                    && getArguments().containsKey(KEY_NAME)
                    && getArguments().containsKey(KEY_ICON)) {
                nameGroup = getArguments().getString(KEY_NAME);
                iconGroup = getArguments().getParcelable(KEY_ICON);
            }
        }

        // populate the name
        nameField.setText(nameGroup);
        // populate the icon
        assignIconView();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(root)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    String name = nameField.getText().toString();
                    if ( name.length() > 0 ) {
                        editGroupListener.approveEditGroup(
                                editGroupDialogAction,
                                name,
                                iconGroup);

                        GroupEditDialogFragment.this.getDialog().cancel();
                    }
                    else {
                        Toast.makeText(getContext(), R.string.error_no_name, Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> {
                    String name = nameField.getText().toString();
                    editGroupListener.cancelEditGroup(
                            editGroupDialogAction,
                            name,
                            iconGroup);

                    GroupEditDialogFragment.this.getDialog().cancel();
                });

        iconButton.setOnClickListener(v -> {
            IconPickerDialogFragment iconPickerDialogFragment = new IconPickerDialogFragment();
            if (getFragmentManager() != null)
                iconPickerDialogFragment.show(getFragmentManager(), "IconPickerDialogFragment");
        });

        return builder.create();
    }

    private void assignIconView() {
        database.getDrawFactory()
                .assignDatabaseIconTo(
                        getContext(),
                        iconButton,
                        iconGroup,
                        iconColor);
    }

    @Override
    public void iconPicked(Bundle bundle) {
        iconGroup = bundle.getParcelable(IconPickerDialogFragment.KEY_ICON_STANDARD);
        assignIconView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_ACTION_ID, editGroupDialogAction.ordinal());
        outState.putString(KEY_NAME, nameGroup);
        outState.putParcelable(KEY_ICON, iconGroup);
        super.onSaveInstanceState(outState);
    }

    public interface EditGroupListener {
        void approveEditGroup(EditGroupDialogAction action, String name, PwIcon selectedIcon);
        void cancelEditGroup(EditGroupDialogAction action, String name, PwIcon selectedIcon);
    }
}
