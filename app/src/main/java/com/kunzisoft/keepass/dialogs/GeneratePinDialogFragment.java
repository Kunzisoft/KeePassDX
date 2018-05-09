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
import android.os.Handler;
import android.os.Looper;
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
import com.patarapolw.randomsentence.SentenceMaker;

public class GeneratePinDialogFragment extends DialogFragment {

    public static final String KEY_PASSWORD_ID = "KEY_PASSWORD_ID";
    public static final String KEY_MNEMONIC_ID = "KEY_MNEMONIC_ID";

    private GeneratePinListener mListener;
    private EditText lengthView;

    private EditText passwordView;
    private EditText mnemonicView;
    private EditText sentenceView;

    private DicewarePassword dicewarePassword;
    private SentenceMaker sentenceMaker = null;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (GeneratePinListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement " + GeneratePinListener.class.getName());
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View root = inflater.inflate(R.layout.generate_pin, null);

        dicewarePassword = new DicewarePassword(getContext());

        passwordView = root.findViewById(R.id.password);
        Util.applyFontVisibilityTo(getContext(), passwordView);

        mnemonicView = root.findViewById(R.id.mnemonic);
        Util.applyFontVisibilityTo(getContext(), mnemonicView);

        sentenceView = root.findViewById(R.id.generated_sentence);
        Util.applyFontVisibilityTo(getContext(), sentenceView);

        lengthView = root.findViewById(R.id.length);

        if(PreferencesUtil.isGenerateSentence(getContext())) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                sentenceMaker = new SentenceMaker(getContext());
                sentenceView.setText(sentenceMaker.makeSentence(dicewarePassword.getKeywordList()));
            });
        } else {
            sentenceView.setVisibility(View.GONE);
        }

//        assignDefaultCharacters();

        Button genPassButton = root.findViewById(R.id.generate_password_button);
        genPassButton.setOnClickListener(v -> fillPassword());

        builder.setView(root)
                .setPositiveButton(R.string.accept, (dialog, id) -> {
                    String mnemonic = "";

                    mnemonic += mnemonicView.getText().toString();
                    if(sentenceMaker != null) {
                        mnemonic += "\n\n";
                        mnemonic += sentenceView.getText().toString();
                    }

                    Bundle bundle = new Bundle();
                    bundle.putString(KEY_PASSWORD_ID, passwordView.getText().toString());
                    bundle.putString(KEY_MNEMONIC_ID, mnemonic);
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
        dicewarePassword.generatePin(Integer.parseInt(lengthView.getText().toString()));

        passwordView.setText(dicewarePassword.getPin());
        mnemonicView.setText(TextUtils.join(" ", dicewarePassword.getKeywordList()));
        if(sentenceMaker != null) {
            sentenceView.setText(sentenceMaker.makeSentence(dicewarePassword.getKeywordList()));
        }
    }

    public interface GeneratePinListener {
        void acceptPassword(Bundle bundle);
        void cancelPassword(Bundle bundle);
    }
}
