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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.keepassdroid.utils.EmptyUtils;
import com.keepassdroid.utils.UriUtil;
import com.keepassdroid.view.KeyFileHelper;
import com.kunzisoft.keepass.R;

public class AssignPasswordDialog extends DialogFragment {

    private String masterPassword;
	private Uri mKeyfile;

	private View rootView;
    private CompoundButton passwordCheckBox;
    private CompoundButton keyfileCheckBox;

    private AssignPasswordDialogListener mListener;

    private KeyFileHelper keyFileHelper;

    public interface AssignPasswordDialogListener {
        void onAssignKeyDialogPositiveClick(String masterPassword, Uri keyFile);
        void onAssignKeyDialogNegativeClick(String masterPassword, Uri keyFile);
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        try {
            mListener = (AssignPasswordDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement " + AssignPasswordDialogListener.class.getName());
        }
    }

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        rootView = inflater.inflate(R.layout.set_password, null);
        builder.setView(rootView)
                .setTitle(R.string.assign_master_key)
                // Add action buttons
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });

        passwordCheckBox = (CompoundButton) rootView.findViewById(R.id.password_checkbox);
        keyfileCheckBox = (CompoundButton) rootView.findViewById(R.id.keyfile_checkox);

        keyFileHelper = new KeyFileHelper(this);
        rootView.findViewById(R.id.browse_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        keyfileCheckBox.setChecked(true);
                        keyFileHelper.getOpenFileOnClickViewListener().onClick(view);
                    }
                });

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                Button positiveButton = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {

                        masterPassword = "";
                        mKeyfile = null;

                        boolean error = false;

                        // Assign password
                        if (passwordCheckBox.isChecked()) {
                            TextView passView = (TextView) rootView.findViewById(R.id.pass_password);
                            masterPassword = passView.getText().toString();
                            TextView passConfView = (TextView) rootView.findViewById(R.id.pass_conf_password);
                            String confpass = passConfView.getText().toString();

                            // Verify that passwords match
                            if (!masterPassword.equals(confpass)) {
                                // Passwords do not match
                                Toast.makeText(getContext(), R.string.error_pass_match, Toast.LENGTH_LONG).show();
                                error = true;
                            }
                        }

                        // Assign keyfile
                        if (keyfileCheckBox.isChecked()) {
                            TextView keyfileView = (TextView) rootView.findViewById(R.id.pass_keyfile);
                            Uri keyfile = UriUtil.parseDefaultFile(keyfileView.getText().toString());
                            mKeyfile = keyfile;

                            // Verify that a keyfile is set
                            if (EmptyUtils.isNullOrEmpty(keyfile)) {
                                Toast.makeText(getContext(), R.string.error_nokeyfile, Toast.LENGTH_LONG).show();
                                error = true;
                            }
                        }

                        if (!error) {
                            if (!keyfileCheckBox.isChecked() &&
                                    (masterPassword == null || masterPassword.isEmpty())) {
                                showEmptyPasswordConfirmationDialog();
                            } else {
                                mListener.onAssignKeyDialogPositiveClick(masterPassword, mKeyfile);
                                dismiss();
                            }
                        }
                    }
                });
                Button negativeButton = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE);
                negativeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        mListener.onAssignKeyDialogNegativeClick(masterPassword, mKeyfile);
                        dismiss();
                    }
                });
            }
        });


        return dialog;
	}

	private void showEmptyPasswordConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.warning_empty_password)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onAssignKeyDialogPositiveClick(masterPassword, mKeyfile);
                        AssignPasswordDialog.this.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onAssignKeyDialogNegativeClick(masterPassword, mKeyfile);
                    }
                });
        builder.create().show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        keyFileHelper.onActivityResultCallback(requestCode, resultCode, data,
                new KeyFileHelper.KeyFileCallback() {
            @Override
            public void onResultCallback(Uri uri) {
                if(uri != null) {
                    EditText keyFileView = (EditText) rootView.findViewById(R.id.pass_keyfile);
                    Uri pathString = UriUtil.parseDefaultFile(uri.toString());
                    if (pathString != null)
                        keyFileView.setText(pathString.toString());
                }
            }
        });
    }
}
