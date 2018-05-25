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
package com.kunzisoft.keepass.stylish;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.v4.app.Fragment;
import android.support.v7.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

public abstract class StylishFragment extends Fragment {

    protected @StyleRes int themeId;
    protected Context contextThemed; // TODO small ref

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context != null) {
            this.themeId = Stylish.getThemeId(context);
        }
        contextThemed = new ContextThemeWrapper(context, themeId);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // To fix status bar color
        if (getActivity() != null
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getActivity().getWindow();

            int[] attrColorPrimaryDark = {android.R.attr.colorPrimaryDark};
            TypedArray taColorPrimaryDark = contextThemed.getTheme().obtainStyledAttributes(attrColorPrimaryDark);
            window.setStatusBarColor(taColorPrimaryDark.getColor(0, Color.BLACK));
            taColorPrimaryDark.recycle();
        }

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    public Context getContextThemed() {
        return contextThemed;
    }
}
