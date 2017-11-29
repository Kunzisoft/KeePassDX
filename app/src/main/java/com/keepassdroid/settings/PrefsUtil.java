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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.kunzisoft.keepass.R;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PrefsUtil {

	public static float getListTextSize(Context ctx) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		return Float.parseFloat(prefs.getString(ctx.getString(R.string.list_size_key), ctx.getString(R.string.list_size_default)));
	}

    public static int getDefaultPasswordLength(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getInt(ctx.getString(R.string.password_length_key),
                        Integer.parseInt(ctx.getString(R.string.default_password_length)));
    }

    public static Set<String> getDefaultPasswordCharacters(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getStringSet(ctx.getString(R.string.list_password_generator_options_key),
                new HashSet<>(Arrays.asList(
                        ctx.getResources()
                                .getStringArray(R.array.list_password_generator_options_default_values))));
    }
}
