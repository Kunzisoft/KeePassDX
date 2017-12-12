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
package com.keepassdroid.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;

import com.kunzisoft.keepass.R;

public class RoundsPreference extends DialogPreference {

    private long mRounds;
    private String explanations;

    public RoundsPreference(Context context) {
        this(context, null);
    }
    public RoundsPreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.dialogPreferenceStyle);
    }
    public RoundsPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, defStyleAttr);
    }
    public RoundsPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.RoundsDialog,
                0, 0);
        try {
            explanations = a.getString(R.styleable.RoundsDialog_description);
        } finally {
            a.recycle();
        }
    }

    @Override
    public int getDialogLayoutResource() {
        return R.layout.pref_dialog_rounds;
    }

    public String getExplanations() {
        return explanations;
    }

    public void setExplanations(String explanations) {
        this.explanations = explanations;
    }

    public long getRounds() {
        return mRounds;
    }

    public void setRounds(long rounds) {
        this.mRounds = rounds;
        // Save to Shared Preferences
        persistLong(rounds);
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
        long rounds;
        if (!restorePersistedValue) {
            rounds = 100000;
            if (defaultValue instanceof String) {
                rounds = Long.parseLong((String) defaultValue);
            }
            if (defaultValue instanceof Integer) {
                rounds = (Integer) defaultValue;
            }
            try {
                rounds = (long) defaultValue;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            rounds = getPersistedLong(mRounds);
        }

        setRounds(rounds);
    }

}
