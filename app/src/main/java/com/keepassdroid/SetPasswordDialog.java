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
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.kunzisoft.keepass.R;
import com.keepassdroid.app.App;
import com.keepassdroid.database.edit.FileOnFinish;
import com.keepassdroid.database.edit.OnFinish;
import com.keepassdroid.database.edit.SetPassword;
import com.keepassdroid.utils.EmptyUtils;
import com.keepassdroid.utils.UriUtil;

public class SetPasswordDialog extends DialogFragment {

    private final static String FINISH_TAG = "FINISH_TAG";

	private byte[] masterKey;
	private Uri mKeyfile;
	private FileOnFinish mFinish;
	private View rootView;
	
	public byte[] getKey() {
		return masterKey;
	}
	
	public Uri keyfile() {
		return mKeyfile;
	}

    public static SetPasswordDialog newInstance(FileOnFinish finish) {
        SetPasswordDialog setPasswordDialog = new SetPasswordDialog();

        Bundle args = new Bundle();
        args.putSerializable(FINISH_TAG, finish);
        setPasswordDialog.setArguments(args);

        return setPasswordDialog;
    }

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        if(getArguments() != null && getArguments().containsKey(FINISH_TAG)) {
            mFinish = (FileOnFinish) getArguments().getSerializable(FINISH_TAG);
        }

        rootView = inflater.inflate(R.layout.set_password, null);
        builder.setView(rootView)
                // Add action buttons
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        TextView passView = (TextView) rootView.findViewById(R.id.pass_password);
                        String pass = passView.getText().toString();
                        TextView passConfView = (TextView) rootView.findViewById(R.id.pass_conf_password);
                        String confpass = passConfView.getText().toString();

                        // Verify that passwords match
                        if ( ! pass.equals(confpass) ) {
                            // Passwords do not match
                            Toast.makeText(getContext(), R.string.error_pass_match, Toast.LENGTH_LONG).show();
                            return;
                        }

                        TextView keyfileView = (TextView) rootView.findViewById(R.id.pass_keyfile);
                        Uri keyfile = UriUtil.parseDefaultFile(keyfileView.getText().toString());
                        mKeyfile = keyfile;

                        // Verify that a password or keyfile is set
                        if ( pass.length() == 0 && EmptyUtils.isNullOrEmpty(keyfile)) {
                            Toast.makeText(getContext(), R.string.error_nopass, Toast.LENGTH_LONG).show();
                            return;

                        }

                        SetPassword sp = new SetPassword(getContext(), App.getDB(), pass, keyfile, new AfterSave(mFinish, new Handler()));
                        final ProgressTask pt = new ProgressTask(getContext(), sp, R.string.saving_database);
                        boolean valid = sp.validatePassword(getContext(), new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                pt.run();
                            }
                        });

                        if (valid) {
                            pt.run();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        SetPasswordDialog.this.getDialog().cancel();
                        if ( mFinish != null ) {
                            mFinish.run();
                        }
                    }
                });
        return builder.create();
	}

	private class AfterSave extends OnFinish {
		private FileOnFinish mFinish;
		
		public AfterSave(FileOnFinish finish, Handler handler) {
			super(finish, handler);
			mFinish = finish;
		}

		@Override
		public void run() {
			if ( mSuccess ) {
				if ( mFinish != null ) {
					mFinish.setFilename(mKeyfile);
				}
				dismiss();
			} else {
				displayMessage(getContext());
			}
			super.run();
		}
	}
}
