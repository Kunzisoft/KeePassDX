/*
 * Copyright 2017 Jeremy Jamet / Kunzisoft.
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
package com.keepassdroid.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.keepassdroid.fileselect.FilePickerStylishActivity;
import com.keepassdroid.utils.UriUtil;
import tech.jgross.keepass.R;
import com.nononsenseapps.filepicker.FilePickerActivity;
import com.nononsenseapps.filepicker.Utils;

import java.io.File;

public class CreateFileDialogFragment extends DialogFragment implements AdapterView.OnItemSelectedListener{

    private final int FILE_CODE = 3853;

    private EditText folderPathView;
    private EditText fileNameView;
    private DefinePathDialogListener mListener;
    private String extension;

    private Uri uriPath;

    private Button positiveButton;
    private Button negativeButton;

    public interface DefinePathDialogListener {
        boolean onDefinePathDialogPositiveClick(Uri pathFile);
        boolean onDefinePathDialogNegativeClick(Uri pathFile);
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        try {
            mListener = (DefinePathDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement " + DefinePathDialogListener.class.getName());
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        assert getActivity() != null;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View rootView = inflater.inflate(R.layout.file_creation, null);
        builder.setView(rootView)
                .setTitle(R.string.create_keepass_file)
                // Add action buttons
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {})
                .setNegativeButton(R.string.cancel, (dialog, id) -> {});

        // To prevent crash issue #69 https://github.com/Kunzisoft/KeePassDX/issues/69
        ActionMode.Callback actionCopyBarCallback = new ActionMode.Callback() {

            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                if (positiveButton != null && negativeButton != null) {
                    positiveButton.setEnabled(false);
                    negativeButton.setEnabled(false);
                }
                return true;
            }

            public void onDestroyActionMode(ActionMode mode) {
                if (positiveButton != null && negativeButton != null) {
                    positiveButton.setEnabled(true);
                    negativeButton.setEnabled(true);
                }
            }

            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return true;
            }

            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return true;
            }
        };

        // Folder selection
        View browseView = rootView.findViewById(R.id.browse_button);
        folderPathView = rootView.findViewById(R.id.folder_path);
        folderPathView.setCustomSelectionActionModeCallback(actionCopyBarCallback);
        fileNameView = rootView.findViewById(R.id.filename);
        fileNameView.setCustomSelectionActionModeCallback(actionCopyBarCallback);

        String defaultPath = Environment.getExternalStorageDirectory().getPath()
                + getString(R.string.database_file_path_default);
        folderPathView.setText(defaultPath);
        browseView.setOnClickListener(v -> {
            Intent i = new Intent(getContext(), FilePickerStylishActivity.class);
            i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
            i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
            i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR);
            i.putExtra(FilePickerActivity.EXTRA_START_PATH,
                    Environment.getExternalStorageDirectory().getPath());
            startActivityForResult(i, FILE_CODE);
        });

        // Init path
        uriPath = null;

        // Extension
        extension = getString(R.string.database_file_extension_default);
        Spinner spinner = rootView.findViewById(R.id.file_types);
        spinner.setOnItemSelectedListener(this);

        // Spinner Drop down elements
        String[] fileTypes = getResources().getStringArray(R.array.file_types);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, fileTypes);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialog1 -> {
            positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
            positiveButton.setOnClickListener(v -> {
                if(mListener.onDefinePathDialogPositiveClick(buildPath()))
                    dismiss();
            });
            negativeButton.setOnClickListener(v -> {
                if(mListener.onDefinePathDialogNegativeClick(buildPath())) {
                    dismiss();
                }
            });
        });

        return dialog;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CODE && resultCode == Activity.RESULT_OK) {
            uriPath = data.getData();
            if (uriPath != null) {
                File file = Utils.getFileForUri(uriPath);
                folderPathView.setText(file.getPath());
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
        extension = adapterView.getItemAtPosition(position).toString();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        // Do nothing
    }

    private Uri buildPath() {
        Uri path = new Uri.Builder().path(folderPathView.getText().toString())
                .appendPath(fileNameView.getText().toString() + extension)
                .build();
        path = UriUtil.translate(getContext(), path);
        return path;
    }
}
