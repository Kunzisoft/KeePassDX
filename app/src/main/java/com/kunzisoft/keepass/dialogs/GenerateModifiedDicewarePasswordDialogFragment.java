/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft,
 * Pacharapol Withayasakpunt
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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.settings.PreferencesUtil;
import com.kunzisoft.keepass.utils.Util;
import com.patarapolw.diceware_utils.DicewarePassword;
import com.patarapolw.diceware_utils.Policy;

public class GenerateModifiedDicewarePasswordDialogFragment extends DialogFragment {
    public static final String KEY_PASSWORD_ID = "KEY_PASSWORD_ID";

	private GeneratePasswordListener mListener;
    private EditText lengthMinView;
	private EditText lengthMaxView;
	private EditText numberOfKeywordsView;
	private EditText punctuationCountMinView;
	private EditText digitCountMinView;

	private EditText passwordView;
	private EditText mnemonicView;

	private DicewarePassword dicewarePassword;
	private Policy policy;

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
        View root = inflater.inflate(R.layout.generate_modified_diceware_password, null);

        dicewarePassword = new DicewarePassword(getContext());
        policy = new Policy(getContext());

        passwordView = root.findViewById(R.id.password);
        Util.applyFontVisibilityTo(getContext(), passwordView);

        mnemonicView = root.findViewById(R.id.mnemonic);
        Util.applyFontVisibilityTo(getContext(), mnemonicView);

        lengthMinView = root.findViewById(R.id.length_min);
        lengthMaxView = root.findViewById(R.id.length_max);
        numberOfKeywordsView = root.findViewById(R.id.number_of_keywords);
        punctuationCountMinView = root.findViewById(R.id.punctuation_count_min);
        digitCountMinView = root.findViewById(R.id.number_count_min);

        if(PreferencesUtil.getDefaultPasswordGenerator(getContext()) == 2){
            numberOfKeywordsView.setText("3");
        }

//        assignDefaultCharacters();

        Button genPassButton = root.findViewById(R.id.generate_password_button);
        genPassButton.setOnClickListener(v -> fillPassword());

        builder.setView(root)
                .setPositiveButton(R.string.accept, (dialog, id) -> {
                    String mnemonic = "";

                    mnemonic += mnemonicView.getText().toString();

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
	
	private void fillPassword() {
		policy.setLength_min(Integer.parseInt(lengthMinView.getText().toString()));
		policy.setLength_max(Integer.parseInt(lengthMaxView.getText().toString()));
		policy.setPunctuation_count(Integer.parseInt(punctuationCountMinView.getText().toString()));
		policy.setDigit_count(Integer.parseInt(digitCountMinView.getText().toString()));

		dicewarePassword.setPolicy(policy);
        dicewarePassword.generateModifiedDicewarePassword(Integer.parseInt(numberOfKeywordsView.getText().toString()));

		passwordView.setText(dicewarePassword.getPassword());
		mnemonicView.setText(TextUtils.join(" ", dicewarePassword.getKeywordList()));
	}

    public interface GeneratePasswordListener {
        void acceptPassword(Bundle bundle);
	    void cancelPassword(Bundle bundle);
    }
}
