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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
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
import com.patarapolw.password_generator.PasswordGenerator;
import com.patarapolw.randomsentence.SentenceMaker;
import com.patarapolw.wordify.MajorSystemPeg;
import com.patarapolw.wordify.Wordify;

public class GeneratePinDialogFragment extends DialogFragment {

    public static final String KEY_PASSWORD_ID = "KEY_PASSWORD_ID";
    public static final String KEY_MNEMONIC_ID = "KEY_MNEMONIC_ID";

    private GeneratePinListener mListener;
    private EditText lengthView;

    private EditText passwordView;
    private EditText mnemonicView;
    private EditText sentenceView;

    private PasswordGenerator passwordGenerator = new PasswordGenerator();
    private MajorSystemPeg majorSystemPeg;
    private SentenceMaker sentenceMaker = null;
    private String[] keywords = new String[]{""};

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

        majorSystemPeg = new MajorSystemPeg(getContext());

        passwordView = root.findViewById(R.id.password);
        Util.applyFontVisibilityTo(getContext(), passwordView);

        mnemonicView = root.findViewById(R.id.mnemonic);
        Util.applyFontVisibilityTo(getContext(), mnemonicView);

        sentenceView = root.findViewById(R.id.generated_sentence);
        Util.applyFontVisibilityTo(getContext(), sentenceView);

        lengthView = root.findViewById(R.id.length);

        if(PreferencesUtil.isGenerateSentence(getContext())) {
            SentenceMakerLoader loader = new SentenceMakerLoader();
            loader.execute(getContext());
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
        String pin = passwordGenerator.generatePin(Integer.parseInt(lengthView.getText().toString()));
        keywords = majorSystemPeg.toWords(pin);

        passwordView.setText(pin);
        mnemonicView.setText(TextUtils.join(" ", keywords));
        if(sentenceMaker != null) {
            sentenceView.setText(sentenceMaker.makeSentence(keywords));
        }
    }

    public interface GeneratePinListener {
        void acceptPassword(Bundle bundle);
        void cancelPassword(Bundle bundle);
    }

    @SuppressLint("StaticFieldLeak")
    private class SentenceMakerLoader extends AsyncTask<Context, Void, SentenceMaker> {
        protected SentenceMaker doInBackground(Context... contexts){

            return new SentenceMaker(contexts[0]);
        }

        protected void onPostExecute(SentenceMaker maker){
            sentenceMaker = maker;
            sentenceView.setText(maker.makeSentence(keywords));
        }
    }
}
