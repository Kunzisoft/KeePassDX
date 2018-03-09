/*
 * Copyright 2017 Jeremy Jamet / Kunzisoft.
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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.keepassdroid.fileselect.FilePickerStylishActivity;
import com.keepassdroid.utils.UriUtil;
import com.kunzisoft.keepass.R;
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
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View rootView = inflater.inflate(R.layout.file_creation, null);
        builder.setView(rootView)
                .setTitle(R.string.create_keepass_file)
                // Add action buttons
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {}
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {}
                });

        // Folder selection
        View browseView = rootView.findViewById(R.id.browse_button);
        folderPathView = (EditText) rootView.findViewById(R.id.folder_path);
        fileNameView = (EditText) rootView.findViewById(R.id.filename);
        String defaultPath = Environment.getExternalStorageDirectory().getPath()
                + getString(R.string.database_file_path_default);
        folderPathView.setText(defaultPath);
        browseView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getContext(), FilePickerStylishActivity.class);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
                i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR);
                i.putExtra(FilePickerActivity.EXTRA_START_PATH,
                        Environment.getExternalStorageDirectory().getPath());
                startActivityForResult(i, FILE_CODE);
            }
        });

        // Init path
        uriPath = null;

        // Extension
        extension = getString(R.string.database_file_extension_default);
        Spinner spinner = (Spinner) rootView.findViewById(R.id.file_types);
        spinner.setOnItemSelectedListener(this);

        // Spinner Drop down elements
        String[] fileTypes = getResources().getStringArray(R.array.file_types);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, fileTypes);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                Button positiveButton = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        if(mListener.onDefinePathDialogPositiveClick(buildPath()))
                            CreateFileDialogFragment.this.dismiss();
                    }
                });
                Button negativeButton = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE);
                negativeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        if(mListener.onDefinePathDialogNegativeClick(buildPath()))
                            CreateFileDialogFragment.this.dismiss();
                    }
                });
            }
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
