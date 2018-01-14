/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
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
package com.keepassdroid.fileselect;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.kunzisoft.keepass.R;

public class FileInformationDialogFragment extends DialogFragment {

    private static final String FILE_SELECT_BEEN_ARG = "FILE_SELECT_BEEN_ARG";

    public static FileInformationDialogFragment newInstance(FileSelectBeen fileSelectBeen) {
        FileInformationDialogFragment fileInformationDialogFragment =
                new FileInformationDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(FILE_SELECT_BEEN_ARG, fileSelectBeen);
        fileInformationDialogFragment.setArguments(args);
        return fileInformationDialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View root = inflater.inflate(R.layout.file_selection_information, null);

        if (getArguments() != null && getArguments().containsKey(FILE_SELECT_BEEN_ARG)) {
            FileSelectBeen fileSelectBeen = (FileSelectBeen) getArguments().getSerializable(FILE_SELECT_BEEN_ARG);
            if(fileSelectBeen != null) {
                TextView fileNameView = (TextView) root.findViewById(R.id.file_filename);
                TextView filePathView = (TextView) root.findViewById(R.id.file_path);
                TextView fileSizeView = (TextView) root.findViewById(R.id.file_size);
                TextView fileModificationView = (TextView) root.findViewById(R.id.file_modification);
                fileNameView.setText(fileSelectBeen.getFileName());
                filePathView.setText(fileSelectBeen.getFileUri().toString());
                fileSizeView.setText(String.valueOf(fileSelectBeen.getSize()));
                fileModificationView.setText(fileSelectBeen.getLastModification().toString());
            }
        }

        builder.setView(root);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

            }
        });
        return builder.create();
    }
}
