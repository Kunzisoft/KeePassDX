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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;

import com.android.keepass.R;
import com.keepassdroid.password.PasswordGenerator;

public class GeneratePasswordFragment extends DialogFragment {

    public static final String KEY_PASSWORD_ID = "KEY_PASSWORD_ID";

	private GeneratePasswordListener mListener;
	private View root;
	private EditText lengthTextView;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (GeneratePasswordListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement " + GeneratePasswordListener.class.getName());
        }
    }

	@NonNull
    @Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater();
        root = inflater.inflate(R.layout.generate_password, null);

        lengthTextView = (EditText) root.findViewById(R.id.length);

        SeekBar seekBar = (SeekBar) root.findViewById(R.id.seekbar_length);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lengthTextView.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        Button genPassButton = (Button) root.findViewById(R.id.generate_password_button);
        genPassButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                fillPassword();
            }
        });

        builder.setView(root)
                .setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        EditText password = (EditText) root.findViewById(R.id.password);
                        Bundle bundle = new Bundle();
                        bundle.putString(KEY_PASSWORD_ID, password.getText().toString());
                        mListener.acceptPassword(bundle);

                        dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Bundle bundle = new Bundle();
                        mListener.cancelPassword(bundle);

                        dismiss();
                    }
                });

        // Pre-populate a password to possibly save the user a few clicks
        fillPassword();

		return builder.create();
	}
	
	private void fillPassword() {
		EditText txtPassword = (EditText) root.findViewById(R.id.password);
		txtPassword.setText(generatePassword());
	}
	
    public String generatePassword() {
    	String password = "";
    	
    	try {
    		int length = Integer.valueOf(((EditText) root.findViewById(R.id.length)).getText().toString());
    		
    		((CheckBox) root.findViewById(R.id.cb_uppercase)).isChecked();
        	
        	PasswordGenerator generator = new PasswordGenerator(getActivity());
       	
	    	password = generator.generatePassword(length,
	    			((CheckBox) root.findViewById(R.id.cb_uppercase)).isChecked(),
	    			((CheckBox) root.findViewById(R.id.cb_lowercase)).isChecked(),
	    			((CheckBox) root.findViewById(R.id.cb_digits)).isChecked(),
	    			((CheckBox) root.findViewById(R.id.cb_minus)).isChecked(),
	    			((CheckBox) root.findViewById(R.id.cb_underline)).isChecked(),
	    			((CheckBox) root.findViewById(R.id.cb_space)).isChecked(),
	    			((CheckBox) root.findViewById(R.id.cb_specials)).isChecked(),
	    			((CheckBox) root.findViewById(R.id.cb_brackets)).isChecked());
    	} catch (NumberFormatException e) {
    		Toast.makeText(getContext(), R.string.error_wrong_length, Toast.LENGTH_LONG).show();
		} catch (IllegalArgumentException e) {
			Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
		}
    	
    	return password;
    }

    public interface GeneratePasswordListener {
        void acceptPassword(Bundle bundle);
	    void cancelPassword(Bundle bundle);
    }
}
