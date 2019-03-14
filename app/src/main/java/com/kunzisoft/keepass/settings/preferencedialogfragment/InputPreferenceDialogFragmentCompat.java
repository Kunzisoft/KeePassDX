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
package com.kunzisoft.keepass.settings.preferencedialogfragment;

import android.support.annotation.StringRes;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.view.View;
import android.widget.TextView;

import com.kunzisoft.keepass.R;

public abstract class InputPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat {

    private TextView textExplanationView;

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        textExplanationView = view.findViewById(R.id.explanation_text);
    }

    public String getExplanationText() {
        if (textExplanationView != null)
            return textExplanationView.getText().toString();
        else
            return "";
    }

    public void setExplanationText(String explanationText) {
        if (textExplanationView != null)
            if (explanationText != null && !explanationText.isEmpty()) {
                textExplanationView.setText(explanationText);
                textExplanationView.setVisibility(View.VISIBLE);
            } else {
                textExplanationView.setText(explanationText);
                textExplanationView.setVisibility(View.VISIBLE);
            }
    }

    public void setExplanationText(@StringRes int explanationTextId) {
        setExplanationText(getString(explanationTextId));
    }
}
