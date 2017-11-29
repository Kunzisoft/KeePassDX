/*
 * Copyright 2017 Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.settings;


import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.widget.Toast;

import com.android.keepass.R;

public class LongEditTextPreference extends EditTextPreference {

    public LongEditTextPreference(Context context) {
        super(context);
    }

    public LongEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LongEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        return String.valueOf(getPersistedLong(-1));
    }

    @Override
    protected boolean persistString(String value) {
        try {
            return persistLong(Long.valueOf(value));
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), R.string.error_rounds_not_number, Toast.LENGTH_LONG).show();
        }

        return false;
    }
}