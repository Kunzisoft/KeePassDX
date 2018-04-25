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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.password.PasswordGenerator;
import com.kunzisoft.keepass.settings.PreferencesUtil;
import com.kunzisoft.keepass.utils.Util;

import java.util.Set;

public class GeneratePasswordDialogFragment extends DialogFragment {

    public static final String KEY_PASSWORD_ID = "KEY_PASSWORD_ID";

	private GeneratePasswordListener mListener;
	private View root;
	private EditText lengthTextView;
	private EditText passwordView;

	private CompoundButton uppercaseBox;
	private CompoundButton lowercaseBox;
	private CompoundButton digitsBox;
	private CompoundButton minusBox;
	private CompoundButton underlineBox;
	private CompoundButton spaceBox;
	private CompoundButton specialsBox;
	private CompoundButton bracketsBox;
	private CompoundButton extendedBox;

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

        passwordView = root.findViewById(R.id.password);
        Util.applyFontVisibilityTo(getContext(), passwordView);

        lengthTextView = root.findViewById(R.id.length);

        uppercaseBox = root.findViewById(R.id.cb_uppercase);
        lowercaseBox = root.findViewById(R.id.cb_lowercase);
        digitsBox = root.findViewById(R.id.cb_digits);
        minusBox = root.findViewById(R.id.cb_minus);
        underlineBox = root.findViewById(R.id.cb_underline);
        spaceBox = root.findViewById(R.id.cb_space);
        specialsBox = root.findViewById(R.id.cb_specials);
        bracketsBox = root.findViewById(R.id.cb_brackets);
        extendedBox = root.findViewById(R.id.cb_extended);

        assignDefaultCharacters();

        SeekBar seekBar = root.findViewById(R.id.seekbar_length);
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
        seekBar.setProgress(PreferencesUtil.getDefaultPasswordLength(getContext()));

        Button genPassButton = root.findViewById(R.id.generate_password_button);
        genPassButton.setOnClickListener(v -> fillPassword());

        builder.setView(root)
                .setPositiveButton(R.string.accept, (dialog, id) -> {
                    Bundle bundle = new Bundle();
                    bundle.putString(KEY_PASSWORD_ID, passwordView.getText().toString());
                    mListener.acceptPassword(bundle);

                    dismiss();
                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> {
                    Bundle bundle = new Bundle();
                    mListener.cancelPassword(bundle);

                    dismiss();
                });

        // Pre-populate a password to possibly save the user a few clicks
        fillPassword();

		return builder.create();
	}

	private void assignDefaultCharacters() {
        uppercaseBox.setChecked(false);
        lowercaseBox.setChecked(false);
        digitsBox.setChecked(false);
        minusBox.setChecked(false);
        underlineBox.setChecked(false);
        spaceBox.setChecked(false);
        specialsBox.setChecked(false);
        bracketsBox.setChecked(false);
        extendedBox.setChecked(false);

        Set<String> defaultPasswordChars =
                PreferencesUtil.getDefaultPasswordCharacters(getContext());
        for(String passwordChar : defaultPasswordChars) {
            if (passwordChar.equals(getString(R.string.value_password_uppercase))) {
                uppercaseBox.setChecked(true);
            }
            else if (passwordChar.equals(getString(R.string.value_password_lowercase))) {
                lowercaseBox.setChecked(true);
            }
            else if (passwordChar.equals(getString(R.string.value_password_digits))) {
                digitsBox.setChecked(true);
            }
            else if (passwordChar.equals(getString(R.string.value_password_minus))) {
                minusBox.setChecked(true);
            }
            else if (passwordChar.equals(getString(R.string.value_password_underline))) {
                underlineBox.setChecked(true);
            }
            else if (passwordChar.equals(getString(R.string.value_password_space))) {
                spaceBox.setChecked(true);
            }
            else if (passwordChar.equals(getString(R.string.value_password_special))) {
                specialsBox.setChecked(true);
            }
            else if (passwordChar.equals(getString(R.string.value_password_brackets))) {
                bracketsBox.setChecked(true);
            }
            else if (passwordChar.equals(getString(R.string.value_password_extended))) {
                extendedBox.setChecked(true);
            }
        }
    }
	
	private void fillPassword() {
		EditText txtPassword = root.findViewById(R.id.password);
		txtPassword.setText(generatePassword());
	}
	
    public String generatePassword() {
    	String password = "";
    	try {
    		int length = Integer.valueOf(((EditText) root.findViewById(R.id.length)).getText().toString());
        	
        	PasswordGenerator generator = new PasswordGenerator(getActivity());
	    	password = generator.generatePassword(length,
                    uppercaseBox.isChecked(),
                    lowercaseBox.isChecked(),
                    digitsBox.isChecked(),
                    minusBox.isChecked(),
                    underlineBox.isChecked(),
                    spaceBox.isChecked(),
                    specialsBox.isChecked(),
                    bracketsBox.isChecked(),
                    extendedBox.isChecked());
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
