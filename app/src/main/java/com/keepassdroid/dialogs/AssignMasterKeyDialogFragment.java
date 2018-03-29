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
package com.keepassdroid.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.keepassdroid.utils.EmptyUtils;
import com.keepassdroid.utils.UriUtil;
import com.keepassdroid.fileselect.KeyFileHelper;
import com.kunzisoft.keepass.R;

public class AssignMasterKeyDialogFragment extends DialogFragment {

    private String masterPassword;
	private Uri mKeyfile;

	private View rootView;
    private CompoundButton passwordCheckBox;
    private TextView passView;
    private TextView passConfView;
    private CompoundButton keyfileCheckBox;
    private TextView keyfileView;

    private AssignPasswordDialogListener mListener;

    private KeyFileHelper keyFileHelper;

    public interface AssignPasswordDialogListener {
        void onAssignKeyDialogPositiveClick(boolean masterPasswordChecked, String masterPassword,
                                            boolean keyFileChecked, Uri keyFile);
        void onAssignKeyDialogNegativeClick(boolean masterPasswordChecked, String masterPassword,
                                            boolean keyFileChecked, Uri keyFile);
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
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> {
                });

        passwordCheckBox = rootView.findViewById(R.id.password_checkbox);
        passView = rootView.findViewById(R.id.pass_password);
        passView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                passwordCheckBox.setChecked(true);
            }
        });
        passConfView = rootView.findViewById(R.id.pass_conf_password);

        keyfileCheckBox = rootView.findViewById(R.id.keyfile_checkox);
        keyfileView = rootView.findViewById(R.id.pass_keyfile);
        keyfileView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                keyfileCheckBox.setChecked(true);
            }
        });

        keyFileHelper = new KeyFileHelper(this);
        rootView.findViewById(R.id.browse_button)
                .setOnClickListener(view -> keyFileHelper.getOpenFileOnClickViewListener().onClick(view));

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialog1 -> {
            Button positiveButton = ((AlertDialog) dialog1).getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {

                masterPassword = "";
                mKeyfile = null;

                boolean error = verifyPassword() || verifyFile();

                if (!passwordCheckBox.isChecked() && !keyfileCheckBox.isChecked()) {
                    error = true;
                    showNoKeyConfirmationDialog();
                }

                if (!error) {
                    mListener.onAssignKeyDialogPositiveClick(
                            passwordCheckBox.isChecked(), masterPassword,
                            keyfileCheckBox.isChecked(), mKeyfile);
                    dismiss();
                }
            });
            Button negativeButton = ((AlertDialog) dialog1).getButton(DialogInterface.BUTTON_NEGATIVE);
            negativeButton.setOnClickListener(v -> {
                mListener.onAssignKeyDialogNegativeClick(
                        passwordCheckBox.isChecked(), masterPassword,
                        keyfileCheckBox.isChecked(), mKeyfile);
                dismiss();
            });
        });


        return dialog;
	}

	private boolean verifyPassword() {
        boolean error = false;
        if (passwordCheckBox.isChecked()) {
            masterPassword = passView.getText().toString();
            String confpass = passConfView.getText().toString();

            // Verify that passwords match
            if (!masterPassword.equals(confpass)) {
                error = true;
                // Passwords do not match
                Toast.makeText(getContext(), R.string.error_pass_match, Toast.LENGTH_LONG).show();
            }

            if (masterPassword == null || masterPassword.isEmpty()) {
                error = true;
                showEmptyPasswordConfirmationDialog();
            }
        }
        return error;
    }

    private boolean verifyFile() {
        boolean error = false;
        if (keyfileCheckBox.isChecked()) {
            Uri keyfile = UriUtil.parseDefaultFile(keyfileView.getText().toString());
            mKeyfile = keyfile;

            // Verify that a keyfile is set
            if (EmptyUtils.isNullOrEmpty(keyfile)) {
                error = true;
                Toast.makeText(getContext(), R.string.error_nokeyfile, Toast.LENGTH_LONG).show();
            }
        }
        return error;
    }

	private void showEmptyPasswordConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.warning_empty_password)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    if (!verifyFile()) {
                        mListener.onAssignKeyDialogPositiveClick(
                                passwordCheckBox.isChecked(), masterPassword,
                                keyfileCheckBox.isChecked(), mKeyfile);
                        AssignMasterKeyDialogFragment.this.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> {});
        builder.create().show();
    }

    private void showNoKeyConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.warning_no_encryption_key)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    mListener.onAssignKeyDialogPositiveClick(
                            passwordCheckBox.isChecked(), masterPassword,
                            keyfileCheckBox.isChecked(), mKeyfile);
                    AssignMasterKeyDialogFragment.this.dismiss();
                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> {});
        builder.create().show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        keyFileHelper.onActivityResultCallback(requestCode, resultCode, data,
                uri -> {
                    if(uri != null) {
                        Uri pathString = UriUtil.parseDefaultFile(uri.toString());
                        if (pathString != null) {
                            keyfileCheckBox.setChecked(true);
                            keyfileView.setText(pathString.toString());
                        }
                    }
                });
    }
}
