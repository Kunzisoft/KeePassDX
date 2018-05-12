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
package com.kunzisoft.keepass.settings.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.crypto.keyDerivation.KdfEngine;

public class InputNumberPreference extends InputTextExplanationPreference {

    private long mNumber;

    public InputNumberPreference(Context context) {
        this(context, null);
    }

    public InputNumberPreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.dialogPreferenceStyle);
    }

    public InputNumberPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, defStyleAttr);
    }

    public InputNumberPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public int getDialogLayoutResource() {
        return R.layout.pref_dialog_numbers;
    }

    public long getNumber() {
        return mNumber;
    }

    public void setNumber(long number) {
        this.mNumber = number;
        // Save to Shared Preferences
        persistLong(number);
    }

    @Override
    public void setSummary(CharSequence summary) {

        if (summary.equals(KdfEngine.UNKNOW_VALUE_STRING)) {
            setEnabled(false);
            super.setSummary("");
        }
        else {
            setEnabled(true);
            super.setSummary(summary);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        // Default value from attribute. Fallback value is set to 0.
        return a.getInt(index, 0);
    }
    @Override
    protected void onSetInitialValue(boolean restorePersistedValue,
                                     Object defaultValue) {
        // Read the value. Use the default value if it is not possible.
        long number;
        if (!restorePersistedValue) {
            number = 100000;
            if (defaultValue instanceof String) {
                number = Long.parseLong((String) defaultValue);
            }
            if (defaultValue instanceof Integer) {
                number = (Integer) defaultValue;
            }
            try {
                number = (long) defaultValue;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            number = getPersistedLong(mNumber);
        }

        setNumber(number);
    }

}
