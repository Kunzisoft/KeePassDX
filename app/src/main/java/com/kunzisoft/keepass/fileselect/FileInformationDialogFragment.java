/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.fileselect;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.kunzisoft.keepass.R;

import java.text.DateFormat;

public class FileInformationDialogFragment extends DialogFragment {

    private static final String FILE_SELECT_BEEN_ARG = "FILE_SELECT_BEEN_ARG";

    private View fileSizeContainerView;
    private View fileModificationContainerView;

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
        TextView fileNameView = root.findViewById(R.id.file_filename);
        TextView filePathView = root.findViewById(R.id.file_path);
        fileSizeContainerView = root.findViewById(R.id.file_size_container);
        TextView fileSizeView = root.findViewById(R.id.file_size);
        fileModificationContainerView = root.findViewById(R.id.file_modification_container);
        TextView fileModificationView = root.findViewById(R.id.file_modification);

        if (getArguments() != null && getArguments().containsKey(FILE_SELECT_BEEN_ARG)) {
            FileSelectBean fileSelectBean = (FileSelectBean) getArguments().getSerializable(FILE_SELECT_BEEN_ARG);
            if(fileSelectBean != null) {

                filePathView.setText(Uri.decode(fileSelectBean.getFileUri().toString()));
                fileNameView.setText(fileSelectBean.getFileName());

                if(fileSelectBean.notFound()) {
                    hideFileInfo();
                } else {
                    showFileInfo();
                    fileSizeView.setText(String.valueOf(fileSelectBean.getSize()));
                    fileModificationView.setText(DateFormat.getDateTimeInstance()
                            .format(fileSelectBean.getLastModification()));
                }
            } else
                hideFileInfo();
        }

        builder.setView(root);
        builder.setPositiveButton(android.R.string.ok, (dialog, id) -> {});
        return builder.create();
    }

    private void showFileInfo() {
        fileSizeContainerView.setVisibility(View.VISIBLE);
        fileModificationContainerView.setVisibility(View.VISIBLE);
    }

    private void hideFileInfo() {
        fileSizeContainerView.setVisibility(View.GONE);
        fileModificationContainerView.setVisibility(View.GONE);
    }
}
