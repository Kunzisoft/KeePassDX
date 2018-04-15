/*
 * Copyright 2017 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.fileselect;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.util.Log;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.stylish.Stylish;
import com.nononsenseapps.filepicker.FilePickerActivity;

/**
 * FilePickerActivity class with a style compatibility
 */
public class FilePickerStylishActivity extends FilePickerActivity {

    private @StyleRes
    int themeId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        this.themeId = FilePickerStylish.getThemeId(this);
        setTheme(themeId);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(FilePickerStylish.getThemeId(this) != this.themeId) {
            Log.d(this.getClass().getName(), "Theme change detected, restarting activity");
            this.recreate();
        }
    }

    /**
     * Derived from the Stylish class, get the specific FilePickerStyle theme
     */
    public static class FilePickerStylish extends Stylish {
        public static @StyleRes int getThemeId(Context context) {
            if (themeString.equals(context.getString(R.string.list_style_name_night)))
                return R.style.KeepassDXStyle_FilePickerStyle_Night;
            else if (themeString.equals(context.getString(R.string.list_style_name_dark)))
                return R.style.KeepassDXStyle_FilePickerStyle_Dark;
            else if (themeString.equals(context.getString(R.string.list_style_name_blue)))
                return R.style.KeepassDXStyle_FilePickerStyle_Blue;
            else if (themeString.equals(context.getString(R.string.list_style_name_purple)))
                return R.style.KeepassDXStyle_FilePickerStyle_Purple;

            return R.style.KeepassDXStyle_FilePickerStyle_Light;
        }
    }
}
