/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
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
package com.keepassdroid.fileselect;

import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import tech.jgross.keepass.R;

import java.text.DateFormat;

public class FileInformationDialogFragment extends DialogFragment {

    private static final String FILE_SELECT_BEEN_ARG = "FILE_SELECT_BEEN_ARG";

    public static FileInformationDialogFragment newInstance(FileSelectBean fileSelectBean) {
        FileInformationDialogFragment fileInformationDialogFragment =
                new FileInformationDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(FILE_SELECT_BEEN_ARG, fileSelectBean);
        fileInformationDialogFragment.setArguments(args);
        return fileInformationDialogFragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View root = inflater.inflate(R.layout.file_selection_information, null);

        if (getArguments() != null && getArguments().containsKey(FILE_SELECT_BEEN_ARG)) {
            FileSelectBean fileSelectBean = (FileSelectBean) getArguments().getSerializable(FILE_SELECT_BEEN_ARG);
            TextView fileWarningView = (TextView) root.findViewById(R.id.file_warning);
            if(fileSelectBean != null) {
                TextView fileNameView = (TextView) root.findViewById(R.id.file_filename);
                TextView filePathView = (TextView) root.findViewById(R.id.file_path);
                TextView fileSizeView = (TextView) root.findViewById(R.id.file_size);
                TextView fileModificationView = (TextView) root.findViewById(R.id.file_modification);
                fileWarningView.setVisibility(View.GONE);
                fileNameView.setText(fileSelectBean.getFileName());
                filePathView.setText(Uri.decode(fileSelectBean.getFileUri().toString()));
                fileSizeView.setText(String.valueOf(fileSelectBean.getSize()));
                fileModificationView.setText(DateFormat.getDateTimeInstance()
                        .format(fileSelectBean.getLastModification()));
                if(fileSelectBean.notFound())
                    showFileNotFound(fileWarningView);
            } else
                showFileNotFound(fileWarningView);
        }

        builder.setView(root);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

            }
        });
        return builder.create();
    }

    private void showFileNotFound(TextView fileWarningView) {
        fileWarningView.setVisibility(View.VISIBLE);
        fileWarningView.setText(R.string.file_not_found);
    }
}
